package de.rooehler.rastertheque.io.gdal;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Callable;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;

import android.graphics.Rect;
import android.util.Log;

import com.vividsolutions.jts.geom.Envelope;

import de.rooehler.rastertheque.core.Band;
import de.rooehler.rastertheque.core.DataType;
import de.rooehler.rastertheque.core.Driver;
import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.core.RasterDataset;
import de.rooehler.rastertheque.core.RasterQuery;

public class GDALDataset implements RasterDataset{
	
	GDALDriver mDriver;

	Dataset dataset;
	
	Envelope mBounds;
	
	Rect mDimension;

	String mSource;
	
	SpatialReference mCRS;
	
	List<Band> mBands;
	
    public GDALDataset(final String pFilePath, Dataset dataset, GDALDriver driver) {

        this.mSource = pFilePath;
        this.dataset = dataset;
        this.mDriver = driver;
        
        getBoundingBox();
        
        getMetadata();
    }	
	
	/**
	 * Perform a @param RasterQuery against the dataset to receive a raster
	 * @param query the query to perform
	 * @return Raster the raster read
	 */
	@Override
	public Raster read(RasterQuery query) {
		
		Rect src = query.getDimension(); 
		Rect target = query.getDimension();
		
		if(query instanceof GDALRasterQuery){
			if(((GDALRasterQuery)query).getTargetDimension() != null){
				target = ((GDALRasterQuery)query).getTargetDimension();
			}
		}
		final int readWidth = src.right - src.left;
		final int readHeight = src.bottom - src.top;
		final int targetWidth = target.right - target.left;
		final int targetHeight = target.bottom - target.top;
				
		final int bufferSize = targetWidth * targetHeight * query.getDataType().size() * query.getBands().size();
		
		final ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
//		final ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
		buffer.order(ByteOrder.nativeOrder()); 

		if(query.getBands().size() == 1){

			((GDALBand)query.getBands().get(0)).getBand().ReadRaster(
					src.left,src.top, //src pos
					readWidth, readHeight, //src dim
					targetWidth,targetHeight, //dst dim
					DataType.toGDAL(query.getBands().get(0).datatype()), // the type of the pixel values in the array. 
					buffer.array());
		}else{
			int[] readBands = new int[query.getBands().size()];
			for(int i = 0; i < query.getBands().size();i++){
				readBands[i] = ((GDALBand)query.getBands().get(i)).getBand().GetBand();
			}
			dataset.ReadRaster(
					src.left,src.top, //src pos
					readWidth, readHeight, //src dim
					targetWidth,targetHeight, //dst dim
					DataType.toGDAL(query.getBands().get(0).datatype()), // the type of the pixel values in the array. 
					buffer.array(), //buffer to write in
					readBands);
		}
		
		GDALBand.applySLDColorMap(mSource);
		
		return new Raster(query.getBounds() , getCRS(), target, query.getBands(), buffer, getMetadata());

	}


	/**
	 * returns the bands of this dataset
	 */
	@Override
	public List<Band> getBands(){
		
		if(mBands == null){
			
			int nbands = dataset.GetRasterCount();
			
			mBands = new ArrayList<Band>(nbands);
			for (int i = 1; i <= nbands; i++) {
				mBands.add(new GDALBand(dataset.GetRasterBand(i)));
			}
			
		}
		return mBands;

	}

	/**
	 * returns the bounds of this dataset
	 */
	@Override
	public Envelope getBoundingBox(){

		if(mBounds == null){

			int width  = dataset.getRasterXSize();
			int	height = dataset.getRasterYSize();

			double[] gt = dataset.GetGeoTransform();
			double	minx = gt[0];
			double	miny = gt[3] + width*gt[4] + height*gt[5]; // from	http://gdal.org/gdal_datamodel.html
			double	maxx = gt[0] + width*gt[1] + height*gt[2]; // from	http://gdal.org/gdal_datamodel.html
			double	maxy = gt[3];


			SpatialReference old_sr = new SpatialReference(dataset.GetProjectionRef());

			SpatialReference new_sr = new SpatialReference();
			new_sr.SetWellKnownGeogCS("WGS84");

			CoordinateTransformation ct =  CoordinateTransformation.CreateCoordinateTransformation(old_sr, new_sr);

			if (ct != null){

				double[] minLatLong = ct.TransformPoint(minx, miny);

				double[] maxLatLong = ct.TransformPoint(maxx, maxy);

				mBounds = new Envelope(minLatLong[0], maxLatLong[0], minLatLong[1], maxLatLong[1]);

				return mBounds;
			}else{
				

				Log.e(GDALDataset.class.getSimpleName(), org.gdal.gdal.gdal.GetLastErrorMsg());	

				return null;

			}
		}else{
			return mBounds;
		}
	}
	
