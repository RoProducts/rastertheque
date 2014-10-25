package de.rooehler.rastertheque.gdal;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Dimension;

import de.rooehler.rastertheque.util.RasterProperty;
import android.util.Log;

public class GDALDecoder {
	
	private static final String TAG = GDALDecoder.class.getSimpleName();
	
	private static Dataset dataset;
	
	
	static {
		System.loadLibrary("proj");
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
		
		dataset = gdal.Open(filePath);

		if (dataset == null) {
			String lastErrMsg = gdal.GetLastErrorMsg();
			String msg = "Unable to open file: " + filePath;
			if (lastErrMsg != null) {
				msg += ", " + lastErrMsg;
			}
			Log.e(TAG, msg +"\n"+ lastErrMsg);
		}else{
			
			Log.d(TAG, filePath.substring(filePath.lastIndexOf("/") + 1) +" successfully opened");
			
			printProperties();
		}
	}
	public static Dataset getCurrentDataSet(){
		
		return dataset;
	}
	public static RasterProperty getRasterProperties(final Dataset dataset){
		
		 double[] adfGeoTransform = new double[6];
		 
		 dataset.GetGeoTransform(adfGeoTransform);
		 
		 return new RasterProperty(dataset.GetRasterXSize(), dataset.getRasterYSize(), adfGeoTransform[1], adfGeoTransform[5]);
	}
	
	public static void printProperties(){
		
		Log.d(TAG, "GetRasterXSize " +dataset.getRasterXSize());
		Log.d(TAG, "GetRasterYSize " +dataset.getRasterYSize());
		
		Log.d(TAG, "GetRasterCount (Bands) " +dataset.GetRasterCount());
		Log.d(TAG, "GetFileList.size() " +dataset.GetFileList().size());
				
		Log.d(TAG, "GetProjection " +dataset.GetProjection());
		Log.d(TAG, "GetGCPProjection " +dataset.GetGCPProjection());
		
		 /* -------------------------------------------------------------------- */
		 /*      Report projection.                                              */
		 /* -------------------------------------------------------------------- */
		if (dataset.GetProjectionRef() != null) {
			SpatialReference hSRS;
			String pszProjection;

			pszProjection = dataset.GetProjectionRef();

			hSRS = new SpatialReference(pszProjection);
			if (hSRS != null && pszProjection.length() != 0) {
				String[] pszPrettyWkt = new String[1];

				hSRS.ExportToPrettyWkt(pszPrettyWkt, 0);
				Log.d(TAG, "Coordinate System is: "+pszPrettyWkt[0]);
			} else
				Log.d(TAG,"Coordinate System is `"+ dataset.GetProjectionRef() + "'");

			hSRS.delete();
		}
		
        /* -------------------------------------------------------------------- */
		/*      Report Geotransform.                                            */
		/* -------------------------------------------------------------------- */
		 double[] adfGeoTransform = new double[6];
		
		 dataset.GetGeoTransform(adfGeoTransform);
		 {
			 if (adfGeoTransform[2] == 0.0 && adfGeoTransform[4] == 0.0) {
				 Log.d(TAG,"Origin = (" + adfGeoTransform[0] + "," + adfGeoTransform[3] + ")");

				 Log.d(TAG,"Pixel Size = (" + adfGeoTransform[1] + "," + adfGeoTransform[5] + ")");
			 } else {
				 Log.d(TAG,"GeoTransform =");
				 Log.d(TAG,"  " + adfGeoTransform[0] + ", " + adfGeoTransform[1] + ", " + adfGeoTransform[2]);
				 Log.d(TAG,"  " + adfGeoTransform[3] + ", " + adfGeoTransform[4] + ", " + adfGeoTransform[5]);
			 }
		 }
		 
			/* -------------------------------------------------------------------- */
			/*      Report corners.                                                 */
			/* -------------------------------------------------------------------- */

			GDALInfoReportCorner(dataset, "Upper Left ", 0.0, 0.0);
			GDALInfoReportCorner(dataset, "Lower Left ", 0.0, dataset.getRasterYSize());
			GDALInfoReportCorner(dataset, "Upper Right", dataset.getRasterXSize(), 0.0);
			GDALInfoReportCorner(dataset, "Lower Right", dataset.getRasterXSize(), dataset.getRasterYSize());
			GDALInfoReportCorner(dataset, "Center     ", dataset.getRasterXSize() / 2.0, dataset.getRasterYSize() / 2.0);

	}

	
	public static BoundingBox getBoundingBox(){
		
		
		int width  = dataset.getRasterXSize();
		int	height = dataset.getRasterYSize();

		double[] gt = dataset.GetGeoTransform();
		double	minx = gt[0];
		double	miny = gt[3] + width*gt[4] + height*gt[5]; // from	http://gdal.org/gdal_datamodel.html
		double	maxx = gt[0] + width*gt[1] + height*gt[2]; // from	http://gdal.org/gdal_datamodel.html
		double	maxy = gt[3];
		

		
		//http://stackoverflow.com/questions/2922532/obtain-latitude-and-longitude-from-a-geotiff-file
		
		SpatialReference old_sr = new SpatialReference(dataset.GetProjectionRef());
		
		SpatialReference new_sr = new SpatialReference();
		new_sr.SetWellKnownGeogCS("WGS84");
		
//		final String WGS_84 = "GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\",6378137,298.257223563,AUTHORITY[\"EPSG\",\"7030\"]],AUTHORITY[\"EPSG\",\"6326\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\",0.01745329251994328,AUTHORITY[\"EPSG\",\"9122\"]],AUTHORITY[\"EPSG\",\"4326\"]]";
		
//		new_sr.ImportFromWkt(WGS_84);
			
		CoordinateTransformation ct =  CoordinateTransformation.CreateCoordinateTransformation(old_sr, new_sr);
		
		if (ct != null){

			double[] minLatLong = ct.TransformPoint(minx, miny);

			double[] maxLatLong = ct.TransformPoint(maxx, maxy);
			return new BoundingBox(minLatLong[1], minLatLong[0], maxLatLong[1], maxLatLong[0]);
		}else{

			Log.e(TAG, gdal.GetLastErrorMsg());
			

			return null;

		}
		
	}
	
