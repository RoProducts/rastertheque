package de.rooehler.rasterapp.test;


import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;

import android.graphics.Rect;
import android.util.Log;

import com.vividsolutions.jts.geom.Envelope;

import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.core.RasterQuery;
import de.rooehler.rastertheque.core.util.ByteBufferReader;
import de.rooehler.rastertheque.io.gdal.GDALDataset;
import de.rooehler.rastertheque.io.gdal.GDALDriver;
import de.rooehler.rastertheque.io.gdal.GDALRasterQuery;
import de.rooehler.rastertheque.processing.Interpolation.ResampleMethod;
import de.rooehler.rastertheque.processing.RasterOp;
import de.rooehler.rastertheque.processing.resampling.MResampler;
import de.rooehler.rastertheque.processing.resampling.OpenCVResampler;
import de.rooehler.rastertheque.processing.resampling.Resampler;
import de.rooehler.rastertheque.util.Hints;
import de.rooehler.rastertheque.util.Hints.Key;


/**
 * 
 * Raw Resampling test 
 * 
 * Tests all data types for correct resampling
 * 
 * byte and float are tested by all three resampling implementations
 * 
 * the other data types which are less frequent only by OpenCV
 * 
 * @author Robert Oehler
 *
 */

public class RawResamplerTester extends android.test.ActivityTestCase {
	
	// 514 x 515
	public final static String TEST_BYTE =  "cea.tif";
	public final static String TEST_CHAR =  "cea_int16.tif";
	public final static String TEST_SHORT = "cea_uint16.tif";
	public final static String TEST_INT =   "cea_int32.tif";
	public final static String TEST_LONG =  "cea_uint32.tif";
	public final static String TEST_FLOAT = "hn_sub.tif";

	
	public void testCharRawResampling() throws IOException {
		
		final int threshold = 2;
		
		GDALDriver driver = new GDALDriver();
		
		final File file = TestUtil.createFileFromAssets(getInstrumentation().getContext(),TEST_CHAR);
		
		assertTrue(driver.canOpen(file.getAbsolutePath()));

		GDALDataset dataset = driver.open(file.getAbsolutePath());
		
		final int rs = 256;

		final Envelope env = new Envelope(0, rs, 0, rs);
		final Rect rect = new Rect(0, 0, rs, rs);

		final RasterQuery query = new GDALRasterQuery(
				env,
				dataset.getCRS(),
				dataset.getBands(),
				rect,
				dataset.getBands().get(0).datatype(),
				rect);

		final Envelope targetEnv = new Envelope(0, env.getWidth() * 4, 0, env.getHeight() * 4);

		final int origSize = (int) (env.getWidth() * env.getHeight());
		final int targetSize = (int) (targetEnv.getWidth() * targetEnv.getHeight());
		
		Raster raster = dataset.read(query);
		
		final byte[] orig = raster.getData().array().clone();
		
		ByteBufferReader reader = new ByteBufferReader(orig, ByteOrder.nativeOrder());
		char[] chars = new char[(int)env.getWidth() * (int) env.getHeight()];
		for(int i = 0; i < origSize; i++){
			chars[i] = reader.readChar();
		}
		
		HashMap<Key,Serializable> resizeParams = new HashMap<>();

		resizeParams.put(Resampler.KEY_SIZE, new Double[]{targetEnv.getWidth() / env.getWidth(), targetEnv.getHeight() / env.getHeight()});
		
		final RasterOp resampler = new OpenCVResampler();
		for(int j = 0; j < ResampleMethod.values().length; j++){

			long now = System.currentTimeMillis();

			final ResampleMethod m = ResampleMethod.values()[j];

			resizeParams.put(Hints.KEY_INTERPOLATION, m);

			resampler.execute(raster, resizeParams,null,null);

			Log.d("RawResamplerTester","char testing with "+m.name()+" took : "+(System.currentTimeMillis() - now));

			final byte[] resampled = raster.getData().array().clone();

			ByteBufferReader resampledReader = new ByteBufferReader(resampled, ByteOrder.nativeOrder());
			char[] resampledChars = new char[targetSize];
			for(int k = 0; k < targetSize; k++){
				resampledChars[k] = resampledReader.readChar();
			}

			Log.d(RawResamplerTester.class.getSimpleName(), String.format("orig [last] %c resampled [last] %c", chars[chars.length - 1], resampledChars[resampledChars.length - 1]));

			assertTrue(compareFloatsWithThreshold(chars[0], resampledChars[0], threshold));
			
			assertTrue(compareFloatsWithThreshold(chars[chars.length - 1], resampledChars[resampledChars.length - 1], threshold));

			//return to initial state
			raster = dataset.read(query);
		}

		
		dataset.close();
		
		TestUtil.deletefile(file);

	}
	
