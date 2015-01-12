package de.rooehler.rastertheque.processing;

import de.rooehler.rastertheque.core.Raster;

public interface Rendering {
	
	public boolean hasColorMap();
	
	public int[] generateGrayScalePixelsCalculatingMinMax(final Raster raster);
	
	public int[] generatePixelsWithColorMap(final Raster raster);
	
	public int[] generateThreeBandedRGBPixels(final Raster raster);

}
