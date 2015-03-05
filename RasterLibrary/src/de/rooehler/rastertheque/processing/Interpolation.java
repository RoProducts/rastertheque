package de.rooehler.rastertheque.processing;

/**
 * Interpolations define how pixels are interpolated
 * during raster operations
 * 
 * @author Robert Oehler
 *
 */
public interface Interpolation {

	public enum ResampleMethod implements Interpolation
	{
		NEARESTNEIGHBOUR,
		BILINEAR,
		BICUBIC;
	}
	

}
