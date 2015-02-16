package de.rooehler.rasterapp.test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import android.os.Environment;
import android.util.Log;

import com.vividsolutions.jts.geom.Envelope;

import de.rooehler.rasterapp.test.testImpl.TestDriverImpl;
import de.rooehler.rastertheque.core.Driver;
import de.rooehler.rastertheque.core.Drivers;
import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.core.RasterQuery;
import de.rooehler.rastertheque.io.gdal.GDALDataset;
import de.rooehler.rastertheque.io.gdal.GDALDriver;
import de.rooehler.rastertheque.io.mbtiles.MBTilesDriver;
import de.rooehler.rastertheque.processing.RasterOp;
import de.rooehler.rastertheque.processing.rendering.MAmplitudeRescaler;

public class TestIO extends android.test.AndroidTestCase {

	
	public final static String GRAY_50M_BYTE = Environment.getExternalStorageDirectory().getAbsolutePath()+"/rastertheque/GRAY_50M_SR_OB.tif";
	public final static String DEM_FLOAT = Environment.getExternalStorageDirectory().getAbsolutePath()+"/rastertheque/dem.tif";
	public final static String RGB_BANDS_BYTE = Environment.getExternalStorageDirectory().getAbsolutePath()+"/rastertheque/land_shallow_topo_21600.tif";
	
	/**
	 * tests opening of the file
	 */
	public void testIO() throws IOException{
				
		GDALDriver driver = new GDALDriver();
		
		assertTrue(driver.canOpen(GRAY_50M_BYTE));
		
		GDALDataset dataset = driver.open(GRAY_50M_BYTE);
		
		assertNotNull(dataset.getBoundingBox());
		
		assertNotNull(dataset.getCRS());
		
		final Envelope dim = dataset.getDimension();
		
		assertTrue(dim.getWidth() > 0);
		
		assertTrue(dim.getHeight() > 0);
		
		dataset.close();
		
	}
	/**
	 * tests reading a region of the file
	 */
	public void testRead() throws IOException{
		
		GDALDriver driver = new GDALDriver();
		
		assertTrue(driver.canOpen(GRAY_50M_BYTE));
		
		GDALDataset dataset = driver.open(GRAY_50M_BYTE);
		
		final Envelope dim = dataset.getDimension();
		final int height = (int) dim.getHeight();
		final int width =  (int) dim.getWidth();
		
		final Envelope env = new Envelope(0, width / 10, 0, height / 10);
		     
        final RasterQuery query = new RasterQuery(
        		env,
        		dataset.getCRS(),
        		dataset.getBands(),
        		new Envelope(0, env.getWidth(), 0, env.getHeight()),
        		dataset.getBands().get(0).datatype());
        
        final Raster raster = dataset.read(query);
        
        final RasterOp renderer = new MAmplitudeRescaler();
        
        renderer.execute(raster, null, null, null);
        
        final int[] pixels  = new int[(int) (env.getWidth() * env.getHeight())];
	    
        raster.getData().asIntBuffer().get(pixels);
        
        assertNotNull(pixels);
        
        //check a pixel
        final int pixel = pixels[((int)raster.getDimension().getWidth() * (int) raster.getDimension().getHeight()) / 2];
        
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
	/**
	 * tests the identification of Driver implementations 
	 * there exists currently two implementation (GDALDriver, MBTilesDriver)
	 * and one stub (TestPluggedDriver)
	 * which all should be found
	 */
	@SuppressWarnings({ "serial", "unchecked" })
	public void testDrivers(){
		
		Method method = null;
		try {
			method = Drivers.class.getDeclaredMethod("getDrivers", String.class);
		} catch (Exception e) {
			Log.e(TestIO.class.getSimpleName(),"exception getting getDrivers() ");
		}
		method.setAccessible(true);
		ArrayList<Driver> drivers = null;
		try {
			drivers = (ArrayList<Driver>) method.invoke(Drivers.class.newInstance(), "org/rastertheque/io/driver/");
		} catch (Exception e) {
			Log.e(TestIO.class.getSimpleName(),"exception invoking : " + method.getName());
		}
		
				
		assertNotNull(drivers);
		
		assertTrue(drivers.size() == 3);
		
		Log.d(TestIO.class.getSimpleName(),"gdal drivers found : " + drivers.size());
		
		ArrayList<Class<?>> classes = new ArrayList<Class<?>>(){{
			add(GDALDriver.class);
			add(MBTilesDriver.class);
			add(TestDriverImpl.class);
		}};
		

		for(int i = 0; i < drivers.size(); i++){

			final Driver driver = drivers.get(i);

			Log.d(TestIO.class.getSimpleName(),"class : " + driver.getName().toString());

			assertTrue(classes.contains(driver.getClass()));

		}
	}
	
}
