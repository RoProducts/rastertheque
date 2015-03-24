package de.rooehler.rasterapp.test;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.graphics.Rect;
import android.util.Log;

import com.vividsolutions.jts.geom.Envelope;

import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.core.RasterQuery;
import de.rooehler.rastertheque.io.gdal.GDALDataset;
import de.rooehler.rastertheque.io.gdal.GDALDriver;
import de.rooehler.rastertheque.io.gdal.GDALRasterQuery;
import de.rooehler.rastertheque.processing.Interpolation.ResampleMethod;
import de.rooehler.rastertheque.processing.RasterOp;
import de.rooehler.rastertheque.processing.RasterOps;
import de.rooehler.rastertheque.processing.rendering.MAmplitudeRescaler;
import de.rooehler.rastertheque.processing.rendering.MColorMap;
import de.rooehler.rastertheque.processing.resampling.JAIResampler;
import de.rooehler.rastertheque.processing.resampling.MResampler;
import de.rooehler.rastertheque.processing.resampling.OpenCVResampler;
import de.rooehler.rastertheque.processing.resampling.Resampler;
import de.rooehler.rastertheque.util.Hints;
import de.rooehler.rastertheque.util.Hints.Key;

/**
 * tests the processing part of the library :
 * 
 * performance of resampling operations
 * 
 * comparison of a resampling by GDAL to a manual Resampling
 * 
 * the functionality of the RaterOps facade
 * 
 * @author Robert Oehler
 *
 */
public class TestProcessing extends android.test.ActivityTestCase  {
	
	/**
	 * tests interpolations in terms of performance (time)
	 */
	public void dotestBilinearInterpolation() throws IOException{		
		
		final GDALDriver driver = new GDALDriver();
		
		final File file = TestUtil.createFileFromAssets(getInstrumentation().getContext(),TestIO.TEST_SMALL_BYTE);
		
		assertTrue(driver.canOpen(file.getAbsolutePath()));
		
		final GDALDataset dataset = driver.open(file.getAbsolutePath());
		
		final Rect dim = dataset.getDimension();
		final int width  = dim.width();
		final int height = dim.height();
		
		final int tileSize = Math.min(width, height);
		
		final Envelope env = new Envelope(0, tileSize, 0, tileSize);
			     
        final RasterQuery query = new GDALRasterQuery(
        		env,
        		dataset.getCRS(),
        		dataset.getBands(),
        		new Rect(0, 0, tileSize, tileSize),
        		dataset.getBands().get(0).datatype(),
        		new Rect(0, 0, tileSize, tileSize));
        
        final Raster raster = dataset.read(query);
        
        final RasterOp renderer = new MAmplitudeRescaler();
        
        renderer.execute(raster, null, null, null);
        
        final int[] pixels = new int[(int) (env.getWidth() * env.getHeight())];
	    
        raster.getData().asIntBuffer().get(pixels);
        
        assertNotNull(pixels);
        
        final int resamplingFactor = 3;
        
        final int resampledSize = tileSize * resamplingFactor;
        
        final int[] mResampled = new int[resampledSize * resampledSize];
        final int[] jaiResampled = new int[resampledSize * resampledSize];
        final int[] openCVResampled = new int[resampledSize * resampledSize];
        
        HashMap<Key,Serializable> resizeParams = new HashMap<>();
        
        resizeParams.put(Resampler.KEY_SIZE, new Double[]{resampledSize / env.getWidth(), resampledSize/ env.getHeight()});
        
    	resizeParams.put(Hints.KEY_INTERPOLATION, ResampleMethod.BILINEAR);
        
        /////// MImp ///////
        
        long now = System.currentTimeMillis();
        
        new MResampler().execute(raster,resizeParams,null, null);
        
        Log.d(TestProcessing.class.getSimpleName(), "MInterpolation took "+ (System.currentTimeMillis() - now));
           
        /////// JAI ///////
        
        now = System.currentTimeMillis();
        
        new JAIResampler().execute(raster,resizeParams, null, null);
        
        Log.d(TestProcessing.class.getSimpleName(), "JAI took "+ (System.currentTimeMillis() - now));
        
        assertTrue(jaiResampled.length == pixels.length * resamplingFactor * resamplingFactor);
        
        /////// OpenCV ///////
        
        now = System.currentTimeMillis();
        
        new OpenCVResampler().execute(raster,resizeParams,null, null);
        
        Log.d(TestProcessing.class.getSimpleName(), "OpenCV took "+ (System.currentTimeMillis() - now));
        
        assertTrue(openCVResampled.length == pixels.length * resamplingFactor * resamplingFactor);
        
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
        
        TestUtil.deletefile(file);
	}
	
