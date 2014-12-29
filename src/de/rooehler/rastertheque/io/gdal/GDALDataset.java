package de.rooehler.rastertheque.io.gdal;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import org.osgeo.proj4j.CoordinateReferenceSystem;

import android.util.Log;
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
import de.rooehler.rastertheque.util.Constants;

public class GDALDataset  implements RasterDataset{

	private static final String TAG = GDALDataset.class.getSimpleName();

	private static Dataset dataset;

	private String mSource;


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


	private int mRasterWidth;
	private int mRasterHeight;

	private DataType mDatatype;
	
	private double mNoData;

	public GDALDataset(){
		//for tests only
	}
	public GDALDataset(final String pFilePath){

		open(pFilePath);

		setup(pFilePath);
		
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
		return true;
	}
	public void setup(final String pFilePath){
		
		this.mSource = pFilePath;

		this.mRasterWidth = dataset.GetRasterXSize();

		this.mRasterHeight = dataset.getRasterYSize();

		//		SpatialReference hProj = new SpatialReference(dataset.GetProjectionRef());
		//		
		//		SpatialReference hLatLong =  hProj.CloneGeogCS();


		List<Band> bands = getBands();
		mDatatype = DataType.BYTE;
		for (int i = 0 ; i < bands.size(); i++) {
			Band band = bands.get(i);
			
			Double nodata[] = new Double[1];
			band.GetNoDataValue(nodata);
			
			if(nodata[0] != null){				
				mNoData = nodata[0];
			}

			DataType dt = DataType.getDatatype(band);
			if (dt.compareTo(mDatatype) > 0) {
				mDatatype = dt;
			}
		}
		
	}

	public String getProjection() {

		return dataset.GetProjectionRef();
	}
	@Override
	public void close(){
		if (dataset != null) {
			dataset.delete();
			dataset = null;
		}
	}

//	@Override
//	public void read(
//			final Rectangle src,
//			final Dimension dstDim,
//			final ByteBuffer buffer){
//		
//
//		List<Band> bands = getBands();
//		int[] readBands = new int[bands.size()];
//		for(int i = 0; i < bands.size();i++){
//			readBands[i] = bands.get(i).GetBand();
//		}
//
//		if(readBands.length == 1){
//
//			bands.get(0).ReadRaster(
//					src.srcX,src.srcY, //src pos
//					src.width, src.height, //src dim
//					dstDim.getWidth(),dstDim.getHeight(), //dst dim
//					DataType.toGDAL(getDatatype()), // the type of the pixel values in the array. 
//					buffer.array());
//		}else{
//			dataset.ReadRaster(
//					src.srcX,src.srcY, //src pos
//					src.width, src.height, //src dim
//					dstDim.getWidth(),dstDim.getHeight(), //dst dim
//					DataType.toGDAL(getDatatype()), // the type of the pixel values in the array. 
//					buffer.array(), //buffer to write in
//					readBands);
//		}
//
//
//
//	}
//
//	@Override
//	public void read(final Rectangle src,final ByteBuffer buffer){
//		read(src, new Dimension(src.width, src.height), buffer);
//	}
	
//	public void readFromBand(Band band, final Rectangle src,final Dimension dstDim,final ByteBuffer buffer){
//		
//		band.ReadRaster_Direct(
//				src.srcX,src.srcY, //src pos
//				src.width, src.height, //src dim
//				dstDim.getWidth(),dstDim.getHeight(), //dst dim
//				DataType.toGDAL(getDatatype()), // the type of the pixel values in the array. 
//				buffer //buffer to write in
//				);
//	}
	

	public void applyProjection(String wkt){

		SpatialReference dstRef = new SpatialReference(wkt);

		Dataset vrt_ds = gdal.AutoCreateWarpedVRT(dataset,dataset.GetProjection(), dstRef.ExportToWkt());

		dataset = vrt_ds;

		this.mRasterWidth = dataset.GetRasterXSize();

		this.mRasterHeight = dataset.getRasterYSize();

	}
	
	public String toWKT(CoordinateReferenceSystem crs) {
		SpatialReference ref = new SpatialReference();
		ref.ImportFromProj4(Proj.toString(crs));
		return ref.ExportToWkt();
	}

