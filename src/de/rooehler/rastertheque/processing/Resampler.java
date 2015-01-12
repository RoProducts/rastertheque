package de.rooehler.rastertheque.processing;

public interface Resampler {

	void resampleBilinear(int srcPixels[], int srcWidth, int srcHeight, int dstPixels[], int dstWidth, int dstHeight);
}
