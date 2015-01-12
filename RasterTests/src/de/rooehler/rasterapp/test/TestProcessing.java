package de.rooehler.rasterapp.test;

import java.io.IOException;

import android.util.Log;
import de.rooehler.rastertheque.core.Dimension;
import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.core.RasterQuery;
import de.rooehler.rastertheque.core.Rectangle;
import de.rooehler.rastertheque.io.gdal.GDALDataset;
import de.rooehler.rastertheque.io.gdal.GDALDriver;
import de.rooehler.rastertheque.processing.colormap.MRendering;
import de.rooehler.rastertheque.processing.resampling.JAIResampler;
import de.rooehler.rastertheque.processing.resampling.MResampler;

public class TestProcessing extends android.test.AndroidTestCase  {
	
	/**
	 * tests and compares interpolations
	 */
	public void testInterpolation() throws IOException{		
		
		final GDALDriver driver = new GDALDriver();
		
		assertTrue(driver.canOpen(TestIO.FILE));
		
		final GDALDataset dataset = driver.open(TestIO.FILE);
		
		final Dimension dim = dataset.getDimension();
		final int height = dim.getHeight();
		final int width = dim.getWidth();
		
		final int tileSize = Math.min(width, height) / 10;
		
		final Rectangle rect = new Rectangle(0, 0, tileSize, tileSize);
			     
        final RasterQuery query = new RasterQuery(
        		rect,
        		dataset.getCRS(),
        		dataset.getBands(),
        		new Dimension(rect.width, rect.height),
        		dataset.getBands().get(0).datatype());
        
        final Raster raster = dataset.read(query);
        
        final MRendering rend = new MRendering(TestIO.FILE);
        
        final int[] pixels  = rend.generatePixelsWithColorMap(raster);
        
        assertNotNull(pixels);
        
        final int resamplingFactor = 3;
        
        final int resampledSize = tileSize * resamplingFactor;
        
        final int[] mResampled = new int[resampledSize * resampledSize];
        final int[] jaiResampled = new int[resampledSize * resampledSize];
        
        long now = System.currentTimeMillis();
        
        new MResampler().resampleBilinear(pixels, tileSize,tileSize, mResampled, resampledSize, resampledSize);
        
        Log.d(TestProcessing.class.getSimpleName(), "MInterpolation took "+ (System.currentTimeMillis() - now));
        
        now = System.currentTimeMillis();
        
        new JAIResampler().resampleBilinear(pixels, tileSize, tileSize, jaiResampled, resampledSize, resampledSize);
        
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
		
		final Rectangle rect = new Rectangle(0, 0, 256, 256);

        //1. Manually ///////////////
		
		int manual = resampleManually(rect, dataset, readSize, targetSize);

        //2. with gdal ////////////
        int gdal = resampleWithGDAL(rect, dataset, targetSize);
       
        assertEquals(manual, gdal);
              
		
	}
	
	public int resampleWithGDAL(final Rectangle rect, final GDALDataset dataset, final int targetSize){
		
        final RasterQuery gdalResampleQuery = new RasterQuery(
        		rect,
        		dataset.getCRS(),
        		dataset.getBands(),
        		new Dimension(targetSize,targetSize),
        		dataset.getBands().get(0).datatype());
        
        final long gdalNow = System.currentTimeMillis();
        
        final Raster raster = dataset.read(gdalResampleQuery);    
		final MRendering rend = new MRendering(TestIO.FILE);
		
        final int[] gdalResampledPixels  = rend.generatePixelsWithColorMap(raster);
        assertNotNull(gdalResampledPixels);
        Log.d(TestProcessing.class.getSimpleName(), "GDAL resampling took "+ (System.currentTimeMillis() - gdalNow)+" ms");

        return gdalResampledPixels.length;
	}
	public int resampleManually(final Rectangle rect, final GDALDataset dataset,final int readSize, final int targetSize){
		
        final RasterQuery manualResamplingQuery = new RasterQuery(
        		rect,
        		dataset.getCRS(),
        		dataset.getBands(),
        		new Dimension(readSize,readSize),
        		dataset.getBands().get(0).datatype());
        
        final long manualNow = System.currentTimeMillis();
        
        final Raster manualRaster = dataset.read(manualResamplingQuery);
		
        Log.d(TestProcessing.class.getSimpleName(), "gdal read took "+ (System.currentTimeMillis() - manualNow)+" ms");
        
    	final MRendering rend = new MRendering(TestIO.FILE);
        
        final int[] manualResampledSourcePixels  = rend.generatePixelsWithColorMap(manualRaster);
        
        final int[] manualResampledTargetPixels = new int[targetSize * targetSize];
        
        new MResampler().resampleBilinear(
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

}