	public void testShortRawResampling() throws IOException {
		
		final int threshold = 2;
		
		GDALDriver driver = new GDALDriver();

		final File file = TestUtil.createFileFromAssets(getInstrumentation().getContext(),TEST_SHORT);
		
		assertTrue(driver.canOpen(file.getAbsolutePath()));

		GDALDataset dataset = driver.open(file.getAbsolutePath());
		
		final int rs = 256;

		final Envelope env = new Envelope(0, rs, 0, rs);
		final Rect rect = new Rect(0, 0, rs, rs);

		final RasterQuery query = new GDALRasterQuery(
				env,
				dataset.getCRS(),
				dataset.getBands(),
				rect,
				dataset.getBands().get(0).datatype(),
				rect);

		final Envelope targetEnv = new Envelope(0, env.getWidth() * 4, 0, env.getHeight() * 4);

		final int origSize = (int) (env.getWidth() * env.getHeight());
		final int targetSize = (int) (targetEnv.getWidth() * targetEnv.getHeight());
		
		Raster raster = dataset.read(query);
		
		final byte[] orig = raster.getData().array().clone();
		
		ByteBufferReader reader = new ByteBufferReader(orig, ByteOrder.nativeOrder());
		short[] shorts = new short[(int)env.getWidth() * (int) env.getHeight()];
		for(int i = 0; i < origSize; i++){
			shorts[i] = reader.readShort();
		}
		
		HashMap<Key,Serializable> resizeParams = new HashMap<>();

		resizeParams.put(Resampler.KEY_SIZE, new Double[]{targetEnv.getWidth() / env.getWidth(), targetEnv.getHeight() / env.getHeight()});
		
		final RasterOp resampler = new OpenCVResampler();
		for(int j = 0; j < ResampleMethod.values().length; j++){

			long now = System.currentTimeMillis();

			final ResampleMethod m = ResampleMethod.values()[j];

			resizeParams.put(Hints.KEY_INTERPOLATION, m);

			resampler.execute(raster, resizeParams,null,null);

			Log.d("RawResamplerTester","short testing with "+m.name()+" took : "+(System.currentTimeMillis() - now));

			final byte[] resampled = raster.getData().array().clone();

			ByteBufferReader resampledReader = new ByteBufferReader(resampled, ByteOrder.nativeOrder());
			short[] resampledShorts = new short[targetSize];
			for(int k = 0; k < targetSize; k++){
				resampledShorts[k] = resampledReader.readShort();
			}

			Log.d(RawResamplerTester.class.getSimpleName(), String.format("orig [last] %s resampled [last] %s", shorts[shorts.length - 1], resampledShorts[resampledShorts.length - 1]));

			assertTrue(compareFloatsWithThreshold(shorts[0], resampledShorts[0], threshold));
			assertTrue(compareFloatsWithThreshold(shorts[shorts.length - 1], resampledShorts[resampledShorts.length - 1],threshold));

			//return to initial state
			raster = dataset.read(query);
		}

		
		dataset.close();
		
		TestUtil.deletefile(file);

	}
	
