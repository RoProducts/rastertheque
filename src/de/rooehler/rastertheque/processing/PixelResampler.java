package de.rooehler.rastertheque.processing;

public abstract interface PixelResampler extends Resampler{
		
	void resample(int srcPixels[], int srcWidth, int srcHeight, int dstPixels[], int dstWidth, int dstHeight, final ResampleMethod method);

	
}
