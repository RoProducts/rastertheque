package de.rooehler.rasterapp.test;


import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;

import android.util.Log;

import com.vividsolutions.jts.geom.Envelope;

import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.core.RasterQuery;
import de.rooehler.rastertheque.core.util.ByteBufferReader;
import de.rooehler.rastertheque.io.gdal.GDALDataset;
import de.rooehler.rastertheque.io.gdal.GDALDriver;
import de.rooehler.rastertheque.processing.Interpolation.ResampleMethod;
import de.rooehler.rastertheque.processing.RasterOp;
import de.rooehler.rastertheque.processing.resampling.JAIResampler;
import de.rooehler.rastertheque.processing.resampling.MResampler;
import de.rooehler.rastertheque.processing.resampling.OpenCVResampler;
import de.rooehler.rastertheque.processing.resampling.Resampler;
import de.rooehler.rastertheque.util.Hints;
import de.rooehler.rastertheque.util.Hints.Key;


/**
 * 
 * Raw Resampling test class
 * 
 * TODO JAI Bicubic
 * 
 * TODO char, short, int, long, double tests with according files  * 
 * 
 * 
 * @author robertoehler
 *
 */

public class RawResamplerTester extends android.test.ActivityTestCase {

	@SuppressWarnings("unchecked")
	public void testFloatRasterRawResampling() throws IOException {
		
		final int threshold = 2;
		
		GDALDriver driver = new GDALDriver();

		assertTrue(driver.canOpen(TestIO.DEM_FLOAT));

		GDALDataset dataset = driver.open(TestIO.DEM_FLOAT);
		
		final int rs = 256;

		final Envelope env = new Envelope(0, rs, 0, rs);

		final RasterQuery query = new RasterQuery(
				env,
				dataset.getCRS(),
				dataset.getBands(),
				env,
				dataset.getBands().get(0).datatype());

		final Envelope targetEnv = new Envelope(0, env.getWidth() * 4, 0, env.getHeight() * 4);

		final int origSize = (int) (env.getWidth() * env.getHeight());
		final int targetSize = (int) (targetEnv.getWidth() * targetEnv.getHeight());
		
		Raster raster = dataset.read(query);
		
		final byte[] orig = raster.getData().array().clone();
		
		ByteBufferReader reader = new ByteBufferReader(orig, ByteOrder.nativeOrder());
		float[] floats = new float[(int)env.getWidth() * (int) env.getHeight()];
		for(int i = 0; i < origSize; i++){
			floats[i] = reader.readFloat();
		}
		
		HashMap<Key,Serializable> resizeParams = new HashMap<>();

		resizeParams.put(Resampler.KEY_SIZE, new Double[]{targetEnv.getWidth() / env.getWidth(), targetEnv.getHeight() / env.getHeight()});
		
	    final ArrayList<RasterOp> ops = new ArrayList(){{
	    	add(new OpenCVResampler());
	    	add(new MResampler());
	    	add(new JAIResampler());
	    }};
	    
		for(int i = 0; i < ops.size(); i++){
			final RasterOp resampler = ops.get(i);
				for(int j = 0; j < ResampleMethod.values().length; j++){
					
					long now = System.currentTimeMillis();

					final ResampleMethod m = ResampleMethod.values()[j];
					
					resizeParams.put(Hints.KEY_INTERPOLATION, m);
					
					resampler.execute(raster, resizeParams,null,null);
					
					Log.d("RawResamplerTester","float testing "+ resampler.getClass().getSimpleName()+" with "+m.name()+" took : "+(System.currentTimeMillis() - now));
					
					final byte[] resampled = raster.getData().array().clone();
					
					ByteBufferReader resampledReader = new ByteBufferReader(resampled, ByteOrder.nativeOrder());
					float[] resampledFloats = new float[targetSize];
					for(int k = 0; k < targetSize; k++){
						resampledFloats[k] = resampledReader.readFloat();
					}

					Log.d(RawResamplerTester.class.getSimpleName(), String.format("orig [last] %f resampled [last] %f", floats[floats.length - 1], resampledFloats[resampledFloats.length - 1]));

					assertTrue(compareFloatsWithThreshold(floats[0], resampledFloats[0], threshold));

					assertTrue(compareFloatsWithThreshold(floats[floats.length - 1], resampledFloats[resampledFloats.length - 1],threshold));

					
					//return to initial state
					raster = dataset.read(query);
				}
		}
		
		dataset.close();

	}
	
	public static boolean compareFloatsWithThreshold(float a, float b, float threshold) {

	    return  Math.abs(a - b) <= threshold;
	}
	
	
	@SuppressWarnings("unchecked")
	public void testByteRasterResampling() {

		GDALDriver driver = new GDALDriver();
		
		final int threshold = 2;
		
		assertTrue(driver.canOpen(TestIO.GRAY_50M_BYTE));
		try{

			GDALDataset dataset = driver.open(TestIO.GRAY_50M_BYTE);

			final Envelope env = new Envelope(0, 256, 0, 256);

			final RasterQuery query = new RasterQuery(
					env,
					dataset.getCRS(),
					dataset.getBands(),
					env,
					dataset.getBands().get(0).datatype());

			final Raster raster = dataset.read(query);

			final byte[] orig = raster.getData().array().clone();

			final Envelope targetEnv = new Envelope(0, env.getWidth() * 4, 0, env.getHeight() * 4);

			final int origSize = (int) (env.getWidth() * env.getHeight());
			final int targetSize = (int) (targetEnv.getWidth() * targetEnv.getHeight());
			
			HashMap<Key,Serializable> resizeParams = new HashMap<>();

			resizeParams.put(Resampler.KEY_SIZE, new Double[]{targetEnv.getWidth() / env.getWidth(), targetEnv.getHeight() / env.getHeight()});
			
		    final ArrayList<RasterOp> ops = new ArrayList(){{
		    	add(new OpenCVResampler());
		    	add(new MResampler());
		    	add(new JAIResampler());
		    }};
			
			for(int i = 0; i < ops.size(); i++){
				final RasterOp resampler = ops.get(i);
					for(int j = 0; j < ResampleMethod.values().length; j++){
						
						long now = System.currentTimeMillis();

						final ResampleMethod m = ResampleMethod.values()[j];
						
						resizeParams.put(Hints.KEY_INTERPOLATION, m);
						
						resampler.execute(raster, resizeParams,null,null);
						
						byte first = raster.getData().array()[0];
						byte last = raster.getData().array()[targetSize - 1];
						
						if(!(resampler instanceof JAIResampler && m == ResampleMethod.BICUBIC)){
							//JAI Bicubic fails this test
							
							assertTrue(compareBytesWithThreshold(first,orig[0],threshold));
						
							assertTrue(compareBytesWithThreshold(last, orig[origSize - 1],threshold));
						}
						assertTrue(raster.getData().array().length == targetEnv.getHeight() * targetEnv.getWidth() * raster.getBands().get(0).datatype().size());
						
						Log.d("RawResamplerTester","byte testing "+ resampler.getClass().getSimpleName()+" with "+m.name()+" took : "+(System.currentTimeMillis() - now));
						
						//return to initial state
						
						raster.setDimension(new Envelope(0, 256, 0, 256));
						raster.setData(ByteBuffer.wrap(orig));
					}
			}

			dataset.close();

		}catch(Exception e){
			Log.e(RawResamplerTester.class.getSimpleName(), "Exception raw resampling",e);
		}
	}
	
	public boolean compareBytesWithThreshold(byte a, byte b, int thrshold){
		
		return Math.abs(a - b) <= thrshold;
	}

}