	public void testIntRawResampling() throws IOException {
		
		final int threshold = 2;
		
		GDALDriver driver = new GDALDriver();

		final File file = TestUtil.createFileFromAssets(getInstrumentation().getContext(),TEST_INT);
		
		assertTrue(driver.canOpen(file.getAbsolutePath()));

		GDALDataset dataset = driver.open(file.getAbsolutePath());
		
		final int rs = 256;

		final Envelope env = new Envelope(0, rs, 0, rs);
		final Rect rect = new Rect(0, 0, rs, rs);

		final RasterQuery query = new GDALRasterQuery(
				env,
				dataset.getCRS(),
				dataset.getBands(),
				rect,
				dataset.getBands().get(0).datatype(),
				rect);

		final Envelope targetEnv = new Envelope(0, env.getWidth() * 4, 0, env.getHeight() * 4);

		final int origSize = (int) (env.getWidth() * env.getHeight());
		final int targetSize = (int) (targetEnv.getWidth() * targetEnv.getHeight());
		
		Raster raster = dataset.read(query);
		
		final byte[] orig = raster.getData().array().clone();
		
		ByteBufferReader reader = new ByteBufferReader(orig, ByteOrder.nativeOrder());
		int[] ints = new int[(int)env.getWidth() * (int) env.getHeight()];
		for(int i = 0; i < origSize; i++){
			ints[i] = reader.readInt();
		}
		
		HashMap<Key,Serializable> resizeParams = new HashMap<>();

		resizeParams.put(Resampler.KEY_SIZE, new Double[]{targetEnv.getWidth() / env.getWidth(), targetEnv.getHeight() / env.getHeight()});
		
		final RasterOp resampler = new OpenCVResampler();
		for(int j = 0; j < ResampleMethod.values().length; j++){

			long now = System.currentTimeMillis();

			final ResampleMethod m = ResampleMethod.values()[j];

			resizeParams.put(Hints.KEY_INTERPOLATION, m);

			resampler.execute(raster, resizeParams,null,null);

			Log.d("RawResamplerTester","int testing with "+m.name()+" took : "+(System.currentTimeMillis() - now));

			final byte[] resampled = raster.getData().array().clone();

			ByteBufferReader resampledReader = new ByteBufferReader(resampled, ByteOrder.nativeOrder());
			int[] resampledInts = new int[targetSize];
			for(int k = 0; k < targetSize; k++){
				resampledInts[k] = resampledReader.readInt();
			}

			Log.d(RawResamplerTester.class.getSimpleName(), String.format("orig [last] %s resampled [last] %s", ints[ints.length - 1], resampledInts[resampledInts.length - 1]));

			assertTrue(compareFloatsWithThreshold(ints[0], resampledInts[0], threshold));
			assertTrue(compareFloatsWithThreshold(ints[ints.length - 1], resampledInts[resampledInts.length - 1],threshold));

			//return to initial state
			raster = dataset.read(query);
		}

		
		dataset.close();
		
		TestUtil.deletefile(file);

	}
	
	public void testLongRawResampling() throws IOException {
		
		final int threshold = 2;
		
		GDALDriver driver = new GDALDriver();

		final File file = TestUtil.createFileFromAssets(getInstrumentation().getContext(),TEST_LONG);
		
		assertTrue(driver.canOpen(file.getAbsolutePath()));

		GDALDataset dataset = driver.open(file.getAbsolutePath());
		
		final int rs = 256;

		final Envelope env = new Envelope(0, rs, 0, rs);
		final Rect rect = new Rect(0, 0, rs, rs);

		final RasterQuery query = new GDALRasterQuery(
				env,
				dataset.getCRS(),
				dataset.getBands(),
				rect,
				dataset.getBands().get(0).datatype(),
				rect);

		final Envelope targetEnv = new Envelope(0, env.getWidth() * 4, 0, env.getHeight() * 4);

		final int origSize = (int) (env.getWidth() * env.getHeight());
		final int targetSize = (int) (targetEnv.getWidth() * targetEnv.getHeight());
		
		Raster raster = dataset.read(query);
		
		final byte[] orig = raster.getData().array().clone();
		
		ByteBufferReader reader = new ByteBufferReader(orig, ByteOrder.nativeOrder());
		long[] longs = new long[(int)env.getWidth() * (int) env.getHeight()];
		for(int i = 0; i < origSize; i++){
			longs[i] = reader.readLong();
		}
		
		HashMap<Key,Serializable> resizeParams = new HashMap<>();

		resizeParams.put(Resampler.KEY_SIZE, new Double[]{targetEnv.getWidth() / env.getWidth(), targetEnv.getHeight() / env.getHeight()});
		
		final RasterOp resampler = new OpenCVResampler();
		for(int j = 0; j < ResampleMethod.values().length; j++){

			long now = System.currentTimeMillis();

			final ResampleMethod m = ResampleMethod.values()[j];

			resizeParams.put(Hints.KEY_INTERPOLATION, m);

			resampler.execute(raster, resizeParams,null,null);

			Log.d("RawResamplerTester","long testing with "+m.name()+" took : "+(System.currentTimeMillis() - now));

			final byte[] resampled = raster.getData().array().clone();

			ByteBufferReader resampledReader = new ByteBufferReader(resampled, ByteOrder.nativeOrder());
			long[] resampledLongs = new long[targetSize];
			for(int k = 0; k < targetSize; k++){
				resampledLongs[k] = resampledReader.readLong();
			}

			Log.d(RawResamplerTester.class.getSimpleName(), String.format("orig [last] %s resampled [last] %s", longs[longs.length - 1], resampledLongs[resampledLongs.length - 1]));

			assertTrue(compareFloatsWithThreshold(longs[0], resampledLongs[0], threshold));
			assertTrue(compareFloatsWithThreshold(longs[longs.length - 1], resampledLongs[resampledLongs.length - 1],threshold));

			//return to initial state
			raster = dataset.read(query);
		}

		
		dataset.close();
		
		TestUtil.deletefile(file);

	}
	
