package de.rooehler.rastertheque.processing;

public interface IResampling {
	
	void resampleBilinear(int srcPixels[], int srcSize, int dstPixels[], int dstSize);

}