	/**
	 * tests and compares resampling methods
	 */
	public void testResampling() throws IOException {
		
		final GDALDriver driver = new GDALDriver();
		
		final File file = TestUtil.createFileFromAssets(getInstrumentation().getContext(),TestIO.TEST_SMALL_BYTE);
		
		assertNotNull(file);
						
		assertTrue(driver.canOpen(file.getAbsolutePath()));
		
		final GDALDataset dataset = driver.open(file.getAbsolutePath());

		final int readSize = 256;
		final int targetSize = 756;
		
		final Envelope env = new Envelope(0, 256, 0, 256);

        //1. Manually ///////////////
		
		int manual = resampleManually(env, dataset, readSize, targetSize);

        //2. with gdal ////////////
        int gdal = resampleWithGDAL(env, dataset, targetSize);
       
        assertEquals(manual, gdal);
              
		TestUtil.deletefile(file);
	}
	
	public int resampleWithGDAL(final Envelope env, final GDALDataset dataset, final int targetSize){
		
        final RasterQuery gdalResampleQuery = new GDALRasterQuery(
        		env,
        		dataset.getCRS(),
        		dataset.getBands(),
        		new Rect(0, 0, targetSize,targetSize),
        		dataset.getBands().get(0).datatype(),
        		new Rect(0, 0, targetSize,targetSize));
        
        final long gdalNow = System.currentTimeMillis();
        
        final Raster raster = dataset.read(gdalResampleQuery);    
		final RasterOp renderer = new MAmplitudeRescaler();
		
        renderer.execute(raster, null, null, null);
        
        final int[] gdalResampledPixels  = new int[targetSize * targetSize];
	    
        raster.getData().asIntBuffer().get(gdalResampledPixels);
        
        assertNotNull(gdalResampledPixels);
        Log.d(TestProcessing.class.getSimpleName(), "GDAL resampling took "+ (System.currentTimeMillis() - gdalNow)+" ms");

        return gdalResampledPixels.length;
	}
	
	public int resampleManually(final Envelope env, final GDALDataset dataset,final int readSize, final int targetSize){
		
        final RasterQuery manualResamplingQuery = new GDALRasterQuery(
        		env,
        		dataset.getCRS(),
        		dataset.getBands(),
        		new Rect(0, 0, readSize, readSize),
        		dataset.getBands().get(0).datatype(),
        		new Rect(0, 0, readSize, readSize));
        
        final long manualNow = System.currentTimeMillis();
        
        final Raster manualRaster = dataset.read(manualResamplingQuery);
		
        Log.d(TestProcessing.class.getSimpleName(), "gdal read took "+ (System.currentTimeMillis() - manualNow)+" ms");
        
        HashMap<Key,Serializable> resizeParams = new HashMap<>();

		resizeParams.put(Resampler.KEY_SIZE, new Double[]{targetSize / (double) readSize, targetSize / (double) readSize});
		
		resizeParams.put(Hints.KEY_INTERPOLATION, ResampleMethod.BILINEAR);
		
        new MResampler().execute(manualRaster,resizeParams,null,null);
        
        Log.d(TestProcessing.class.getSimpleName(), "manual resampling took "+ (System.currentTimeMillis() - manualNow)+" ms");
          
        final int newWidth  = manualRaster.getDimension().width();
		final int newHeight = manualRaster.getDimension().height();
        
        return newHeight * newWidth;
	}
	
	/**
	 * test if all currently available implementations of RasterOp are retrieved during runtime
	 */
	@SuppressWarnings("unchecked")
	public void dotestRasterOpServices(){
		
		//the getRasterOps method is private - it needs to reflection to test it
		Method method = null;
		try {
			method = RasterOps.class.getDeclaredMethod("getRasterOps", String.class);
		} catch (Exception e) {
			Log.e(TestProcessing.class.getSimpleName(),"exception getting getRasterOps() ");
		}
		method.setAccessible(true);
		Map<String,List<RasterOp>> ops = null;
		try {
			ops = (HashMap<String,List<RasterOp>>) method.invoke(RasterOps.class.newInstance(), "org/rastertheque/processing/raster/");
		} catch (Exception e) {
			Log.e(TestProcessing.class.getSimpleName(),"exception invoking : " + method.getName());
		}
		
		assertNotNull(ops);
		
		int count = 0;
		
		for(String key : ops.keySet()){
			List<RasterOp> list = ops.get(key);
			count += list.size();
		}
		//there are currently six rasterop impl + one test
		assertTrue(count == 7);
		
	}

}