	@SuppressWarnings("unchecked")
	public void testFloatRasterRawResampling() throws IOException {
		
		final int threshold = 2;
		
		GDALDriver driver = new GDALDriver();

		final File file = TestUtil.createFileFromAssets(getInstrumentation().getContext(),TEST_FLOAT);
		
		assertTrue(driver.canOpen(file.getAbsolutePath()));

		GDALDataset dataset = driver.open(file.getAbsolutePath());
		
		final int rs = 256;

		final Envelope env = new Envelope(0, rs, 0, rs);
		final Rect rect = new Rect(0, 0, rs, rs);

		final RasterQuery query = new GDALRasterQuery(
				env,
				dataset.getCRS(),
				dataset.getBands(),
				rect,
				dataset.getBands().get(0).datatype(),
				rect);

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

					Log.d(RawResamplerTester.class.getSimpleName(), String.format("orig [first] %f resampled [first] %f", floats[0], resampledFloats[0]));
					Log.d(RawResamplerTester.class.getSimpleName(), String.format("orig [last] %f resampled [last] %f", floats[floats.length - 1], resampledFloats[resampledFloats.length - 1]));

					assertTrue(compareFloatsWithThreshold(floats[0], resampledFloats[0], threshold));

					assertTrue(compareFloatsWithThreshold(floats[floats.length - 1], resampledFloats[resampledFloats.length - 1],threshold));

					
					//return to initial state
					raster = dataset.read(query);
				}
		}
		
		dataset.close();
		
		TestUtil.deletefile(file);

	}
	
