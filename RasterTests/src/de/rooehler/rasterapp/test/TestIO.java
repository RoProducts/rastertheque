package de.rooehler.rasterapp.test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.os.Environment;
import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.core.RasterQuery;
import de.rooehler.rastertheque.core.model.Dimension;
import de.rooehler.rastertheque.core.model.Rectangle;
import de.rooehler.rastertheque.io.gdal.GDALDataset;
import de.rooehler.rastertheque.processing.colormap.MColorMapProcessing;

public class TestIO extends android.test.AndroidTestCase {

	
	public final static String FILE = Environment.getExternalStorageDirectory().getAbsolutePath()+"/rastertheque/GRAY_50M_SR_OB.tif";
	
	/**
	 * tests opening of the file
	 */
	public void testIO(){
		
		
		GDALDataset dataset = new GDALDataset();
		
		assertTrue(dataset.open(FILE));
		
		dataset.setup(FILE);
		
		assertNotNull(dataset.getCenterPoint());
		
		assertNotNull(dataset.getBoundingBox());
		
		assertNotNull(dataset.getCRS());
		
		assertTrue(dataset.getRasterWidth() > 0);
		
		assertTrue(dataset.getRasterHeight() > 0);
		
		dataset.close();
		
	}
	/*
	 * tests reading a region of the file
	 */
	public void testRead(){
		
		GDALDataset dataset = new GDALDataset(FILE);
		
		int width = dataset.getRasterWidth();
		
		int height = dataset.getRasterHeight();
		
		final Rectangle rect = new Rectangle(0, 0, width / 10, height / 10);
		     
        final RasterQuery query = new RasterQuery(
        		rect,
        		dataset.getBands(),
        		new Dimension(rect.width, rect.height),
        		dataset.getDatatype());
        
        final Raster raster = dataset.read(query);
        
        final MColorMapProcessing cmp = new MColorMapProcessing(FILE);
        
        final int[] pixels  = cmp.generateGrayScalePixelsCalculatingMinMax(
        		raster.getData(),
        		raster.getDimension().getSize(),
        		dataset.getDatatype());
        
        assertNotNull(pixels);
        
        //check a pixel
        final int pixel = pixels[raster.getDimension().getSize() / 2];
        
        final int red = (pixel >> 16) & 0xff;
        final int green = (pixel >> 8) & 0xff;
        final int blue = (pixel) & 0xff;
        
        //valid values
        assertTrue(red >= 0 && red <= 255);
        assertTrue(green >= 0 && green <= 255);
        assertTrue(blue >= 0 && blue <= 255);
        
        //this should be a grey pixel, hence
        assertTrue(red == green);
        assertTrue(green == blue);
        
	}
}
