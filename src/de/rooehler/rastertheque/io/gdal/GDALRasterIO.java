package de.rooehler.rastertheque.io.gdal;

import java.io.EOFException;
import java.io.IOException;
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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

import de.rooehler.rastertheque.RasterIO;
import de.rooehler.rastertheque.core.DataContainer;
import de.rooehler.rastertheque.core.Dimension;
import de.rooehler.rastertheque.core.RasterDataSet;
import de.rooehler.rastertheque.core.Rectangle;
import de.rooehler.rastertheque.proj.Proj;
import de.rooehler.rastertheque.util.Constants;

public class GDALRasterIO  implements RasterDataSet, RasterIO{

	private static final String TAG = GDALRasterIO.class.getSimpleName();

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

	public GDALRasterIO(final String pFilePath){

		open(pFilePath);

		this.mSource = pFilePath;

		this.mRasterWidth = dataset.GetRasterXSize();

		this.mRasterHeight = dataset.getRasterYSize();
		//		
		//		SpatialReference hProj = new SpatialReference(dataset.GetProjectionRef());
		//		
		//		SpatialReference hLatLong =  hProj.CloneGeogCS();


		List<Band> bands = getBands();
		mDatatype = DataType.BYTE;
		for (int i = 0 ; i < bands.size(); i++) {
			Band band = bands.get(i);
			DataType dt = DataType.getDatatype(band);
			if (dt.compareTo(mDatatype) > 0) {
				mDatatype = dt;
			}
		}
	}
	@Override
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
	@Override
	public void close(){
		if (dataset != null) {
			dataset.delete();
			dataset = null;
		}
	}

	@Override
	public void read(
			final Rectangle src,
			final Dimension dstDim,
			final ByteBuffer buffer){
		

		List<Band> bands = getBands();
		int[] readBands = new int[bands.size()];
		for(int i = 0; i < bands.size();i++){
			readBands[i] = bands.get(i).GetBand();
		}

		if(readBands.length == 1){
			dataset.ReadRaster_Direct(
					src.srcX,src.srcY, //src pos
					src.width, src.height, //src dim
					dstDim.getWidth(),dstDim.getHeight(), //dst dim
					DataType.toGDAL(getDatatype()), // the type of the pixel values in the array. 
					buffer, //buffer to write in
					readBands, //the list of band numbers being read/written. Note band numbers are 1 based. This may be null to select the first nBandCount bands.
					0, //The byte offset from the start of one pixel value in the buffer to the start of the next pixel value within a scanline. If defaulted (0) the size of the datatype buf_type is used.
					0, //The byte offset from the start of one scanline in the buffer to the start of the next. If defaulted the size of the datatype buf_type * buf_xsize is used.
					0  //the byte offset from the start of one bands data to the start of the next. If defaulted (zero) the value will be nLineSpace * buf_ysize implying band sequential organization of the data buffer.
					);
		}else{

			dataset.ReadRaster_Direct(src.srcX,src.srcY,
					src.width, src.height,
					dstDim.getWidth(),dstDim.getHeight(),
					DataType.toGDAL(getDatatype()),
					buffer,
					readBands,
					getDatatype().size(),
					0, 
					1 
					);
		}


	}

	@Override
	public void read(final Rectangle src,final ByteBuffer buffer){
		read(src, new Dimension(src.width, src.height), buffer);
	}

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


	private Envelope getMEnvelope(){


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
			return new Envelope(minLatLong[0],minLatLong[1], maxLatLong[0], maxLatLong[1]);
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

//	public byte getStartZoomLevel(Coordinate center, final int tileSize){
//
//		double[] adfGeoTransform = new double[6];
//
//		dataset.GetGeoTransform(adfGeoTransform);
//
//		if (adfGeoTransform[2] == 0.0 && adfGeoTransform[4] == 0.0) {
//
//			SpatialReference sr = new SpatialReference(dataset.GetProjectionRef());
//			Log.d(TAG, "Unit " + sr.GetLinearUnitsName());
//
//
//			int width =  dataset.GetRasterXSize();
//			int height =  dataset.GetRasterYSize();
//
//			int max = Math.max(width, height);
//
//			double tilesEnter = (double) max / tileSize;
//
//			double zoom = Math.log(tilesEnter) / Math.log(2);
//
//			return (byte) Math.max(2,Math.round(zoom));
//
//
//
//			//				 if(sr.GetLinearUnitsName().toLowerCase().equals("degree")){
//			//					 Log.d(TAG,"Pixel Size = (" + adfGeoTransform[1] + "," + adfGeoTransform[5] + ")");
//			//					 float xResInMeters = (float) (adfGeoTransform[1] * distanceOfOneDegreeOfLongitudeAccordingToLatitude(center.latitude));
//			//					 float yResInMeters = (float) (adfGeoTransform[5] * Constants.DEGREE_IN_METERS_AT_EQUATOR);
//			//					 Log.d(TAG,"(xResInMeters " + xResInMeters + ", yResInMeters " + Math.abs(yResInMeters) + ")");
//			//					 return zoomLevelAccordingToMetersPerPixel(Math.min(Math.abs(xResInMeters), Math.abs(yResInMeters)));
//			//				 }else if(sr.GetLinearUnitsName().toLowerCase().equals("metre") || sr.GetLinearUnitsName().toLowerCase().equals("meter")){
//			//					 return zoomLevelAccordingToMetersPerPixel((float)Math.min(Math.abs(adfGeoTransform[1]),Math.abs(adfGeoTransform[5])));
//			//				 }
//
//		}
//		//invalid data	 
//		throw new IllegalArgumentException("unexpected transformation");
//
//	}

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
	public Envelope getEnvelope() {
		
		return getMEnvelope();
	}
}
