package de.rooehler.rastertheque.processing;

public interface Interpolation {

	public enum ResampleMethod implements Interpolation
	{
		NEARESTNEIGHBOUR,
		BILINEAR,
		BICUBIC;
	}
	

}
