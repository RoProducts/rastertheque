package de.rooehler.rasterapp.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
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
import de.rooehler.rastertheque.processing.resampling.JAIResampler;
import de.rooehler.rastertheque.processing.resampling.MResampler;
import de.rooehler.rastertheque.processing.resampling.OpenCVResampler;
import de.rooehler.rastertheque.util.Hints;
import de.rooehler.rastertheque.util.Hints.Key;


public class TestInterpolationOutput extends android.test.ActivityTestCase {
	
	
	public void testInterpolationMethods(){

		Dataset dataset = null;
		try {
			dataset = Drivers.open(TestIO.RGB_BANDS_BYTE, null);
		} catch (IOException e) {
			Log.e(TestInterpolationOutput.class.getSimpleName(), "Error opening three banded raster");
		}

		assertNotNull(dataset);

		assertTrue(dataset instanceof GDALDataset);
		
		GDALDataset ds = (GDALDataset) dataset;
		
		assertTrue(ds.getDimension().getWidth() == 21600);

		final int rf = 2345;
		final int os = 256;
		final int ts = os * 4; 
		
		final int targetSize = ts * ts;
		
		final RasterQuery query = new RasterQuery(
	        		new Envelope(rf, os, rf, os),
	        		ds.getCRS(),
	        		ds.getBands(),
	        		new Envelope(rf, os, rf, os),
	        		ds.getBands().get(0).datatype());
	        
	    final Raster raster = ds.read(query);
	    
	    final byte[] origBytes = raster.getData().array().clone();
	    
	    final ArrayList<RasterOp> ops = new ArrayList(){{
	    	add(new OpenCVResampler());
	    	add(new MResampler());
	    	add(new JAIResampler());
	    }};
		
		HashMap<Key,Serializable> resizeParams = new HashMap<>();

		resizeParams.put(Hints.KEY_SIZE, new Envelope(0, ts, 0, ts));
			
		boolean write = false;

		for(int i = 0; i < ops.size(); i++){
			final RasterOp resampler = ops.get(i);
				for(int j = 0; j < ResampleMethod.values().length; j++){
					
					long now = System.currentTimeMillis();

					final ResampleMethod m = ResampleMethod.values()[j];
					
					resizeParams.put(Hints.KEY_INTERPOLATION, m);
					
					resampler.execute(raster, resizeParams,null,null);
		
					assertTrue(raster.getData().array().length == targetSize * 3);
					
					Log.i(TestInterpolationOutput.class.getSimpleName(), "testing " + resampler.getClass().getSimpleName()+" with "+ m.name()+ " took : "+(System.currentTimeMillis() - now));
					
					if(write){
						convertToPixels(raster, ts, targetSize);
					}
					
					//return to initial state
					
					raster.setDimension(new Envelope(rf, os, rf, os));
					raster.setData(ByteBuffer.wrap(origBytes));
				}
		}


	}
	private void convertToPixels(Raster raster, final int ts, final int targetSize){
		
		final byte[] bytes = raster.getData().array().clone();
		
		final byte[] r_band  = new byte[targetSize];
		final byte[] g_band  = new byte[targetSize];
		final byte[] b_band  = new byte[targetSize];
		
//		raster.getData().get(r_band, 0, targetSize);
//		raster.getData().get(g_band, 0, targetSize);
//		raster.getData().get(b_band, 0, targetSize);
		
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

		saveImage(bitmap,"lena_resampled");
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
