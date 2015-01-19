package de.rooehler.rastertheque.processing;

import de.rooehler.rastertheque.core.Raster;

public interface Renderer {

	
	int[] render(final Raster raster);
	
	void useRGBBands(boolean hasRgbBands);
	

}
