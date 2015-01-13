package de.rooehler.rasterapp.test;

import java.io.IOException;
import java.util.ArrayList;

import android.os.Environment;
import android.util.Log;
import de.rooehler.rastertheque.core.Dimension;
import de.rooehler.rastertheque.core.Driver;
import de.rooehler.rastertheque.core.Drivers;
import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.core.RasterQuery;
import de.rooehler.rastertheque.core.Rectangle;
import de.rooehler.rastertheque.io.gdal.GDALDataset;
import de.rooehler.rastertheque.io.gdal.GDALDriver;
import de.rooehler.rastertheque.processing.Renderer;
import de.rooehler.rastertheque.processing.rendering.MRenderer;

public class TestIO extends android.test.AndroidTestCase {

	
	public final static String FILE = Environment.getExternalStorageDirectory().getAbsolutePath()+"/rastertheque/GRAY_50M_SR_OB.tif";
	
	/**
	 * tests opening of the file
	 */
	public void testIO() throws IOException{
				
		GDALDriver driver = new GDALDriver();
		
		assertTrue(driver.canOpen(FILE));
		
		GDALDataset dataset = driver.open(FILE);
				
		assertNotNull(dataset.getCenterPoint());
		
		assertNotNull(dataset.getBoundingBox());
		
		assertNotNull(dataset.getCRS());
		
		final Dimension dim = dataset.getDimension();
		
		assertTrue(dim.getWidth() > 0);
		
		assertTrue(dim.getHeight() > 0);
		
		dataset.close();
		
	}
	/*
	 * tests reading a region of the file
	 */
	public void testRead() throws IOException{
		
		GDALDriver driver = new GDALDriver();
		
		assertTrue(driver.canOpen(FILE));
		
		GDALDataset dataset = driver.open(FILE);
		
		final Dimension dim = dataset.getDimension();
		final int height = dim.getHeight();
		final int width = dim.getWidth();
		
		final Rectangle rect = new Rectangle(0, 0, width / 10, height / 10);
		     
        final RasterQuery query = new RasterQuery(
        		rect,
        		dataset.getCRS(),
        		dataset.getBands(),
        		new Dimension(rect.width, rect.height),
        		dataset.getBands().get(0).datatype());
        
        final Raster raster = dataset.read(query);
        
        final Renderer renderer = new MRenderer(FILE);
        
        final int[] pixels  = renderer.grayscale(raster);
        
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
        
        dataset.close();
        
	}
	
	@SuppressWarnings("serial")
	public void testGDALDrivers(){
		
		ArrayList<String> driverLocations = new ArrayList<String>(){{
			add("org/rastertheque/io/raster/gdal/");
		}};
		
		ArrayList<Driver<?>> gdalDrivers = Drivers.getDrivers(driverLocations);
				
		assertNotNull(gdalDrivers);
		
		assertTrue(gdalDrivers.size() > 0);
		
		Log.d(TestIO.class.getSimpleName(),"gdal drivers found : " + gdalDrivers.size());
		
		ArrayList<Class<?>> classes = new ArrayList<Class<?>>(){{
			add(GDALDriver.class);
			add(TestPluggedDriver.class);
		}};
		

		for(int i = 0; i < gdalDrivers.size(); i++){

			final Driver<?> driver = gdalDrivers.get(i);

			Log.d(TestIO.class.getSimpleName(),"class : " + driver.getName().toString());

			assertTrue(classes.contains(driver.getClass()));

		}
	}
	
	@SuppressWarnings("serial")
	public void testMBTilesDrivers(){
		

		ArrayList<String> driverLocations = new ArrayList<String>(){{
			add("org/rastertheque/io/raster/mbtiles/");
		}};
		
		ArrayList<Driver<?>> drivers = Drivers.getDrivers(driverLocations);
				
		assertNotNull(drivers);
		
		assertTrue(drivers.size() > 0);
		
		Log.d(TestIO.class.getSimpleName(),"MBTiles drivers found : " + drivers.size());
		
	}
}
