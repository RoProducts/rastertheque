package de.rooehler.rastertheque.io.mbtiles;

import java.io.File;

import de.rooehler.rastertheque.core.Raster;

public class MBTilesRaster extends Raster {
	
	
	public MBTilesRaster(final String pFilePath){
		
		super(pFilePath);
		
		
	}

	@Override
	public int getMinZoom() {
		return 10;
	}

	@Override
	public int getMaxZoom() {
		return 16;
	}

}
