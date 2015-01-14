package de.rooehler.rasterapp.test;

import java.io.IOException;

import android.util.Log;

import com.vividsolutions.jts.geom.Envelope;

import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.core.RasterQuery;
import de.rooehler.rastertheque.io.gdal.GDALDataset;
import de.rooehler.rastertheque.io.gdal.GDALDriver;
import de.rooehler.rastertheque.processing.Resampler;
import de.rooehler.rastertheque.processing.Resampler.ResampleMethod;
import de.rooehler.rastertheque.processing.rendering.MRenderer;
import de.rooehler.rastertheque.processing.resampling.JAIResampler;
import de.rooehler.rastertheque.processing.resampling.MResampler;

public class TestProcessing extends android.test.AndroidTestCase  {
	
	/**
	 * tests and compares interpolations
	 */
	public void testBilinearInterpolation() throws IOException{		
		
		final GDALDriver driver = new GDALDriver();
		
		assertTrue(driver.canOpen(TestIO.FILE));
		
		final GDALDataset dataset = driver.open(TestIO.FILE);
		
		final Envelope  dim = dataset.getDimension();
		final int height = (int) dim.getHeight();
		final int width =  (int)dim.getWidth();
		
		final int tileSize = Math.min(width, height) / 10;
		
		final Envelope env = new Envelope(0, tileSize, 0, tileSize);
			     
        final RasterQuery query = new RasterQuery(
        		env,
        		dataset.getCRS(),
        		dataset.getBands(),
        		new Envelope(0, env.getWidth(), 0, env.getHeight()),
        		dataset.getBands().get(0).datatype());
        
        final Raster raster = dataset.read(query);
        
        final MRenderer renderer = new MRenderer(TestIO.FILE);
        
        final int[] pixels  = renderer.colormap(raster);
        
        assertNotNull(pixels);
        
        final int resamplingFactor = 3;
        
        final int resampledSize = tileSize * resamplingFactor;
        
        final int[] mResampled = new int[resampledSize * resampledSize];
        final int[] jaiResampled = new int[resampledSize * resampledSize];
        
        long now = System.currentTimeMillis();
        
        new MResampler(ResampleMethod.BILINEAR).resample(pixels, tileSize,tileSize, mResampled, resampledSize, resampledSize);
        
        Log.d(TestProcessing.class.getSimpleName(), "MInterpolation took "+ (System.currentTimeMillis() - now));
        
        now = System.currentTimeMillis();
        
        new JAIResampler(ResampleMethod.BILINEAR).resample(pixels, tileSize, tileSize, jaiResampled, resampledSize, resampledSize);
        
        Log.d(TestProcessing.class.getSimpleName(), "JAI took "+ (System.currentTimeMillis() - now));
        
        assertTrue(mResampled.length == pixels.length * resamplingFactor * resamplingFactor);
        
        //check a pixel
        final int mPixel = mResampled[resampledSize / 2];
        final int jaiPixel = jaiResampled[resampledSize / 2];
        
        final int mRed = (mPixel >> 16) & 0xff;
        final int mGreen = (mPixel >> 8) & 0xff;
        final int mBlue = (mPixel) & 0xff;
        
        final int jaiRed = (jaiPixel >> 16) & 0xff;
        final int jaiGreen = (jaiPixel >> 8) & 0xff;
        final int jaiBlue = (jaiPixel) & 0xff;
        
        //valid values
        assertTrue(mRed >= 0 && mRed <= 255);
        assertTrue(mGreen >= 0 && mGreen <= 255);
        assertTrue(mBlue >= 0 && mBlue <= 255);
        
        assertTrue(mRed == jaiRed);
        assertTrue(mGreen == jaiGreen);
        assertTrue(mBlue == jaiBlue);
        
        dataset.close();
	}
	
	/**
	 * tests and compares resampling methods
	 */
	public void testResampling() throws IOException {
				
		final GDALDriver driver = new GDALDriver();
		
		assertTrue(driver.canOpen(TestIO.FILE));
		
		final GDALDataset dataset = driver.open(TestIO.FILE);

		final int readSize = 256;
		final int targetSize = 756;
		
		final Envelope env = new Envelope(0, 256, 0, 256);

        //1. Manually ///////////////
		
		int manual = resampleManually(env, dataset, readSize, targetSize);

        //2. with gdal ////////////
        int gdal = resampleWithGDAL(env, dataset, targetSize);
       
        assertEquals(manual, gdal);
              
		
	}
	
	public int resampleWithGDAL(final Envelope env, final GDALDataset dataset, final int targetSize){
		
        final RasterQuery gdalResampleQuery = new RasterQuery(
        		env,
        		dataset.getCRS(),
        		dataset.getBands(),
        		new Envelope(0, targetSize, 0 ,targetSize),
        		dataset.getBands().get(0).datatype());
        
        final long gdalNow = System.currentTimeMillis();
        
        final Raster raster = dataset.read(gdalResampleQuery);    
		final MRenderer renderer = new MRenderer(TestIO.FILE);
		
        final int[] gdalResampledPixels  = renderer.colormap(raster);
        assertNotNull(gdalResampledPixels);
        Log.d(TestProcessing.class.getSimpleName(), "GDAL resampling took "+ (System.currentTimeMillis() - gdalNow)+" ms");

        return gdalResampledPixels.length;
	}
	
	public int resampleManually(final Envelope env, final GDALDataset dataset,final int readSize, final int targetSize){
		
        final RasterQuery manualResamplingQuery = new RasterQuery(
        		env,
        		dataset.getCRS(),
        		dataset.getBands(),
        		new Envelope(0 , readSize, 0, readSize),
        		dataset.getBands().get(0).datatype());
        
        final long manualNow = System.currentTimeMillis();
        
        final Raster manualRaster = dataset.read(manualResamplingQuery);
		
        Log.d(TestProcessing.class.getSimpleName(), "gdal read took "+ (System.currentTimeMillis() - manualNow)+" ms");
        
    	final MRenderer rend = new MRenderer(TestIO.FILE);
        
        final int[] manualResampledSourcePixels  = rend.colormap(manualRaster);
        
        final int[] manualResampledTargetPixels = new int[targetSize * targetSize];
        
        new MResampler(ResampleMethod.BILINEAR).resample(
        		manualResampledSourcePixels,
        		readSize,
        		readSize,
        		manualResampledTargetPixels,
        		targetSize,
        		targetSize);
        
        Log.d(TestProcessing.class.getSimpleName(), "manual resampling took "+ (System.currentTimeMillis() - manualNow)+" ms");
        
        
        assertNotNull(manualResampledTargetPixels);
        
        return manualResampledTargetPixels.length;
	}

	public void testBicubic(){
		
		int white = 0xffffffff;
		int black = 0xff000000;
		
		int[] pic = new int[]{white,black,white,black};
		
		int[] resampled = new int[pic.length * 4];
		
		Resampler resampler = new MResampler(ResampleMethod.BICUBIC);
		
		resampler.resample(pic, 2, 2, resampled, 4, 4);
		
		assertTrue(resampled[0] == white);
		
	}
}
