package de.rooehler.rastertheque.processing;

public abstract interface Resampler {
	
	public enum ResampleMethod
	{
		NEARESTNEIGHBOUR,
		BILINEAR,
		BICUBIC;
	}

}
