package de.rooehler.rastertheque.processing.resampling;

import javax.media.jai.InterpolationBicubic;
import javax.media.jai.InterpolationBilinear;
import javax.media.jai.InterpolationNearest;

import de.rooehler.raster_jai.JaiInterpolate;
import de.rooehler.rastertheque.processing.Resampler;

public class JAIResampler extends Resampler{


	public JAIResampler(ResampleMethod method) {
		super(method);

	}

	protected void resampleBilinear(int srcPixels[], int srcWidth, int srcHeight, int dstPixels[], int dstWidth, int dstHeight){

		if(srcWidth == dstWidth && srcHeight == dstHeight){
			System.arraycopy(srcPixels, 0, dstPixels, 0, srcPixels.length);
			return;
		}
		
		JaiInterpolate.interpolate(srcPixels, srcWidth, srcHeight, dstPixels, dstWidth, dstHeight, new InterpolationBilinear());
		
	}


	protected void resampleBicubic(int[] srcPixels, int srcWidth, int srcHeight,int[] dstPixels, int dstWidth, int dstHeight) {
		
		if(srcWidth == dstWidth && srcHeight == dstHeight){
			System.arraycopy(srcPixels, 0, dstPixels, 0, srcPixels.length);
			return;
		}
		
		JaiInterpolate.interpolate(srcPixels, srcWidth, srcHeight, dstPixels, dstWidth, dstHeight, new InterpolationBicubic(0));
		
	}

	@Override
	protected void resampleNN(int[] srcPixels, int srcWidth, int srcHeight,	int[] dstPixels, int dstWidth, int dstHeight) {
		
		JaiInterpolate.interpolate(srcPixels, srcWidth, srcHeight, dstPixels, dstWidth, dstHeight, new InterpolationNearest());
		
		
	}



}
