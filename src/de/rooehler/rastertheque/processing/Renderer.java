package de.rooehler.rastertheque.processing;

import de.rooehler.rastertheque.core.Raster;

public interface Renderer {
	
	public boolean hasColorMap();
	
	public int[] grayscale(final Raster raster);
	
	public int[] colormap(final Raster raster);
	
	public int[] rgbBands(final Raster raster);

}
