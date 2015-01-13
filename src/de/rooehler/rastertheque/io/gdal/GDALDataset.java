package de.rooehler.rastertheque.io.gdal;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import org.gdal.gdal.Dataset;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import org.osgeo.proj4j.CoordinateReferenceSystem;

import android.util.Log;
import de.rooehler.rastertheque.core.Band;
import de.rooehler.rastertheque.core.BoundingBox;
import de.rooehler.rastertheque.core.Coordinate;
import de.rooehler.rastertheque.core.DataType;
import de.rooehler.rastertheque.core.Dimension;
import de.rooehler.rastertheque.core.Driver;
import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.core.RasterDataset;
import de.rooehler.rastertheque.core.RasterQuery;
import de.rooehler.rastertheque.core.Rectangle;
import de.rooehler.rastertheque.processing.Resampler;
import de.rooehler.rastertheque.proj.Proj;

public class GDALDataset extends Resampler implements RasterDataset{
	
	private static final String TAG = GDALDataset.class.getSimpleName();
	
	private GDALDriver mDriver;

	private Dataset dataset;
	
	private static BoundingBox mBB;
	
	private static Dimension mDimension;
	
	private List<Band> mBands;

	private String mSource;
	
	CoordinateReferenceSystem mCRS;
	
    public GDALDataset(final ResampleMethod method,final String pFilePath, Dataset dataset, GDALDriver driver) {
    	super(method);
        this.mSource = pFilePath;
        this.dataset = dataset;
        this.mDriver = driver;
        
        getBoundingBox();
    }

	@Override
	public void close(){
		if (dataset != null) {
			dataset.delete();
			dataset = null;
			mDimension = null;
			mBB = null;
			mCRS = null;
		}
	}	

	
	public String toWKT(CoordinateReferenceSystem crs) {
		SpatialReference ref = new SpatialReference();
		ref.ImportFromProj4(Proj.toString(crs));
		return ref.ExportToWkt();
	}


	@Override
	public List<Band> getBands(){
		int nbands = dataset.GetRasterCount();

		List<Band> bands = new ArrayList<Band>(nbands);
		for (int i = 1; i <= nbands; i++) {
			bands.add(new GDALBand(dataset.GetRasterBand(i)));
		}

		return bands;
	}


	@Override
	public BoundingBox getBoundingBox(){

		if(mBB == null){

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

				mBB = new BoundingBox(minLatLong[0], minLatLong[1], maxLatLong[0], maxLatLong[1]);

				return mBB;
			}else{

				Log.e(TAG, org.gdal.gdal.gdal.GetLastErrorMsg());	

				return null;

			}
		}else{
			return mBB;
		}
	}
	
	public Coordinate getCenterPoint(){

		return getBoundingBox().getCenter();
	}

	@Override
	public String getSource() {

		return mSource;
	}
	@Override
	public CoordinateReferenceSystem getCRS() {

		if(mCRS == null){
			
			String proj = dataset.GetProjection();
			if (proj != null) {
				SpatialReference ref = new SpatialReference(proj);
				try{
					mCRS =  Proj.crs(ref.ExportToProj4());
				}catch(RuntimeException e){
					Log.w(TAG, "Exceptopm getting crs from projection");
					return null;
				}
				return mCRS;
			}
			return null;
		}else{
			return mCRS;
		}
	}
	@Override
	public Driver<GDALDataset> getDriver() {
		
		return mDriver;
	}
	@Override
	public String getName() {
	
		return mSource.substring(mSource.lastIndexOf("/") + 1);
	}
	@Override
	public String getDescription() {
		
		return dataset.GetDescription();
	}
	@Override
	public Dimension getDimension() {

		if(mDimension == null){

			mDimension = new Dimension(dataset.GetRasterXSize(), dataset.getRasterYSize());
		}
		
		return mDimension;
	}
	
	
	@Override
	public Raster read(RasterQuery query) {
		
		Rectangle src = query.getBounds();
		Dimension dstDim = query.getSize();
		
		final int bufferSize = dstDim.getSize() * query.getDataType().size() * query.getBands().size();
		
		final ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
		buffer.order(ByteOrder.nativeOrder()); 

		if(query.getBands().size() == 1){

			((GDALBand)query.getBands().get(0)).getBand().ReadRaster(
					src.srcX,src.srcY, //src pos
					src.width, src.height, //src dim
					dstDim.getWidth(),dstDim.getHeight(), //dst dim
					DataType.toGDAL(query.getBands().get(0).datatype()), // the type of the pixel values in the array. 
					buffer.array());
		}else{
			int[] readBands = new int[query.getBands().size()];
			for(int i = 0; i < query.getBands().size();i++){
				readBands[i] = ((GDALBand)query.getBands().get(i)).getBand().GetBand();
			}
			dataset.ReadRaster(
					src.srcX,src.srcY, //src pos
					src.width, src.height, //src dim
					dstDim.getWidth(),dstDim.getHeight(), //dst dim
					DataType.toGDAL(query.getBands().get(0).datatype()), // the type of the pixel values in the array. 
					buffer.array(), //buffer to write in
					readBands);
		}
		//TODO what about the crs ?
		return new Raster(buffer, dstDim, query.getBands());
	}

	@Override
	protected void resampleBilinear(int[] srcPixels, int srcWidth,
			int srcHeight, int[] dstPixels, int dstWidth, int dstHeight) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void resampleBicubic(int[] srcPixels, int srcWidth,
			int srcHeight, int[] dstPixels, int dstWidth, int dstHeight) {
		// TODO Auto-generated method stub
		
	}
	
	

//	public void applyProjection(final String wkt){
//
//		SpatialReference dstRef = new SpatialReference(wkt);
//
//		Dataset vrt_ds = gdal.AutoCreateWarpedVRT(dataset,dataset.GetProjection(), dstRef.ExportToWkt());
//
//		dataset = vrt_ds;
//
//		this.mRasterWidth = dataset.GetRasterXSize();
//
//		this.mRasterHeight = dataset.getRasterYSize();
//
//	}
	
	//
//	public Callable<Dataset> saveCurrentProjectionToFile(final String newFileName){
//
//
//		Callable<Dataset> c =  new Callable<Dataset>() {
//			@Override
//			public Dataset call() throws Exception {
//
//				//		String fileName = mFilePath.substring(mFilePath.lastIndexOf("/") + 1);
//				//		fileName = fileName.substring(0, fileName.lastIndexOf("."))+"_reprojected"+fileName.substring(fileName.lastIndexOf("."));
//				final String newPath = mSource.substring(0,mSource.lastIndexOf("/") + 1) + newFileName;
//				Log.d(TAG, "saving to path "+newPath);
//				//		        return dataset.GetDriver().CreateCopy(newPath, dataset);
//				return dataset.GetDriver().Create(newPath,dataset.getRasterXSize(),dataset.GetRasterYSize(),dataset.getRasterCount());
//
//			}
//		};
//		return c;
//	}
}
