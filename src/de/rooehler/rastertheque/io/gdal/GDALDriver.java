package de.rooehler.rastertheque.io.gdal;

import java.io.IOException;

import org.gdal.gdal.gdal;

import android.util.Log;
import de.rooehler.rastertheque.core.Driver;

public class GDALDriver implements Driver<GDALDataset> {
	
	private static final String TAG = GDALDriver.class.getSimpleName();
	
	
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

	@Override
	public String getName() {
		
		return "GDAL Driver";
	}

	@Override
	public boolean canOpen(String filePath) {
		
		org.gdal.gdal.Driver drv =  gdal.IdentifyDriver(filePath);
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