	/************************************************************************/
	/*                        GDALInfoReportCorner()                        */
	/************************************************************************/

	static boolean GDALInfoReportCorner(Dataset hDataset, String corner_name,double x, double y)

	{
		double dfGeoX, dfGeoY;
		String pszProjection;
		double[] adfGeoTransform = new double[6];
		CoordinateTransformation hTransform = null;

		/* -------------------------------------------------------------------- */
		/*      Transform the point into georeferenced coordinates.             */
		/* -------------------------------------------------------------------- */
		hDataset.GetGeoTransform(adfGeoTransform);
		{
			pszProjection = hDataset.GetProjectionRef();

			dfGeoX = adfGeoTransform[0] + adfGeoTransform[1] * x + adfGeoTransform[2] * y;
			dfGeoY = adfGeoTransform[3] + adfGeoTransform[4] * x + adfGeoTransform[5] * y;
		}

		if (adfGeoTransform[0] == 0 && adfGeoTransform[1] == 0
				&& adfGeoTransform[2] == 0 && adfGeoTransform[3] == 0
				&& adfGeoTransform[4] == 0 && adfGeoTransform[5] == 0) {
			Log.d(TAG,"(" + x + "," + y + ")");
			return false;
		}

		/* -------------------------------------------------------------------- */
		/*      Report the georeferenced coordinates.                           */
		/* -------------------------------------------------------------------- */
		Log.d(TAG,"Raw : (" + dfGeoX + "," + dfGeoY + ") ");

		/* -------------------------------------------------------------------- */
		/*      Setup transformation to lat/long.                               */
		/* -------------------------------------------------------------------- */
		if (pszProjection != null && pszProjection.length() > 0) {
			SpatialReference hProj, hLatLong = null;

			hProj = new SpatialReference(pszProjection);
			if (hProj != null)
				hLatLong = hProj.CloneGeogCS();

			if (hLatLong != null) {
				/* New in GDAL 1.10. Before was "new CoordinateTransformation(srs,dst)". */
				hTransform = CoordinateTransformation.CreateCoordinateTransformation(hProj, hLatLong);
            }

			if (hProj != null)
				hProj.delete();
		}

		/* -------------------------------------------------------------------- */
		/*      Transform to latlong and report.                                */
		/* -------------------------------------------------------------------- */
		if (hTransform != null) {
			double[] transPoint = new double[3];
			hTransform.TransformPoint(transPoint, dfGeoX, dfGeoY, 0);
			Log.d(TAG,"WGS_84 : ("+ transPoint[0] +", "+transPoint[1]+ ")");
		}

		if (hTransform != null)
			hTransform.delete();

		return true;
	}

}
