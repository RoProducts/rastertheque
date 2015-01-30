package de.rooehler.rastertheque.processing;

import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.processing.Resampler.ResampleMethod;

public interface RawResampler {
	
	/**
	 * resample the Raster data from the rasters bounds dimension of this raster
	 * to the size of the dimension of this raster using a resampling method
	 * 
	 * after the operation the resampled data is stored inside the rasters byte buffer
	 * 
	 * @param raster the raster to resample
	 * @param method the resampling method to apply
	 */
	void resample(Raster raster, ResampleMethod method);

}
