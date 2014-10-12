package de.rooehler.rastertheque;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import org.mapsforge.core.model.BoundingBox;

import android.util.Log;

public class GDALDecoder {
	
	private static final String TAG = GDALDecoder.class.getSimpleName();
	
	
	static {
    	System.loadLibrary("gdaljni");
    	System.loadLibrary("gdalconstjni");
        System.loadLibrary("osrjni");
        try {
            init();
        }
        catch(Throwable e) {
        	Log.e(GDALDecoder.class.getSimpleName(),"gdal initialization failed", e);
        }
    }
	
	public static void init() throws Throwable {
        if (gdal.GetDriverCount() == 0) {
            gdal.AllRegister();
        }
    }
	
	public static void open(String filePath){
		
		Dataset dataset = gdal.Open(filePath);

		if (dataset == null) {
			String lastErrMsg = gdal.GetLastErrorMsg();
			String msg = "Unable to open file: " + filePath;
			if (lastErrMsg != null) {
				msg += ", " + lastErrMsg;
			}
			Log.e(GDALDecoder.class.getSimpleName(), msg +"\n"+ lastErrMsg);
		}else{
			
			printProperties(dataset);
			
			Log.d(TAG,"BoundingBox : \n"+getBoundingBox(dataset).toString());
		}
	}
	
	public static void printProperties(Dataset dataset){
		
		Log.d(TAG, "GetRasterXSize " +dataset.getRasterXSize());
		Log.d(TAG, "GetRasterYSize " +dataset.getRasterYSize());
		
		Log.d(TAG, "GetRasterCount (Bands) " +dataset.GetRasterCount());
		Log.d(TAG, "GetFileList.size() " +dataset.GetFileList().size());
		
		Log.d(TAG, "GetProjection " +dataset.GetProjection());
		Log.d(TAG, "GetGCPProjection " +dataset.GetGCPProjection());

	}
	
	public static BoundingBox getBoundingBox(Dataset dataset){
		
		
		int width  = dataset.getRasterXSize();
		int	height = dataset.getRasterYSize();

		double[] gt = dataset.GetGeoTransform();
		double	minx = gt[0];
		double	miny = gt[3] + width*gt[4] + height*gt[5]; // from	http://gdal.org/gdal_datamodel.html
		double	maxx = gt[0] + width*gt[1] + height*gt[2]; // from	http://gdal.org/gdal_datamodel.html
		double	maxy = gt[3];
		
		
		//http://stackoverflow.com/questions/2922532/obtain-latitude-and-longitude-from-a-geotiff-file
		
		SpatialReference old_sr = new SpatialReference();
		old_sr.ImportFromWkt(dataset.GetProjectionRef());
		
		SpatialReference new_sr = new SpatialReference();
		new_sr.SetWellKnownGeogCS("WGS84");
		
		gdal.PushErrorHandler( "CPLQuietErrorHandler" );
			
		CoordinateTransformation ct =  CoordinateTransformation.CreateCoordinateTransformation(new_sr, old_sr);
	
		gdal.PopErrorHandler();
		
		if (ct != null){

			double[] minLatLong = ct.TransformPoint(minx, miny);

			double[] maxLatLong = ct.TransformPoint(maxx, maxy);
			return new BoundingBox(minLatLong[0], minLatLong[1], maxLatLong[0], maxLatLong[1]);
		}else{

			Log.e(TAG, gdal.GetLastErrorMsg());
			

			return null;

		}
		
	}

}
