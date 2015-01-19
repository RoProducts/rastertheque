package de.rooehler.rastertheque.processing.resampling;

import javax.media.jai.Interpolation;

import de.rooehler.native_jai.JaiInterpolate;
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
		
		JaiInterpolate.interpolate(srcPixels, srcWidth, srcHeight, dstPixels, dstWidth, dstHeight, Interpolation.getInstance(Interpolation.INTERP_BILINEAR));
		
	}


	protected void resampleBicubic(int[] srcPixels, int srcWidth, int srcHeight,int[] dstPixels, int dstWidth, int dstHeight) {
		
		if(srcWidth == dstWidth && srcHeight == dstHeight){
			System.arraycopy(srcPixels, 0, dstPixels, 0, srcPixels.length);
			return;
		}
		
		JaiInterpolate.interpolate(srcPixels, srcWidth, srcHeight, dstPixels, dstWidth, dstHeight, Interpolation.getInstance(Interpolation.INTERP_BICUBIC));
		
	}

	@Override
	protected void resampleNN(int[] srcPixels, int srcWidth, int srcHeight,	int[] dstPixels, int dstWidth, int dstHeight) {
		
		if(srcWidth == dstWidth && srcHeight == dstHeight){
			System.arraycopy(srcPixels, 0, dstPixels, 0, srcPixels.length);
			return;
		}
		
		JaiInterpolate.interpolate(srcPixels, srcWidth, srcHeight, dstPixels, dstWidth, dstHeight, Interpolation.getInstance(Interpolation.INTERP_NEAREST));
		
		
	}

}
