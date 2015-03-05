package de.rooehler.rasterapp.test;

import java.io.File;
import java.io.FileOutputStream;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.os.Environment;
import android.util.Log;
import de.rooehler.rastertheque.core.Raster;

/**
 * provides some utility methods for testing
 * 
 * @author Robert Oehler
 *
 */
public class TestUtil {

	/**
	 * converts a @param raster containing 3 bands of RGB values
	 * to ARGB pixels and subsequently saved the result to disk
	 * @param raster the raster to operate on
	 * @param ts the targetSize of the image
	 * @param fileName the name for the file to save
	 */
	public static void convert3bandByteRasterToPixels(Raster raster, final int ts, final String fileName){

		final int targetSize = ts * ts;
		final byte[] bytes = raster.getData().array().clone();

		final byte[] r_band  = new byte[targetSize];
		final byte[] g_band  = new byte[targetSize];
		final byte[] b_band  = new byte[targetSize];

		int count = 0;
		for(int i = 0; i < 3; i++){
			for(int j = 0; j < targetSize; j++){

				byte current = bytes[count];

				switch(i){
				case 0:
					r_band[j] = current;
					break;
				case 1:
					g_band[j] = current;
					break;
				case 2:
					b_band[j] = current;
					break;
				}

				count++;
			}
		}

		int[] pixels = new int[targetSize];

		for (int l = 0; l < targetSize; l++) {	

			byte r = r_band[l];
			byte g = g_band[l];
			byte b = b_band[l];

			pixels[l] = 0xff000000 | ((((int) r) << 16) & 0xff0000) | ((((int) g) << 8) & 0xff00) | ((int) b);
		}


		Bitmap bitmap = Bitmap.createBitmap(ts, ts, Config.ARGB_8888);
		bitmap.setPixels(pixels, 0, ts, 0, 0, ts, ts);	

		saveImage(bitmap,fileName);
	}
	/**
	 * saves a @param bitmap to disk to a folder named "rastertheque"
	 * using the provided filename @param name
	 * 
	 */
	public static void saveImage(final Bitmap bitmap, final String name) {

		String root = Environment.getExternalStorageDirectory().toString();
		File myDir = new File(root + "/rastertheque");    
		if(!myDir.exists()){
			myDir.mkdirs();
		}

		String fname = name +".png";
		File file = new File (myDir, fname);
		if (file.exists ()){
			file.delete (); 
		}
		try {
			FileOutputStream out = new FileOutputStream(file);
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
			out.flush();
			out.close();

		} catch (Exception e) {
			Log.e(TestInterpolationOutput.class.getSimpleName(), "error saving "+name,e);
			throw new AssertionError("error saving name");
		}
	}
}
