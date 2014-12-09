package de.rooehler.rasterapp.test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.os.Environment;
import de.rooehler.rastertheque.core.Rectangle;
import de.rooehler.rastertheque.io.gdal.GDALRasterIO;
import de.rooehler.rastertheque.processing.colormap.MColorMapProcessing;

public class IOTest extends android.test.AndroidTestCase {

	
	public final static String FILE = Environment.getExternalStorageDirectory().getAbsolutePath()+"/rastertheque/GRAY_50M_SR_OB.tif";
	
	
	public void testIO(){
		
		
		GDALRasterIO io = new GDALRasterIO();
		
		assertTrue(io.open(FILE));
		
		io.setup(FILE);
		
		assertNotNull(io.getCenterPoint());
		
		assertNotNull(io.getEnvelope());
		
		assertNotNull(io.getCRS());
		
		assertTrue(io.getRasterWidth() > 0);
		
		assertTrue(io.getRasterHeight() > 0);
		
		io.close();
		
	}
	
	public void testRead(){
		
		GDALRasterIO io = new GDALRasterIO(FILE);
		
		int width = io.getRasterWidth();
		
		int height = io.getRasterHeight();
		
		Rectangle rect = new Rectangle(0, 0, width / 10, height / 10);
		
		final int bufferSize = width / 10 * height / 10 * io.getDatatype().size();
		
        ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
        
        buffer.order(ByteOrder.nativeOrder()); 
        
        io.read(rect, buffer);
        
        MColorMapProcessing cmp = new MColorMapProcessing(FILE);
        
        int[] pixels  = cmp.generateGrayScalePixelsCalculatingMinMax(buffer, bufferSize, io.getDatatype());
        
        assertNotNull(pixels);
        
        //check a pixel
        int pixel = pixels[bufferSize / 2];
        
        int red = (pixel >> 16) & 0xff;
        int green = (pixel >> 8) & 0xff;
        int blue = (pixel) & 0xff;
        
        //valid values
        assertTrue(red >= 0 && red <= 255);
        assertTrue(green >= 0 && green <= 255);
        assertTrue(blue >= 0 && blue <= 255);
        
        //this should be a grey pixel, hence
        assertTrue(red == green);
        assertTrue(green == blue);
        
	}
}
