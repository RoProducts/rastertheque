package de.rooehler.rastersampleapplication.rasterrenderer;

import org.mapsforge.core.graphics.TileBitmap;


public abstract class RasterRenderer {

	
	public abstract TileBitmap executeJob(RasterJob job);
	
	public abstract void start();
	
	public abstract void stop();
	
	public abstract boolean isWorking();
	
	public abstract String getFilePath();
	
	public abstract void destroy();

}
