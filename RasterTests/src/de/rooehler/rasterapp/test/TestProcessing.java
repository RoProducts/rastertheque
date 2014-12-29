package de.rooehler.rasterapp.test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.util.Log;
import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.core.RasterQuery;
import de.rooehler.rastertheque.core.model.Dimension;
import de.rooehler.rastertheque.core.model.Rectangle;
import de.rooehler.rastertheque.io.gdal.GDALDataset;
import de.rooehler.rastertheque.processing.colormap.MColorMapProcessing;
import de.rooehler.rastertheque.processing.resampling.JAI_Interpolation;
import de.rooehler.rastertheque.processing.resampling.MBilinearInterpolator;

public class TestProcessing extends android.test.AndroidTestCase  {
	
	/**
	 * tests and compares interpolations
	 */
	public void testInterpolation(){		
		
		final GDALDataset dataset = new GDALDataset(TestIO.FILE);
		
		final int width = dataset.getRasterWidth();
		
		final int height = dataset.getRasterHeight();
		
		final int tileSize = Math.min(width, height) / 10;
		
		final Rectangle rect = new Rectangle(0, 0, tileSize, tileSize);
			     
        final RasterQuery query = new RasterQuery(
        		rect,
        		dataset.getBands(),
        		new Dimension(rect.width, rect.height),
        		dataset.getDatatype());
        
        final Raster raster = dataset.read(query);
        
        final MColorMapProcessing cmp = new MColorMapProcessing(TestIO.FILE);
        
        final int[] pixels  = cmp.generatePixelsWithColorMap(
        		raster.getData(),
        		raster.getDimension().getSize(),
        		dataset.getDatatype());
        
        assertNotNull(pixels);
        
        final int resamplingFactor = 3;
        
        final int resampledSize = tileSize * resamplingFactor;
        
        final int[] mResampled = new int[resampledSize * resampledSize];
        final int[] jaiResampled = new int[resampledSize * resampledSize];
        
        long now = System.currentTimeMillis();
        
        new MBilinearInterpolator().resampleBilinear(pixels, tileSize, mResampled, resampledSize);
        
        Log.d(TestProcessing.class.getSimpleName(), "MInterpolation took "+ (System.currentTimeMillis() - now));
        
        now = System.currentTimeMillis();
        
        new JAI_Interpolation().resampleBilinear(pixels, tileSize, jaiResampled, resampledSize);
        
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
	}

}
