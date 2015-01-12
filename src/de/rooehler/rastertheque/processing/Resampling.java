package de.rooehler.rastertheque.processing;

public interface Resampling {
	
	void resampleBilinear(int srcPixels[], int srcSize, int dstPixels[], int dstSize);

}
