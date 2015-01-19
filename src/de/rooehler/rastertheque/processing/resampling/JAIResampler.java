package de.rooehler.rastertheque.processing.resampling;

import javax.media.jai.Interpolation;

import de.rooehler.native_jai.JaiInterpolate;
import de.rooehler.rastertheque.processing.Resampler;

public class JAIResampler implements Resampler{


	@Override
	public void resample(int srcPixels[], int srcWidth, int srcHeight, int dstPixels[], int dstWidth, int dstHeight, ResampleMethod method){

		if(srcWidth == dstWidth && srcHeight == dstHeight){
			System.arraycopy(srcPixels, 0, dstPixels, 0, srcPixels.length);
			return;
		}
		
		Interpolation i = null;
		switch (method) {
		case NEARESTNEIGHBOUR:
			i = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
			break;
		case BILINEAR:
			Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
			break;
		case BICUBIC:
			Interpolation.getInstance(Interpolation.INTERP_BICUBIC);
			break;
		}
		
		JaiInterpolate.interpolate(srcPixels, srcWidth, srcHeight, dstPixels, dstWidth, dstHeight, i);
		
	}

}
