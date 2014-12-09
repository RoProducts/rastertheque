package de.rooehler.rasterapp.test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.util.Log;
import de.rooehler.rastertheque.core.Rectangle;
import de.rooehler.rastertheque.io.gdal.GDALRasterIO;
import de.rooehler.rastertheque.processing.colormap.MColorMapProcessing;
import de.rooehler.rastertheque.processing.resampling.JAI_Interpolation;
import de.rooehler.rastertheque.processing.resampling.MBilinearInterpolator;

public class TestProcessing extends android.test.AndroidTestCase  {
	
	public void testInterpolation(){
		
		
		GDALRasterIO io = new GDALRasterIO(IOTest.FILE);
		
		int width = io.getRasterWidth();
		
		int height = io.getRasterHeight();
		
		final int tileSize = Math.min(width, height) / 10;
		
		Rectangle rect = new Rectangle(0, 0, tileSize, tileSize);
		
		final int bufferSize = tileSize * tileSize * io.getDatatype().size();
		
        ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
        
        buffer.order(ByteOrder.nativeOrder()); 
        
        io.read(rect, buffer);
        
        MColorMapProcessing cmp = new MColorMapProcessing(IOTest.FILE);
        
        int[] pixels  = cmp.generatePixelsWithColorMap(buffer, bufferSize, io.getDatatype());
        
        assertNotNull(pixels);
        
        final int resamplingFactor = 3;
        
        final int resampledSize = tileSize * resamplingFactor;
        
        int[] mResampled = new int[resampledSize * resampledSize];
        int[] jaiResampled = new int[resampledSize * resampledSize];
        
        long now = System.currentTimeMillis();
        
        new MBilinearInterpolator().resampleBilinear(pixels, tileSize, mResampled, resampledSize);
        
        Log.d(TestProcessing.class.getSimpleName(), "MInterpolation took "+ (System.currentTimeMillis() - now));
        
        now = System.currentTimeMillis();
        
        new JAI_Interpolation().resampleBilinear(pixels, tileSize, jaiResampled, resampledSize);
        
        Log.d(TestProcessing.class.getSimpleName(), "JAI took "+ (System.currentTimeMillis() - now));
        
        assertTrue(mResampled.length == pixels.length * resamplingFactor * resamplingFactor);
        
        //check a pixel
        int mPixel = mResampled[resampledSize / 2];
        int jaiPixel = jaiResampled[resampledSize / 2];
        
        int mRed = (mPixel >> 16) & 0xff;
        int mGreen = (mPixel >> 8) & 0xff;
        int mBlue = (mPixel) & 0xff;
        
        int jaiRed = (jaiPixel >> 16) & 0xff;
        int jaiGreen = (jaiPixel >> 8) & 0xff;
        int jaiBlue = (jaiPixel) & 0xff;
        
        //valid values
        assertTrue(mRed >= 0 && mRed <= 255);
        assertTrue(mGreen >= 0 && mGreen <= 255);
        assertTrue(mBlue >= 0 && mBlue <= 255);
        
        assertTrue(mRed == jaiRed);
        assertTrue(mGreen == jaiGreen);
        assertTrue(mBlue == jaiBlue);
	}

}
