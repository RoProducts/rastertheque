//package de.rooehler.rasterapp.test;
//
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.InputStream;
//
//import android.content.Context;
//import android.content.res.Resources;
//import android.graphics.Bitmap;
//import android.graphics.Bitmap.Config;
//import android.graphics.BitmapFactory;
//import android.os.Environment;
//import android.util.Log;
//import de.rooehler.mapsforgerenderer.test.R;
//import de.rooehler.rastertheque.processing.Resampler;
//import de.rooehler.rastertheque.processing.Resampler.ResampleMethod;
//import de.rooehler.rastertheque.processing.resampling.raw.OpenCVResampler;
//
//
//public class TestInterpolationOutput extends android.test.ActivityTestCase {
//	
//	
//	public void testInterpolationMethods(){
//
//		Context testContext = getInstrumentation().getContext();
//		Resources testRes = testContext.getResources();
//		InputStream is = testRes.openRawResource(R.drawable.lena);
//
//		Bitmap original = BitmapFactory.decodeStream(is);
//
//		assertNotNull(original);
//
//		assertTrue(original.getWidth() == 512);
//
//		final int origSize = original.getWidth() * original.getHeight();
//
//		final int os = original.getWidth();
//		final int ts = os * 4;
//		
//		final int targetSize = ts * ts;
//
//		int[] pixels = new int[origSize];
//
//		original.getPixels(pixels, 0, os, 0, 0, os, os);
//
//		Resampler resampler = new OpenCVResampler();	
//
//		for(int i = 0; i < ResampleMethod.values().length; i++){
//
//			int[] resampledPixels = new int[targetSize];
//
//			final ResampleMethod m = ResampleMethod.values()[i];
//			Log.d(TestInterpolationOutput.class.getSimpleName(), "testing "+m.name());
//
//			resampler.resample(pixels, os, os, resampledPixels, ts, ts, m);
//
//			Bitmap bitmap = Bitmap.createBitmap(ts, ts, Config.ARGB_8888);
//			bitmap.setPixels(resampledPixels, 0, ts, 0, 0, ts, ts);	
//
//			saveImage(bitmap,resampler.getClass().getSimpleName()+"_"+ m.name());
//
//		}  
//	}
//	
//	private void saveImage(Bitmap finalBitmap, final String name) {
//
//	    String root = Environment.getExternalStorageDirectory().toString();
//	    File myDir = new File(root + "/rastertheque");    
//	    if(!myDir.exists()){
//	    	myDir.mkdirs();
//	    }
//
//	    String fname = name +".png";
//	    File file = new File (myDir, fname);
//	    if (file.exists ()){
//	    	file.delete (); 
//	    }
//	    try {
//	           FileOutputStream out = new FileOutputStream(file);
//	           finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
//	           out.flush();
//	           out.close();
//
//	    } catch (Exception e) {
//	    		Log.e(TestInterpolationOutput.class.getSimpleName(), "error saving "+name,e);
//	    		throw new AssertionError("error saving name");
//	    }
//	}
//
//}
