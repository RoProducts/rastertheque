package de.rooehler.rasterapp.test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import de.rooehler.rastertheque.core.Rectangle;
import de.rooehler.rastertheque.io.gdal.GDALRasterIO;
import de.rooehler.rastertheque.processing.colormap.MColorMapProcessing;
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
        
        final int resamplingFactor = 10;
        
        final int resampledSize = tileSize * resamplingFactor;
        
        int[] resampled = new int[resampledSize * resampledSize];
        
        MBilinearInterpolator.resampleBilinear(pixels, tileSize, resampled, resampledSize);
        
        assertTrue(resampled.length == pixels.length * resamplingFactor * resamplingFactor);
        
        //check a pixel
        int pixel = resampled[resampledSize / 2];
        
        int red = (pixel >> 16) & 0xff;
        int green = (pixel >> 8) & 0xff;
        int blue = (pixel) & 0xff;
        
        //valid values
        assertTrue(red >= 0 && red <= 255);
        assertTrue(green >= 0 && green <= 255);
        assertTrue(blue >= 0 && blue <= 255);
	}

}
