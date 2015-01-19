package de.rooehler.rastertheque.processing.resampling;


import java.io.File;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.os.Environment;
import android.util.Log;
import de.rooehler.rastertheque.processing.Resampler;

public class OpenCVResampler implements Resampler {
	
	static {
	    if (!OpenCVLoader.initDebug()) {
	        // Handle initialization error
	    	
	    	Log.e(OpenCVResampler.class.getSimpleName(), "error initialising OpenCV");
	    } 
	}
	
	

	@Override
	public void resample(int[] srcPixels, int srcWidth, int srcHeight,	int[] dstPixels, int dstWidth, int dstHeight, ResampleMethod method) {
		
		if(srcWidth == dstWidth && srcHeight == dstHeight){
			System.arraycopy(srcPixels, 0, dstPixels, 0, srcPixels.length);
			return;
		}
		
		int i = 0;
		switch (method) {
		case NEARESTNEIGHBOUR:
			i = Imgproc.INTER_NEAREST;
			break;
		case BILINEAR:
			i = Imgproc.INTER_LINEAR;
			break;
		case BICUBIC:
			i = Imgproc.INTER_CUBIC;
			break;
		}
		
		final float scaleFactorX = dstWidth / srcWidth;
		final float scaleFactorY = dstHeight / srcHeight;
		
		Bitmap bitmap = Bitmap.createBitmap(srcWidth, srcHeight, Config.ARGB_8888);
        bitmap.setPixels(srcPixels, 0, srcWidth, 0, 0, srcWidth, srcHeight);
        Mat srcMat = new Mat();
		
//        Mat srcMat = new Mat(srcHeight, srcWidth, CvType.CV_32SC1);
//        
//        Mat convertMat = new Mat(srcHeight, srcWidth,  CvType.CV_8UC4);
//        
//        Imgproc.cvtColor(srcMat, convertMat, Imgproc.COLOR_mRGBA2RGBA);
        
        org.opencv.android.Utils.bitmapToMat(bitmap, srcMat);


		Mat dstMat = new Mat();
	
		Imgproc.resize(srcMat, dstMat, new Size(), scaleFactorX, scaleFactorY, i);
		
		Bitmap resizedBitmap = Bitmap.createBitmap(dstWidth, dstHeight, Config.ARGB_8888);
		org.opencv.android.Utils.matToBitmap(dstMat, resizedBitmap);
		 
		resizedBitmap.getPixels(dstPixels, 0, dstWidth, 0, 0, dstWidth, dstHeight);
	}


	
	@SuppressWarnings("unused")
	private static void testDirectRead(){
		
		String root = Environment.getExternalStorageDirectory().toString();
		File file = new File(root + "/rastertheque/earth_BILINEAR.png");    
	    File writefile = new File(root + "/rastertheque/earth_BILINEAR_openCV.png");    
	    
	    Mat m = Highgui.imread(file.getAbsolutePath());
	    
		int depth = m.depth();
		int channels = m.channels();
	    
	    Mat dst = new Mat();
	    
	   Imgproc.resize(m, dst, new Size(),2,2, Imgproc.INTER_LINEAR);
	   
	   Highgui.imwrite(writefile.getAbsolutePath(), dst);
	    
	}

}