//	public void testDoubleRasterRawResampling() throws IOException {
//		
//		final int threshold = 2;
//		
//		GDALDriver driver = new GDALDriver();
//
//		assertTrue(driver.canOpen(SAMPLE_DOUBLE));
//
//		GDALDataset dataset = driver.open(SAMPLE_DOUBLE);
//		
//		final int rs = 256;
//
//		final Envelope env = new Envelope(0, rs, 0, rs);
//		final Rect rect = new Rect(0, 0, rs, rs);
//
//		final RasterQuery query = new GDALRasterQuery(
//				env,
//				dataset.getCRS(),
//				dataset.getBands(),
//				rect,
//				dataset.getBands().get(0).datatype(),
//				rect);
//
//		final Envelope targetEnv = new Envelope(0, env.getWidth() * 4, 0, env.getHeight() * 4);
//
//		final int origSize = (int) (env.getWidth() * env.getHeight());
//		final int targetSize = (int) (targetEnv.getWidth() * targetEnv.getHeight());
//		
//		Raster raster = dataset.read(query);
//		
//		final byte[] orig = raster.getData().array().clone();
//		
//		ByteBufferReader reader = new ByteBufferReader(orig, ByteOrder.nativeOrder());
//		double[] doubles = new double[(int)env.getWidth() * (int) env.getHeight()];
//		for(int i = 0; i < origSize; i++){
//			doubles[i] = reader.readDouble();
//		}
//		
//		HashMap<Key,Serializable> resizeParams = new HashMap<>();
//
//		resizeParams.put(Resampler.KEY_SIZE, new Double[]{targetEnv.getWidth() / env.getWidth(), targetEnv.getHeight() / env.getHeight()});
//		
//		final RasterOp resampler = new OpenCVResampler();
//
//		for(int j = 0; j < ResampleMethod.values().length; j++){
//
//			long now = System.currentTimeMillis();
//
//			final ResampleMethod m = ResampleMethod.values()[j];
//
//			resizeParams.put(Hints.KEY_INTERPOLATION, m);
//
//			resampler.execute(raster, resizeParams,null,null);
//
//			Log.d("RawResamplerTester","double testing with "+m.name()+" took : "+(System.currentTimeMillis() - now));
//
//			final byte[] resampled = raster.getData().array().clone();
//
//			ByteBufferReader resampledReader = new ByteBufferReader(resampled, ByteOrder.nativeOrder());
//			double[] resampledDoubles = new double[targetSize];
//			for(int k = 0; k < targetSize; k++){
//				resampledDoubles[k] = resampledReader.readDouble();
//			}
//
//			Log.d(RawResamplerTester.class.getSimpleName(), String.format("orig [last] %f resampled [last] %f", doubles[doubles.length - 1], resampledDoubles[resampledDoubles.length - 1]));
//
//			assertTrue(compareDoublesWithThreshold(doubles[0], resampledDoubles[0], threshold));
//			assertTrue(compareDoublesWithThreshold(doubles[doubles.length - 1], resampledDoubles[resampledDoubles.length - 1],threshold));
//
//			//return to initial state
//			raster = dataset.read(query);
//		}
//
//
//		dataset.close();
//
//	}

	
	
	@SuppressWarnings("unchecked")
	public void testByteRasterResampling() {

		GDALDriver driver = new GDALDriver();
		
		final int threshold = 256;
		
		try{
			final File file = TestUtil.createFileFromAssets(getInstrumentation().getContext(),TEST_BYTE);
			
			assertTrue(driver.canOpen(file.getAbsolutePath()));

			GDALDataset dataset = driver.open(file.getAbsolutePath());

			final int rs = 256;
			
			final Envelope env = new Envelope(rs, rs + rs, rs , rs + rs);
			final Rect rect = new Rect(0, 0, rs, rs);

			final RasterQuery query = new GDALRasterQuery(
					env,
					dataset.getCRS(),
					dataset.getBands(),
					rect,
					dataset.getBands().get(0).datatype(),
					rect);

			final Raster raster = dataset.read(query);

			final byte[] orig = raster.getData().array().clone();

			final Envelope targetEnv = new Envelope(0, env.getWidth() * 4, 0, env.getHeight() * 4);
			
			HashMap<Key,Serializable> resizeParams = new HashMap<>();

			resizeParams.put(Resampler.KEY_SIZE, new Double[]{targetEnv.getWidth() / env.getWidth(), targetEnv.getHeight() / env.getHeight()});
			
		    final ArrayList<RasterOp> ops = new ArrayList(){{
		    	add(new OpenCVResampler());
		    	add(new MResampler());
		    }};
			
			for(int i = 0; i < ops.size(); i++){
				final RasterOp resampler = ops.get(i);
					for(int j = 0; j < ResampleMethod.values().length; j++){
						
						long now = System.currentTimeMillis();

						final ResampleMethod m = ResampleMethod.values()[j];
						
						resizeParams.put(Hints.KEY_INTERPOLATION, m);
						
						resampler.execute(raster, resizeParams,null,null);
						
						byte first = raster.getData().array()[0];

						assertTrue(compareBytesWithThreshold(first,orig[0],threshold));

						assertTrue(raster.getData().array().length == targetEnv.getHeight() * targetEnv.getWidth() * raster.getBands().get(0).datatype().size());
						
						Log.d("RawResamplerTester","byte testing "+ resampler.getClass().getSimpleName()+" with "+m.name()+" took : "+(System.currentTimeMillis() - now));
						
						//return to initial state
						
						raster.setDimension(rect);
						raster.setData(ByteBuffer.wrap(orig));
					}
			}

			dataset.close();
			
			TestUtil.deletefile(file);

		}catch(Exception e){
			Log.e(RawResamplerTester.class.getSimpleName(), "Exception raw resampling",e);
		}
	}
	
	public static boolean compareFloatsWithThreshold(float a, float b, float threshold) {

	    return  Math.abs(a - b) <= threshold;
	}
	
	
	public static boolean compareDoublesWithThreshold(double a, double b, float threshold) {

	    return  Math.abs(a - b) <= threshold;
	}
	
	public boolean compareBytesWithThreshold(byte a, byte b, int thrshold){
		
		return Math.abs(a - b) <= thrshold;
	}

}
