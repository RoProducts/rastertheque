package de.rooehler.rasterapp.test;


import java.io.IOException;
import java.nio.ByteOrder;

import android.util.Log;

import com.vividsolutions.jts.geom.Envelope;

import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.core.RasterQuery;
import de.rooehler.rastertheque.core.util.ByteBufferReader;
import de.rooehler.rastertheque.io.gdal.GDALDataset;
import de.rooehler.rastertheque.io.gdal.GDALDriver;
import de.rooehler.rastertheque.processing.RawResampler;
import de.rooehler.rastertheque.processing.Resampler.ResampleMethod;
import de.rooehler.rastertheque.processing.resampling.JAIRawResampler;
import de.rooehler.rastertheque.processing.resampling.MRawResampler;
import de.rooehler.rastertheque.processing.resampling.OpenCVRawResampler;

public class RawResamplerTester extends android.test.ActivityTestCase {

	public void testFloatRasterRawResampling() throws IOException {

		GDALDriver driver = new GDALDriver();

		assertTrue(driver.canOpen(TestIO.DEM_FLOAT));

		GDALDataset dataset = driver.open(TestIO.DEM_FLOAT);

		final Envelope env = new Envelope(0, 256, 0, 256);

		final RasterQuery query = new RasterQuery(
				env,
				dataset.getCRS(),
				dataset.getBands(),
				env,
				dataset.getBands().get(0).datatype());

		final Envelope targetEnv = new Envelope(0, env.getWidth() * 2, 0, env.getHeight() * 2);

		final int origSize = (int) (env.getWidth() * env.getHeight());
		final int targetSize = (int) (targetEnv.getWidth() * targetEnv.getHeight());

		////////////////OpenCV Raw resampler /////////////////
		final Raster raster = dataset.read(query);

		final byte[] orig = raster.getData().array().clone();

		ByteBufferReader reader = new ByteBufferReader(orig, ByteOrder.nativeOrder());
		float[] floats = new float[(int)env.getWidth() * (int) env.getHeight()];
		for(int i = 0; i < origSize; i++){
			floats[i] = reader.readFloat();
		}

		long now = System.currentTimeMillis();

		raster.setDimension(targetEnv);

		RawResampler openCVResampler = new OpenCVRawResampler();

		openCVResampler.resample(raster, ResampleMethod.BILINEAR);

		final byte[] resampled = raster.getData().array().clone();

		ByteBufferReader resampledReader = new ByteBufferReader(resampled, ByteOrder.nativeOrder());
		float[] resampledFloats = new float[targetSize];
		for(int i = 0; i < targetSize; i++){
			resampledFloats[i] = resampledReader.readFloat();
		}

		Log.d(RawResamplerTester.class.getSimpleName(), String.format("orig [last] %f resampled [last] %f", floats[floats.length - 1], resampledFloats[resampledFloats.length - 1]));

		assertEquals(floats[0], resampledFloats[0]);

		assertEquals(floats[floats.length - 1], resampledFloats[resampledFloats.length - 1]);


		Log.d(RawResamplerTester.class.getSimpleName(), "OpenCV took : "+(System.currentTimeMillis() - now));


		////////////////MRaw resampler /////////////////

		final Raster raster2 = dataset.read(query);

		long now2 = System.currentTimeMillis();

		raster2.setDimension(targetEnv);

		RawResampler mResampler = new MRawResampler();

		mResampler.resample(raster2, ResampleMethod.BILINEAR);

		final byte[] resampled2 = raster2.getData().array().clone();

		ByteBufferReader resampledReader2 = new ByteBufferReader(resampled2, ByteOrder.nativeOrder());
		float[] resampledFloats2 = new float[targetSize];
		for(int i = 0; i < targetSize; i++){
			resampledFloats2[i] = resampledReader2.readFloat();
		}
		
		Log.d(RawResamplerTester.class.getSimpleName(), String.format("orig [last] %f resampled [last] %f", floats[floats.length - 1], resampledFloats2[resampledFloats2.length - 1]));

		assertEquals(floats[0], resampledFloats2[0]);

		assertTrue(similarUnscaled(floats[floats.length - 1], resampledFloats2[resampledFloats2.length - 1],1));

		Log.d(RawResamplerTester.class.getSimpleName(), "MResampler (float) took : "+(System.currentTimeMillis() - now2));

		////////////////JAI Raw resampler /////////////////

		final Raster raster3 = dataset.read(query);

		long now3 = System.currentTimeMillis();

		raster3.setDimension(targetEnv);

		RawResampler jaiResampler = new JAIRawResampler();

		jaiResampler.resample(raster3, ResampleMethod.BILINEAR);

		final byte[] resampled3 = raster3.getData().array().clone();

		ByteBufferReader resampledReader3 = new ByteBufferReader(resampled3, ByteOrder.nativeOrder());
		float[] resampledFloats3 = new float[targetSize];
		for(int i = 0; i < targetSize; i++){
			resampledFloats3[i] = resampledReader3.readFloat();
		}

		Log.d(RawResamplerTester.class.getSimpleName(), String.format("orig [last] %f resampled [last] %f", floats[floats.length - 1], resampledFloats3[resampledFloats3.length - 1]));

		assertEquals(floats[0], resampledFloats3[0]);

		assertTrue(similarUnscaled(floats[floats.length - 1], resampledFloats3[resampledFloats3.length - 1],1));

		Log.d(RawResamplerTester.class.getSimpleName(), "JAIResampler (float) took : "+(System.currentTimeMillis() - now3));



		dataset.close();

	}
	
