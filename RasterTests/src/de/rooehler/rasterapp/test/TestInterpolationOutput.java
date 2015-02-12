package de.rooehler.rasterapp.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.os.Environment;
import android.util.Log;

import com.vividsolutions.jts.geom.Envelope;

import de.rooehler.rastertheque.core.Dataset;
import de.rooehler.rastertheque.core.Drivers;
import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.core.RasterQuery;
import de.rooehler.rastertheque.io.gdal.GDALDataset;
import de.rooehler.rastertheque.processing.Interpolation.ResampleMethod;
import de.rooehler.rastertheque.processing.RasterOp;
import de.rooehler.rastertheque.processing.resampling.OpenCVResampler;
import de.rooehler.rastertheque.util.Hints;
import de.rooehler.rastertheque.util.Hints.Key;


public class TestInterpolationOutput extends android.test.ActivityTestCase {
	
	
	public void testInterpolationMethods(){

		Dataset dataset = null;
		try {
			dataset = Drivers.open(TestIO.LENA, null);
		} catch (IOException e) {
			Log.e(TestInterpolationOutput.class.getSimpleName(), "Error opening Lena");
		}

		assertNotNull(dataset);

		assertTrue(dataset instanceof GDALDataset);
		
		GDALDataset ds = (GDALDataset) dataset;
		
		assertTrue(ds.getDimension().getWidth() == 512);

		final int os = (int) ds.getDimension().getWidth();
		final int ts = os * 4;
		
		final int targetSize = ts * ts;
		
		final RasterQuery query = new RasterQuery(
	        		new Envelope(0, 512, 0, 512),
	        		ds.getCRS(),
	        		ds.getBands(),
	        		new Envelope(0, 512, 0, 512),
	        		ds.getBands().get(0).datatype());
	        
	    final Raster raster = ds.read(query);

		RasterOp resampler = new OpenCVResampler();	
		
		HashMap<Key,Serializable> resizeParams = new HashMap<>();

		resizeParams.put(Hints.KEY_SIZE, new Envelope(0, ts, 0, ts));
			
		resizeParams.put(Hints.KEY_INTERPOLATION, ResampleMethod.BILINEAR);

//		for(int i = 0; i < ResampleMethod.values().length; i++){

			int[] resampledPixels = new int[targetSize];

//			final ResampleMethod m = ResampleMethod.values()[i];
//			Log.d(TestInterpolationOutput.class.getSimpleName(), "testing "+m.name());

			resampler.execute(raster, resizeParams,null,null);
			
			raster.getData().asIntBuffer().get(resampledPixels);

			Bitmap bitmap = Bitmap.createBitmap(ts, ts, Config.ARGB_8888);
			bitmap.setPixels(resampledPixels, 0, ts, 0, 0, ts, ts);	

			saveImage(bitmap,resampler.getClass().getSimpleName());

//		}  
	}
	
	private void saveImage(Bitmap finalBitmap, final String name) {

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
	           finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
	           out.flush();
	           out.close();

	    } catch (Exception e) {
	    		Log.e(TestInterpolationOutput.class.getSimpleName(), "error saving "+name,e);
	    		throw new AssertionError("error saving name");
	    }
	}

}
