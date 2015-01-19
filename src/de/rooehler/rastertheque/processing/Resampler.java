package de.rooehler.rastertheque.processing;

public abstract interface Resampler {
	
	public enum ResampleMethod
	{
		NEARESTNEIGHBOUR,
		BILINEAR,
		BICUBIC;
	}
	
	void resample(int srcPixels[], int srcWidth, int srcHeight, int dstPixels[], int dstWidth, int dstHeight, final ResampleMethod method);

	
}