	/**
	 * applies a projection, defined in the @param wkt to the current dataset
	 * @param wkt a projection provided in wkt format see
	 * http://www.geoapi.org/3.0/javadoc/org/opengis/referencing/doc-files/WKT.html
	 * 
	 */
	public void applyProjection(final String wkt){

		SpatialReference dstRef = new SpatialReference(wkt);

		Dataset vrt_ds = gdal.AutoCreateWarpedVRT(dataset,dataset.GetProjection(), dstRef.ExportToWkt());

		dataset = vrt_ds;
		
		mCRS = null;
		
		getCRS();

	}
	
	/**
	 * Saves the current dataset to disk using the provided fileName
	 * 
	 * As writing large files can take a lot of time, this operation is 
	 * wrapped into a Callable object which will be returned when the operation has finished
	 * 
	 * @param newFileName of the file to save
	 * @return the callable, indicating the file was written
	 */
	public Callable<Dataset> saveCurrentProjectionToFile(final String newFileName){


		Callable<Dataset> c =  new Callable<Dataset>() {
			@Override
			public Dataset call() throws Exception {

				final String newPath = mSource.substring(0,mSource.lastIndexOf("/") + 1) + newFileName;
				Log.d(GDALDataset.class.getSimpleName(), "saving to path "+newPath);

				return dataset.GetDriver().Create(newPath,dataset.getRasterXSize(),dataset.GetRasterYSize(),dataset.getRasterCount());

			}
		};
		return c;
	}
	
    /**
     * closes this dataset and cleans up resources used by it
     */
	@Override
	public void close(){
		
		if (dataset != null) {
			dataset.delete();
			dataset = null;
		}
		mDimension = null;
		mBounds = null;
		mCRS = null;
		mBands = null;
		
		GDALBand.clearColorMap();
	}
	
	/**
	 * returns the path to the source of this dataset
	 */
	@Override
	public String getSource() {

		return mSource;
	}
	/**
	 * returns the CoordinateReferenceSystem of this dataset, if available
	 * null otherwise
	 */
	@Override
	public SpatialReference getCRS() {

		if(mCRS == null){
			
			String proj = dataset.GetProjection();
			if (proj != null) {
				mCRS = new SpatialReference(proj);
//				try{
//					mCRS =  Proj.crs(ref.ExportToProj4());
//				}catch(RuntimeException e){
//					Log.w(GDALDataset.class.getSimpleName(), "Exception getting crs from projection");
//					return null;
//				}
				return mCRS;
			}
			return null;
		}
		
		return mCRS;
		
	}
	/**
	 * converts a CoordinateReferenceSystem to a well-known text formatted String
	 * http://www.geoapi.org/3.0/javadoc/org/opengis/referencing/doc-files/WKT.html
	 * @param crs the crs to convert
	 * @return the crs as wkt
	 */
	public String toWKT(SpatialReference crs) {
		
//		SpatialReference ref = new SpatialReference();
//		ref.ImportFromProj4(Proj.toString(crs));
		if(getCRS() != null){
			
			return getCRS().ExportToWkt();
		}else{
			
			return null;
			
		}
	}
	
	/**
	 * returns the Driver which is used to access this dataset
	 */
	@Override
	public Driver getDriver() {
		
		return mDriver;
	}
	/**
	 * returns the name of this dataset
	 */
	@Override
	public String getName() {
	
		return mSource.substring(mSource.lastIndexOf("/") + 1);
	}
	/**
	 * returns a description of this dataset
	 */
	@Override
	public String getDescription() {
		
		return dataset.GetDescription();
	}
	/**
	 * returns the extent of this dataset
	 */
	@Override
	public Rect getDimension() {

		if(mDimension == null){

			mDimension = new Rect(0, 0, dataset.GetRasterXSize(), dataset.getRasterYSize());
		}
		
		return mDimension;
	}
	/**
	 * returns the metadata of this dataset
	 * @return the metadata or null if non is available
	 */
	public Hashtable<?, ?> getMetadata(){
		
		return dataset.GetMetadata_Dict();
				
	}
}
