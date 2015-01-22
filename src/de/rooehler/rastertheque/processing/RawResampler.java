package de.rooehler.rastertheque.processing;

import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.processing.Resampler.ResampleMethod;

public interface RawResampler {
	
	
	void resample(Raster raster, ResampleMethod method);

}
