package de.rooehler.rasterapp.test;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import android.graphics.Rect;
import android.util.Log;

import com.vividsolutions.jts.geom.Envelope;

import de.rooehler.rastertheque.core.Dataset;
import de.rooehler.rastertheque.core.Drivers;
import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.core.RasterQuery;
import de.rooehler.rastertheque.io.gdal.GDALDataset;
import de.rooehler.rastertheque.io.gdal.GDALRasterQuery;
import de.rooehler.rastertheque.processing.Interpolation.ResampleMethod;
import de.rooehler.rastertheque.processing.RasterOp;
import de.rooehler.rastertheque.processing.resampling.JAIResampler;
import de.rooehler.rastertheque.processing.resampling.MResampler;
import de.rooehler.rastertheque.processing.resampling.OpenCVResampler;
import de.rooehler.rastertheque.processing.resampling.Resampler;
import de.rooehler.rastertheque.util.Hints;
import de.rooehler.rastertheque.util.Hints.Key;

/**
 * tests the output of a resampling on RGB raster file
 * which does not need to be symbolized
 * 
 * this way the operation result can be verified visually
 * 
 * and differences between interpolation methods can be analyzed
 * 
 * use the flag write to write the result to disk
 * 
 * @author Robert Oehler
 *
 */
public class TestInterpolationOutput extends android.test.ActivityTestCase {
	
	final boolean write = false;

	
	public void testInterpolationMethods(){

		final File file = TestUtil.createFileFromAssets(getInstrumentation().getContext(),TestIO.TEST_RGB_BANDS_BYTE);
		
		assertNotNull(file);
		
		Dataset dataset = null;
		try {
			dataset = Drivers.open(file.getAbsolutePath(), null);
		} catch (IOException e) {
			Log.e(TestInterpolationOutput.class.getSimpleName(), "Error opening three banded raster");
		}

		assertNotNull(dataset);

		assertTrue(dataset instanceof GDALDataset);
		
		GDALDataset ds = (GDALDataset) dataset;
		
		assertTrue(ds.getDimension().right == 1000);

		final int rf = 0;
		final int os = 256;
		final int ts = os * 4; 
		
		final Envelope srcEnv = new Envelope(rf, rf + os, rf, rf + os);
		final Rect rect = new Rect(rf,rf,rf+ os,rf+os);
		
		final int targetSize = ts * ts;
		
		final RasterQuery query = new GDALRasterQuery(
	        		srcEnv,
	        		ds.getCRS(),
	        		ds.getBands(),
	        		rect,
	        		ds.getBands().get(0).datatype(),
	        		rect);
	        
	    final Raster raster = ds.read(query);
	    
	    final byte[] origBytes = raster.getData().array().clone();
	    
		final ArrayList<RasterOp> ops = new ArrayList(){{
	    	add(new OpenCVResampler());
	    	add(new MResampler());
	    	add(new JAIResampler());
	    }};
		
		HashMap<Key,Serializable> resizeParams = new HashMap<>();

		resizeParams.put(Resampler.KEY_SIZE, new Double[]{ts / srcEnv.getWidth(), ts / srcEnv.getHeight()});
			

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
						TestUtil.convert3bandByteRasterToPixels(raster, ts, "RGB_BANDS_"+ resampler.getClass().getSimpleName()+"_"+m.toString()+"_Resampled");
					}
					
					//return to initial state
					
					raster.setDimension(rect);
					raster.setData(ByteBuffer.wrap(origBytes));
				}
		}

		TestUtil.deletefile(file);
	}
}
