package de.rooehler.rastertheque.processing.resampling;

import de.rooehler.raster_jai.JaiInterpolate;
import de.rooehler.rastertheque.processing.Resampling;

public class JAI_Interpolation implements Resampling{

	@Override
	public void resampleBilinear(int[] srcPixels, int srcSize, int[] dstPixels,	int dstSize) {

		
		JaiInterpolate.interpolate2D(srcPixels, srcSize, dstPixels, dstSize);
		
	}

}
