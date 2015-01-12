package de.rooehler.rastertheque.processing.resampling;

import de.rooehler.raster_jai.JaiInterpolate;
import de.rooehler.rastertheque.processing.Resampler;

public class JAIResampler implements Resampler{

	@Override
	public void resampleBilinear(int srcPixels[], int srcWidth, int srcHeight, int dstPixels[], int dstWidth, int dstHeight){

		if(srcWidth == dstWidth && srcHeight == dstHeight){
			System.arraycopy(srcPixels, 0, dstPixels, 0, srcPixels.length);
			return;
		}
		
		JaiInterpolate.interpolate2D(srcPixels, srcWidth, srcHeight, dstPixels, dstWidth, dstHeight);
		
	}

}
