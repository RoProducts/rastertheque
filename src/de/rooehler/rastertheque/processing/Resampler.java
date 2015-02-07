package de.rooehler.rastertheque.processing;

import com.vividsolutions.jts.geom.Envelope;

import de.rooehler.rastertheque.core.Raster;

public interface Resampler {
	
	public enum ResampleMethod
	{
		NEARESTNEIGHBOUR,
		BILINEAR,
		BICUBIC;
	}
	
	void resample(Raster raster, Envelope dstDim, ResampleMethod method, ProgressListener listener);

}
