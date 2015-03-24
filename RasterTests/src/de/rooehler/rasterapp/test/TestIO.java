package de.rooehler.rasterapp.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;

import android.content.res.AssetManager;
import android.graphics.Rect;
import android.os.Environment;
import android.util.Log;

import com.vividsolutions.jts.geom.Envelope;

import de.rooehler.rasterapp.test.testImpl.TestDriverImpl;
import de.rooehler.rastertheque.core.Dataset;
import de.rooehler.rastertheque.core.Driver;
import de.rooehler.rastertheque.core.Drivers;
import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.core.RasterDataset;
import de.rooehler.rastertheque.core.RasterQuery;
import de.rooehler.rastertheque.io.gdal.GDALDataset;
import de.rooehler.rastertheque.io.gdal.GDALDriver;
import de.rooehler.rastertheque.io.gdal.GDALRasterQuery;
import de.rooehler.rastertheque.io.mbtiles.MBTilesDriver;
import de.rooehler.rastertheque.processing.RasterOp;
import de.rooehler.rastertheque.processing.rendering.MAmplitudeRescaler;
/**
 * tests of the I/O part of the library containing:
 * 
 * test if a file can be opened
 * test if a file can be read
 * 
 * test the driver implementations retrieval functionality
 * 
 * @author Robert Oehler
 *
 */
public class TestIO extends android.test.ActivityTestCase {

	
	public final static String TEST_SMALL_BYTE = "test_000261_0001sx8.tif";
	public final static String TEST_GRAY_BYTE = "gray_sub.tif";
	public final static String TEST_RGB_BANDS_BYTE = "shallow_topo.tif";
	public final static String TEST_INTERNAL_COLORMAP = "internal_colormap_sub.tif";
	
	/**
	 * tests opening of the file
	 */
	public void testIO() throws IOException{
				
		final File file = TestUtil.createFileFromAssets(getInstrumentation().getContext(),TEST_SMALL_BYTE);
		
		Dataset dataset = Drivers.open(file.getAbsolutePath(), null);

		assertNotNull(dataset);
		
		assertTrue(dataset instanceof GDALDataset);
		
		assertNotNull(dataset.getBoundingBox());
		
		assertNotNull(dataset.getCRS());
		
		final Rect dim = ((RasterDataset)dataset).getDimension();
		final int width  = dim.width();
		final int height = dim.height();
		
		assertTrue(width > 0);
		
		assertTrue(height > 0);
		
		dataset.close();
		
		if(file != null && file.exists()){			
			file.delete();
		}
		
	}

	/**
	 * tests reading a region of the file
	 */
	public void testRead() throws IOException{
		
		final File file = TestUtil.createFileFromAssets(getInstrumentation().getContext(),TEST_SMALL_BYTE);
		
		Dataset ds = Drivers.open(file.getAbsolutePath(), null);
		
		assertTrue(ds instanceof GDALDataset);
		
		GDALDataset dataset = (GDALDataset) ds;
		
		final Rect dim = dataset.getDimension();
		final int width  = dim.width();
		final int height = dim.height();
		
		final Envelope env = new Envelope(0, width / 10, 0, height / 10);
		final Rect rect = new Rect(0, 0, width/ 10, height / 10);
		     
        final RasterQuery query = new GDALRasterQuery(
        		env,
        		dataset.getCRS(),
        		dataset.getBands(),
        		rect,
        		dataset.getBands().get(0).datatype(),
        		rect);
        
        final Raster raster = dataset.read(query);
        
        final RasterOp renderer = new MAmplitudeRescaler();
        
        renderer.execute(raster, null, null, null);
        
        final int[] pixels  = new int[(int) (env.getWidth() * env.getHeight())];
	    
        raster.getData().asIntBuffer().get(pixels);
        
        assertNotNull(pixels);
        
        final int rasterWidth  = raster.getDimension().width();
      	final int rasterHeight = raster.getDimension().height();
        
        //check a pixel
        final int pixel = pixels[rasterWidth * rasterHeight / 2];
        
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
        
		if(file != null && file.exists()){			
			file.delete();
		}
        
	}
	/**
	 * tests the identification of Driver implementations 
	 * there exists currently two implementation (GDALDriver, MBTilesDriver)
	 * and one stub (TestPluggedDriver)
	 * which all should be found
	 */
	@SuppressWarnings({ "serial", "unchecked" })
	public void testDrivers(){
		
		//The "getDrivers" method is no longer public, hence use Reflection to execute it 
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
