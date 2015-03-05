package de.rooehler.rastertheque.io.gdal;

import java.io.IOException;

import org.gdal.gdal.gdal;

import android.util.Log;
import de.rooehler.rastertheque.core.Driver;

/**
 * GDALDriver connects the GDAL library to a GDALDataset
 * 
 * it registers all available GDAL drivers and tries to
 * open a file with them
 * 
 * @author Robert Oehler
 *
 */
public class GDALDriver implements Driver {
	
	private static final String TAG = GDALDriver.class.getSimpleName();
	
	/**
	 * load the GDAL library components when this class is loaded
	 */
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
	/**
	 * registers all GDAL drivers
	 * 
	 * @throws Throwable
	 */
	public static void init() throws Throwable {
		if (gdal.GetDriverCount() == 0) {
			gdal.AllRegister();
		}
	}

	@Override
	public String getName() {
		
		return "GDAL Driver";
	}

	/**
	 * returns if GDAL is able to load the file
	 * specified in @param filePath
	 * 
	 * if not a warning is printed
	 * 
	 * @param filePath the path to the file to open
	 * @return if this file can be opened
	 */
	@Override
	public boolean canOpen(String filePath) {
		
		org.gdal.gdal.Driver drv = gdal.IdentifyDriver(filePath);
		
        if (drv == null) {
            String msg = "Unable to locate driver";
            String lastErrMsg = gdal.GetLastErrorMsg();
            if (lastErrMsg != null) {
                msg += ": " + lastErrMsg;
            }

            Log.w(TAG, "cannot open file : "+filePath+ " error : "+msg);
            return false;
        }
        
        return true;
	}
	/**
	 * opens the file specified in @param filePath
	 * and creates a GDALDataset from it
	 * which is returned
	 * @param filePath the path to the file to open
	 * @return the GDALDataset or null if opening the file failed
	 */
	@Override
	public GDALDataset open(String filePath) throws IOException {
	
		org.gdal.gdal.Dataset dataset = gdal.Open(filePath);

		if (dataset == null) {
			String lastErrMsg = gdal.GetLastErrorMsg();
			String msg = "Unable to open file: " + filePath;
			if (lastErrMsg != null) {
				msg += ", " + lastErrMsg;
			}
			Log.e(TAG, msg +"\n"+ lastErrMsg);
			return null;
		}else{

			Log.d(TAG, filePath.substring(filePath.lastIndexOf("/") + 1) +" successfully opened");
			
			return new GDALDataset(filePath, dataset, this);
		}
	}

}