	public Callable<Dataset> saveCurrentProjectionToFile(final String newFileName){


		Callable<Dataset> c =  new Callable<Dataset>() {
			@Override
			public Dataset call() throws Exception {

				//		String fileName = mFilePath.substring(mFilePath.lastIndexOf("/") + 1);
				//		fileName = fileName.substring(0, fileName.lastIndexOf("."))+"_reprojected"+fileName.substring(fileName.lastIndexOf("."));
				final String newPath = mSource.substring(0,mSource.lastIndexOf("/") + 1) + newFileName;
				Log.d(TAG, "saving to path "+newPath);
				//		        return dataset.GetDriver().CreateCopy(newPath, dataset);
				return dataset.GetDriver().Create(newPath,dataset.getRasterXSize(),dataset.GetRasterYSize(),dataset.getRasterCount());

			}
		};
		return c;
	}

	@Override
	public List<Band> getBands(){
		int nbands = dataset.GetRasterCount();

		List<Band> bands = new ArrayList<Band>(nbands);
		for (int i = 1; i <= nbands; i++) {
			bands.add(dataset.GetRasterBand(i));
		}

		return bands;
	}

	public DataType getDatatype(){
		return mDatatype;
	}

	@Override
	public BoundingBox getBoundingBox(){


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
	
			return new BoundingBox(minLatLong[0], minLatLong[1], maxLatLong[0], maxLatLong[1]);
		}else{

			Log.e(TAG, gdal.GetLastErrorMsg());	

			return null;

		}
	}
	
	public Coordinate getCenterPoint(){

		double[] adfGeoTransform = new double[6];

		final SpatialReference hProj = new SpatialReference(dataset.GetProjectionRef());

		final SpatialReference hLatLong =  new SpatialReference(Constants.EPSG_4326);// hProj.CloneGeogCS();

		final CoordinateTransformation transformation = CoordinateTransformation.CreateCoordinateTransformation(hProj, hLatLong);
		dataset.GetGeoTransform(adfGeoTransform);

		if (adfGeoTransform[2] == 0.0 && adfGeoTransform[4] == 0.0) {

			double dfGeoX = adfGeoTransform[0] + adfGeoTransform[1] * (dataset.GetRasterXSize() / 2) + adfGeoTransform[2] * (dataset.GetRasterYSize() / 2);
			double dfGeoY = adfGeoTransform[3] + adfGeoTransform[4] * (dataset.GetRasterXSize() / 2) + adfGeoTransform[5] * (dataset.GetRasterYSize() / 2);
			if(transformation != null){

				double[] transPoint = new double[3];
				transformation.TransformPoint(transPoint, dfGeoX, dfGeoY, 0);
				Log.d(TAG,"Origin : ("+ transPoint[0] +", "+transPoint[1]+ ")");
				return  new Coordinate(transPoint[0],transPoint[1]);
				
			}else{
				return new Coordinate(dfGeoX, dfGeoY);
			}
			

		}else{
			//this is rotated
			//TODO handle
			throw new IllegalArgumentException("unexpected transformation");
		}
	}
	/**
	 * returns a float indicating the ratio between width and height of this raster which will be
	 * 1.0 is width and heigth are equal
	 * > 1, the ratio the width is larger  than the height
	 * < 0, the ratio the width is smaller than the height
	 * @return
	 */
	public float getWidthHeightRatio(){

		int rasterWidth = dataset.GetRasterXSize();

		int rasterHeight = dataset.getRasterYSize();

		return (float) rasterWidth / rasterHeight;
	}

	public int getRasterWidth() {
		return mRasterWidth;
	}


	public int getRasterHeight() {
		return mRasterHeight;
	}

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
		
		return new Dimension(mRasterWidth, mRasterHeight);
	}
	@Override
	public Raster read(RasterQuery query) {
		
		Rectangle src = query.getBounds();
		Dimension dstDim = query.getSize();
		
		final int bufferSize = dstDim.getSize() * query.getDataType().size() * query.getBands().size();
		
		final ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
		buffer.order(ByteOrder.nativeOrder()); 

		if(query.getBands().size() == 1){

			query.getBands().get(0).ReadRaster(
					src.srcX,src.srcY, //src pos
					src.width, src.height, //src dim
					dstDim.getWidth(),dstDim.getHeight(), //dst dim
					DataType.toGDAL(getDatatype()), // the type of the pixel values in the array. 
					buffer.array());
		}else{
			int[] readBands = new int[query.getBands().size()];
			for(int i = 0; i < query.getBands().size();i++){
				readBands[i] = query.getBands().get(i).GetBand();
			}
			dataset.ReadRaster(
					src.srcX,src.srcY, //src pos
					src.width, src.height, //src dim
					dstDim.getWidth(),dstDim.getHeight(), //dst dim
					DataType.toGDAL(getDatatype()), // the type of the pixel values in the array. 
					buffer.array(), //buffer to write in
					readBands);
		}
		//TODO what about the crs ?
		return new Raster(buffer, dstDim, query.getBands());
	}
}
