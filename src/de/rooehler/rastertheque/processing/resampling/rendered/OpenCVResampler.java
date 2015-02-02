package de.rooehler.rastertheque.processing.resampling.rendered;


import java.io.File;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.os.Environment;
import android.util.Log;
import de.rooehler.rastertheque.processing.PixelResampler;

public class OpenCVResampler implements PixelResampler {
	
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
		//TODO find a way to avoid the convertion to bitmap ( pixels->bitmap->mat->resample->bitmap->pixels )
		//as seen at https://github.com/Itseez/opencv/blob/master/modules/java/generator/src/cpp/utils.cpp
		
		Bitmap bitmap = Bitmap.createBitmap(srcWidth, srcHeight, Config.ARGB_8888);
        bitmap.setPixels(srcPixels, 0, srcWidth, 0, 0, srcWidth, srcHeight);
        
        Mat srcMat = new Mat();
        org.opencv.android.Utils.bitmapToMat(bitmap, srcMat);

		Mat dstMat = new Mat();
	
		Imgproc.resize(srcMat, dstMat, new Size(dstWidth, dstHeight), 0, 0, i);
		
		Bitmap resizedBitmap = Bitmap.createBitmap(dstWidth, dstHeight, Config.ARGB_8888);
		org.opencv.android.Utils.matToBitmap(dstMat, resizedBitmap);
		 
		resizedBitmap.getPixels(dstPixels, 0, dstWidth, 0, 0, dstWidth, dstHeight);
	}

	public static double[] copyFromIntArray(int[] source) {
	    double[] dest = new double[source.length];
	    for(int i=0; i<source.length; i++) {
	        dest[i] = source[i];
	    }
	    return dest;
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
