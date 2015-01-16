package de.rooehler.rastertheque.processing.resampling;

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
		
		JaiInterpolate.interpolateBilinear(srcPixels, srcWidth, srcHeight, dstPixels, dstWidth, dstHeight);
		
	}


	protected void resampleBicubic(int[] srcPixels, int srcWidth, int srcHeight,int[] dstPixels, int dstWidth, int dstHeight) {
		
		if(srcWidth == dstWidth && srcHeight == dstHeight){
			System.arraycopy(srcPixels, 0, dstPixels, 0, srcPixels.length);
			return;
		}
		
		JaiInterpolate.interpolateBicubic(srcPixels, srcWidth, srcHeight, dstPixels, dstWidth, dstHeight);
		
	}

	@Override
	protected void resampleNN(int[] srcPixels, int srcWidth, int srcHeight,
			int[] dstPixels, int dstWidth, int dstHeight) {
		
		
	}



}
