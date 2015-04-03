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
import org.osgeo.proj4j.CoordinateReferenceSystem;

import android.graphics.Rect;
import android.util.Log;

import com.vividsolutions.jts.geom.Envelope;

import de.rooehler.rastertheque.core.Band;
import de.rooehler.rastertheque.core.DataType;
import de.rooehler.rastertheque.core.Driver;
import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.core.RasterDataset;
import de.rooehler.rastertheque.core.RasterQuery;
import de.rooehler.rastertheque.proj.Proj;
/**
 * A GDALDataset wraps a org.gdal.gdal.dataset
 * and gives access to its properties and metadata
 * 
 * @author Robert Oehler
 *
 */
public class GDALDataset implements RasterDataset{
	
	GDALDriver mDriver;

	Dataset dataset;
	
	Envelope mBounds;
	
	Rect mDimension;

	String mSource;
	
	CoordinateReferenceSystem mCRS;
	
	List<Band> mBands;
	
	/**
	 * default constructor using the path to the raster file,
	 * the dataset opened by the driver and the driver itself
	 * @param pFilePath the path to the file of this dataset
	 * @param dataset the GDAL dataset
	 * @param driver the driver this dataset was opened with
	 */
    public GDALDataset(final String pFilePath, org.gdal.gdal.Dataset dataset, GDALDriver driver) {

        this.mSource = pFilePath;
        this.dataset = dataset;
        this.mDriver = driver;
        
        getBoundingBox();
        
        getMetadata();
        
		((GDALBand)this.getBands().get(0)).applySLDColorMap(mSource);
    }
    /**
     * convenience constructor mainly for tests and raster operations
     * which may need to create a dataset from memory
     * @param dataset the GDAL dataset 
     */
    public GDALDataset(org.gdal.gdal.Dataset dataset){
    	this(null,dataset,null);
    }
	
	/**
	 * Perform a @param RasterQuery against the dataset to receive a raster
	 * @param query the query to perform
	 * @return Raster the raster which resulted from this query against the dataset
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
		final int readWidth = src.width();
		final int readHeight = src.height();
		final int targetWidth = target.width();
		final int targetHeight = target.height();
				
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
	 * in the coordinate system of this crs
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

			mBounds = new Envelope(minx, maxx, miny, maxy);
			
			return mBounds;
			
		}else{
			return mBounds;
		}
	}
	/**
	 * converts the envelope into a envelope in lat/lon coordinates
	 * @param input the src envelope
	 * @param src_Ref the "well-know text" describing the src projection
	 * @return the envelope in geographic (lat-lon) coordinates
	 */
	public static Envelope convertToLatLon(Envelope input, String src_wkt){
		
		SpatialReference old_sr = new SpatialReference(src_wkt);

		SpatialReference new_sr = new SpatialReference();
		new_sr.SetWellKnownGeogCS("WGS84");

		CoordinateTransformation ct =  CoordinateTransformation.CreateCoordinateTransformation(old_sr, new_sr);

		if (ct != null){

			double[] minLatLong = ct.TransformPoint(input.getMinX(), input.getMinY());

			double[] maxLatLong = ct.TransformPoint(input.getMaxX(), input.getMaxY());

			return new Envelope(minLatLong[0], maxLatLong[0], minLatLong[1], maxLatLong[1]);

		}else{
			
			Log.e(GDALDataset.class.getSimpleName(), org.gdal.gdal.gdal.GetLastErrorMsg());	

			return null;
		}
	}
	
	/**
	 * applies a transformation to this dataset according 
	 * to the projection defined in the @param wkt 
	 * @param wkt a projection provided in wkt format see
	 * http://www.geoapi.org/3.0/javadoc/org/opengis/referencing/doc-files/WKT.html
	 * @return dataset in the projection of @param wkt
	 */
	public org.gdal.gdal.Dataset transform(final String wkt){

		SpatialReference dstRef = new SpatialReference(wkt);

		Dataset vrt_ds = gdal.AutoCreateWarpedVRT(dataset,dataset.GetProjection(), dstRef.ExportToWkt());

		return vrt_ds;

	}
	
	/**
	 * Saves the current dataset to disk using the provided fileName
	 * 
	 * The file is written to the directory of the source file of this raster dataset
	 * 
	 * TODO this is experimental and currently not used, test if you want to use it
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
		
		if(this.getBands() != null){
			
			((GDALBand)this.getBands().get(0)).clearColorMap();
		}
		
		if (dataset != null) {
			dataset.delete();
			dataset = null;
		}
		mDimension = null;
		mBounds = null;
		mCRS = null;
		mBands = null;
		
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
	public CoordinateReferenceSystem getCRS() {

		if(mCRS == null){
			
			String proj = dataset.GetProjection();
			if (proj != null && (!proj.equals(""))) {
				try{
					mCRS = Proj.crs(proj);
				}catch(RuntimeException e){
					Log.e(GDALDataset.class.getSimpleName(), "error parsing proj "+proj);
					return null;
				}
			}else{				
				return null;
			}
		}
		
		return mCRS;
		
	}
	/**
	 * converts a CoordinateReferenceSystem to a well-known text formatted String
	 * http://www.geoapi.org/3.0/javadoc/org/opengis/referencing/doc-files/WKT.html
	 * @param crs the crs to convert
	 * @return the crs as wkt
	 */
	public String toWKT(CoordinateReferenceSystem crs) {
		
		if(crs != null){
			
			return Proj.proj2wkt(crs.getParameterString());
		}
			
		return null;	
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
	
		return mSource != null ? mSource.substring(mSource.lastIndexOf("/") + 1) : null;
	}
	/**
	 * returns a description of this dataset
	 */
	@Override
	public String getDescription() {
		
		return dataset != null ? dataset.GetDescription() : null;
	}
	/**
	 * returns the extent of this dataset
	 */
	@Override
	public Rect getDimension() {

		if(mDimension == null && dataset != null){

			mDimension = new Rect(0, 0, dataset.GetRasterXSize(), dataset.getRasterYSize());
		}
		
		return mDimension;
	}
	/**
	 * returns the metadata of this dataset
	 * @return the metadata or null if non is available
	 */
	public Hashtable<?, ?> getMetadata(){
		
		return dataset != null ? dataset.GetMetadata_Dict() : null;
				
	}
}
