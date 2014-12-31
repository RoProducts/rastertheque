package de.rooehler.rastertheque.io.gdal;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import org.osgeo.proj4j.CoordinateReferenceSystem;

import android.util.Log;
import de.rooehler.rastertheque.core.Band;
import de.rooehler.rastertheque.core.DataType;
import de.rooehler.rastertheque.core.Driver;
import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.core.RasterDataset;
import de.rooehler.rastertheque.core.RasterQuery;
import de.rooehler.rastertheque.core.model.BoundingBox;
import de.rooehler.rastertheque.core.model.Coordinate;
import de.rooehler.rastertheque.core.model.Dimension;
import de.rooehler.rastertheque.core.model.Rectangle;
import de.rooehler.rastertheque.proj.Proj;

public class GDALDataset implements RasterDataset{
	
	private static final String TAG = GDALDataset.class.getSimpleName();
	
	static {
		System.loadLibrary("proj");
		System.loadLibrary("gdaljni");
		System.loadLibrary("gdalconstjni");
		System.loadLibrary("osrjni");
		try {
			init();
		}
		catch(Throwable e) {
			Log.e(TAG,"gdal initialization failed", e);
		}
	}

	public static void init() throws Throwable {
		if (gdal.GetDriverCount() == 0) {
			gdal.AllRegister();
		}
	}

	private static Dataset dataset;
	
	private static BoundingBox mBB;
	
	private static Dimension mDimension;

	private String mSource;

//	private int mRasterWidth;
//	private int mRasterHeight;

//	private DataType mDatatype;

	public GDALDataset(final String pFilePath){

		open(pFilePath);
			
	}

	public boolean open(String filePath){

		dataset = gdal.Open(filePath);

		if (dataset == null) {
			String lastErrMsg = gdal.GetLastErrorMsg();
			String msg = "Unable to open file: " + filePath;
			if (lastErrMsg != null) {
				msg += ", " + lastErrMsg;
			}
			Log.e(TAG, msg +"\n"+ lastErrMsg);
			return false;
		}else{

			Log.d(TAG, filePath.substring(filePath.lastIndexOf("/") + 1) +" successfully opened");

		}
		this.mSource = filePath;
		
		getBoundingBox();
		
		return true;
	}
//	public void setup(final String pFilePath){
		

//		this.mRasterWidth = dataset.GetRasterXSize();
//
//		this.mRasterHeight = dataset.getRasterYSize();

//		SpatialReference hProj = new SpatialReference(dataset.GetProjectionRef());
		
//		SpatialReference hLatLong =  hProj.CloneGeogCS();


//		List<Band> bands = getBands();
//		mDatatype = DataType.BYTE;
//		
//		for (int i = 0 ; i < bands.size(); i++) {
//			Band band = bands.get(i);
//			GDALBand gdalBand = new GDALBand(band);
//			
//			mNodata = gdalBand.nodata();
//
//			DataType dt = DataType.getDatatype(band);
//			if (dt.compareTo(mDatatype) > 0) {
//				mDatatype = dt;
//			}
//		}
		
		
//	}
//
//	public String getProjection() {
//
//		return dataset.GetProjectionRef();
//	}
	@Override
	public void close(){
		if (dataset != null) {
			dataset.delete();
			dataset = null;
		}
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
	
	public String toWKT(CoordinateReferenceSystem crs) {
		SpatialReference ref = new SpatialReference();
		ref.ImportFromProj4(Proj.toString(crs));
		return ref.ExportToWkt();
	}
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

	@Override
	public List<Band> getBands(){
		int nbands = dataset.GetRasterCount();

		List<Band> bands = new ArrayList<Band>(nbands);
		for (int i = 1; i <= nbands; i++) {
			bands.add(new GDALBand(dataset.GetRasterBand(i)));
		}

		return bands;
	}

//	public DataType getDatatype(){
//		return mDatatype;
//	}

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

				Log.e(TAG, gdal.GetLastErrorMsg());	

				return null;

			}
		}else{
			return mBB;
		}
	}
	
	public Coordinate getCenterPoint(){

		return getBoundingBox().getCenter();
	}
//	/**
//	 * returns a float indicating the ratio between width and height of this raster which will be
//	 * 1.0 is width and heigth are equal
//	 * > 1, the ratio the width is larger  than the height
//	 * < 0, the ratio the width is smaller than the height
//	 * @return
//	 */
//	public float getWidthHeightRatio(){
//
//		int rasterWidth = dataset.GetRasterXSize();
//
//		int rasterHeight = dataset.getRasterYSize();
//
//		return (float) rasterWidth / rasterHeight;
//	}

//	public int getRasterWidth() {
//		return mRasterWidth;
//	}
//
//
//	public int getRasterHeight() {
//		return mRasterHeight;
//	}

	@Override
	public String getSource() {

		return mSource;
	}
	@Override
	public CoordinateReferenceSystem getCRS() {

		String proj = dataset.GetProjection();
		if (proj != null) {
			SpatialReference ref = new SpatialReference(proj);
			return Proj.crs(ref.ExportToProj4());
		}
		return null;
	}
	@Override
	public Driver getDriver() {
		// TODO Auto-generated method stub
		return null;
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
}
