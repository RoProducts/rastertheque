package de.rooehler.rasterapp.test;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import android.util.Log;

import com.vividsolutions.jts.geom.Envelope;

import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.core.RasterQuery;
import de.rooehler.rastertheque.io.gdal.GDALDataset;
import de.rooehler.rastertheque.io.gdal.GDALDriver;
import de.rooehler.rastertheque.processing.RasterOp;
import de.rooehler.rastertheque.processing.RasterOps;
import de.rooehler.rastertheque.processing.Interpolation.ResampleMethod;
import de.rooehler.rastertheque.processing.rendering.MColorMap;
import de.rooehler.rastertheque.processing.resampling.JAIResampler;
import de.rooehler.rastertheque.processing.resampling.MResampler;
import de.rooehler.rastertheque.processing.resampling.OpenCVResampler;
import de.rooehler.rastertheque.util.Hints;
import de.rooehler.rastertheque.util.Hints.Key;

public class TestProcessing extends android.test.AndroidTestCase  {
	
	/**
	 * tests and compares interpolations
	 */
	public void testBilinearInterpolation() throws IOException{		
		
		final GDALDriver driver = new GDALDriver();
		
		assertTrue(driver.canOpen(TestIO.GRAY_50M_BYTE));
		
		final GDALDataset dataset = driver.open(TestIO.GRAY_50M_BYTE);
		
		final Envelope  dim = dataset.getDimension();
		final int height = (int) dim.getHeight();
		final int width =  (int)dim.getWidth();
		
		final int tileSize = Math.min(width, height) / 10;
		
		final Envelope env = new Envelope(0, tileSize, 0, tileSize);
			     
        final RasterQuery query = new RasterQuery(
        		env,
        		dataset.getCRS(),
        		dataset.getBands(),
        		new Envelope(0, env.getWidth(), 0, env.getHeight()),
        		dataset.getBands().get(0).datatype());
        
        final Raster raster = dataset.read(query);
        
        final RasterOp renderer = new MColorMap();
        
        renderer.execute(raster, null, null, null);
        
        final int[] pixels = new int[(int) (env.getWidth() * env.getHeight())];
	    
        raster.getData().asIntBuffer().get(pixels);
        
        assertNotNull(pixels);
        
        final int resamplingFactor = 3;
        
        final int resampledSize = tileSize * resamplingFactor;
        
        final int[] mResampled = new int[resampledSize * resampledSize];
        final int[] jaiResampled = new int[resampledSize * resampledSize];
        final int[] openCVResampled = new int[resampledSize * resampledSize];
        
        final Envelope targetEnv = new Envelope(0, resampledSize, 0, resampledSize);
        
        HashMap<Key,Serializable> resizeParams = new HashMap<>();
        
        resizeParams.put(Hints.KEY_SIZE, targetEnv);
        
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
	}
	
	/**
	 * tests and compares resampling methods
	 */
	public void testResampling() throws IOException {
				
		final GDALDriver driver = new GDALDriver();
		
		assertTrue(driver.canOpen(TestIO.GRAY_50M_BYTE));
		
		final GDALDataset dataset = driver.open(TestIO.GRAY_50M_BYTE);

		final int readSize = 256;
		final int targetSize = 756;
		
		final Envelope env = new Envelope(0, 256, 0, 256);

        //1. Manually ///////////////
		
		int manual = resampleManually(env, dataset, readSize, targetSize);

        //2. with gdal ////////////
        int gdal = resampleWithGDAL(env, dataset, targetSize);
       
        assertEquals(manual, gdal);
              
		
	}
	
	public int resampleWithGDAL(final Envelope env, final GDALDataset dataset, final int targetSize){
		
        final RasterQuery gdalResampleQuery = new RasterQuery(
        		env,
        		dataset.getCRS(),
        		dataset.getBands(),
        		new Envelope(0, targetSize, 0 ,targetSize),
        		dataset.getBands().get(0).datatype());
        
        final long gdalNow = System.currentTimeMillis();
        
        final Raster raster = dataset.read(gdalResampleQuery);    
		final RasterOp renderer = new MColorMap();
		
        renderer.execute(raster, null, null, null);
        
        final int[] gdalResampledPixels  = new int[targetSize * targetSize];
	    
        raster.getData().asIntBuffer().get(gdalResampledPixels);
        
        assertNotNull(gdalResampledPixels);
        Log.d(TestProcessing.class.getSimpleName(), "GDAL resampling took "+ (System.currentTimeMillis() - gdalNow)+" ms");

        return gdalResampledPixels.length;
	}
	
	public int resampleManually(final Envelope env, final GDALDataset dataset,final int readSize, final int targetSize){
		
        final RasterQuery manualResamplingQuery = new RasterQuery(
        		env,
        		dataset.getCRS(),
        		dataset.getBands(),
        		new Envelope(0 , readSize, 0, readSize),
        		dataset.getBands().get(0).datatype());
        
        final long manualNow = System.currentTimeMillis();
        
        final Raster manualRaster = dataset.read(manualResamplingQuery);
		
        Log.d(TestProcessing.class.getSimpleName(), "gdal read took "+ (System.currentTimeMillis() - manualNow)+" ms");
        
        HashMap<Key,Serializable> resizeParams = new HashMap<>();

		resizeParams.put(Hints.KEY_SIZE, new Envelope(0, targetSize, 0, targetSize));
		
		resizeParams.put(Hints.KEY_INTERPOLATION, ResampleMethod.BILINEAR);
		
        new MResampler().execute(manualRaster,resizeParams,null,null);
        
        Log.d(TestProcessing.class.getSimpleName(), "manual resampling took "+ (System.currentTimeMillis() - manualNow)+" ms");
               
        return (int) (manualRaster.getDimension().getHeight() * manualRaster.getDimension().getWidth());
	}
	
	@SuppressWarnings("unchecked")
	public void testRasterOpServices(){
		
		//the getRasterOps method is private - it needs to reflection to test it
		Method method = null;
		try {
			method = RasterOps.class.getDeclaredMethod("getRasterOps", String.class, Class.class);
		} catch (Exception e) {
			Log.e(TestProcessing.class.getSimpleName(),"exception getting getRasterOps() ");
		}
		method.setAccessible(true);
		ArrayList<RasterOp> ops = null;
		try {
			ops = (ArrayList<RasterOp>) method.invoke(RasterOps.class.newInstance(), "org/rastertheque/processing/raster/",RasterOp.class);
		} catch (Exception e) {
			Log.e(TestProcessing.class.getSimpleName(),"exception invoking : " + method.getName());
		}
		
		assertNotNull(ops);
		
		//there are currently six rasterop impl + one test
		assertTrue(ops.size() == 7);
		
	}

}
