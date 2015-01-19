package de.rooehler.rasterapp.test;

import java.io.InputStream;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import de.rooehler.mapsforgerenderer.test.R;
import de.rooehler.rastertheque.processing.Resampler;
import de.rooehler.rastertheque.processing.Resampler.ResampleMethod;
import de.rooehler.rastertheque.processing.resampling.JAIResampler;
import de.rooehler.rastertheque.processing.resampling.MResampler;
import de.rooehler.rastertheque.processing.resampling.OpenCVResampler;

public class TestInterpolationSpeed extends android.test.ActivityTestCase {
	
	
	public void testInterpolationSpeed(){
		
		Context testContext = getInstrumentation().getContext();
		Resources testRes = testContext.getResources();
		InputStream is = testRes.openRawResource(R.drawable.lena);

		Bitmap original = BitmapFactory.decodeStream(is);

		assertNotNull(original);

		assertTrue(original.getWidth() == 512);

		final int origSize = original.getWidth() * original.getHeight();

		final int os = original.getWidth();
		final int ts = os * 4;
		
		final int targetSize = ts * ts;

		int[] pixels = new int[origSize];

		original.getPixels(pixels, 0, os, 0, 0, os, os);

		final Resampler[] resamplers = new Resampler[]{
				new OpenCVResampler(),
				new MResampler(),
				new JAIResampler()
		};

		for(Resampler resampler : resamplers){

			for(int i = 0; i < ResampleMethod.values().length; i++){

				int[] resampledPixels = new int[targetSize];
				long now = System.currentTimeMillis();
				final ResampleMethod m = ResampleMethod.values()[i];
				
				resampler.resample(pixels, os, os, resampledPixels, ts, ts, m);
				
				long took = System.currentTimeMillis() - now;
				
				assertTrue(resampledPixels.length == targetSize);
				assertEquals(pixels[0], resampledPixels[0]);
//				assertEquals(pixels[pixels.length - 1 ], resampledPixels[resampledPixels.length - 1]);
				
				Log.d(TestInterpolationOutput.class.getSimpleName(), "tested "+resampler.getClass().getSimpleName()+" "+ m.name()+" took : " + took );


			} 
		}
	}
	

}