	public static boolean similarUnscaled(float a, float b, float threshold) {

	    return  Math.abs(a - b) <= threshold;
	}

	public void testByteRasterResampling() {

		GDALDriver driver = new GDALDriver();

		assertTrue(driver.canOpen(TestIO.GRAY_50M_BYTE));
		try{

			GDALDataset dataset = driver.open(TestIO.GRAY_50M_BYTE);

			final Envelope env = new Envelope(0, 512, 0, 512);

			final RasterQuery query = new RasterQuery(
					env,
					dataset.getCRS(),
					dataset.getBands(),
					env,
					dataset.getBands().get(0).datatype());

			final Raster raster = dataset.read(query);

			final byte[] orig = raster.getData().array().clone();

			long now = System.currentTimeMillis();

			final Envelope targetEnv = new Envelope(0, env.getWidth() * 4, 0, env.getHeight() * 4);

			final int origSize = (int) (env.getWidth() * env.getHeight());
			final int targetSize = (int) (targetEnv.getWidth() * targetEnv.getHeight());

			raster.setDimension(targetEnv);
			
			////////////////OpenCV Raw resampler /////////////////
			
			RawResampler rawResampler = new OpenCVRawResampler();

			rawResampler.resample(raster, ResampleMethod.BILINEAR);

			byte first = raster.getData().array()[0];
			byte last = raster.getData().array()[targetSize - 1];

			assertTrue(first == orig[0]);

			assertTrue(last == orig[origSize - 1]);

			assertTrue(raster.getData().array().length == targetEnv.getHeight() * targetEnv.getWidth() * raster.getBands().get(0).datatype().size());

			Log.d(RawResamplerTester.class.getSimpleName(), "opencv took : "+(System.currentTimeMillis() - now));

			
			////////////////MRaw resampler /////////////////
			
			final Raster raster2 = dataset.read(query);
			
			long now2 = System.currentTimeMillis();
			
			raster2.setDimension(targetEnv);
			
			RawResampler mResampler = new MRawResampler();

			mResampler.resample(raster2, ResampleMethod.BILINEAR);

			byte first2 = raster2.getData().array()[0];
			byte last2 = raster2.getData().array()[targetSize - 1];

			assertTrue(first2 == orig[0]);

			assertTrue(last2 == orig[origSize - 1]);

			assertTrue(raster2.getData().array().length == targetEnv.getHeight() * targetEnv.getWidth() * raster.getBands().get(0).datatype().size());

			Log.d(RawResamplerTester.class.getSimpleName(), "m took : "+(System.currentTimeMillis() - now2));

			
			////////////////JAI Raw resampler /////////////////
			
			final Raster raster3 = dataset.read(query);
			
			long now3 = System.currentTimeMillis();
			
			raster3.setDimension(targetEnv);
			
			RawResampler jaiResampler = new JAIRawResampler();
			
			jaiResampler.resample(raster3, ResampleMethod.BILINEAR);
			
			byte first3 = raster3.getData().array()[0];
			byte last3 = raster3.getData().array()[targetSize - 1];

			assertTrue(first3 == orig[0]);

			assertTrue(last3 == orig[origSize - 1]);

			assertTrue(raster3.getData().array().length == targetEnv.getHeight() * targetEnv.getWidth() * raster.getBands().get(0).datatype().size());

			Log.d(RawResamplerTester.class.getSimpleName(), "jai took : "+(System.currentTimeMillis() - now3));


			dataset.close();

		}catch(Exception e){
			Log.e(RawResamplerTester.class.getSimpleName(), "Exception raw resampling",e);
		}
	}

}