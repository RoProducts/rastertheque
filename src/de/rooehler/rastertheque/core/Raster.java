package de.rooehler.rastertheque.core;


public abstract class Raster {

	public abstract int getMinZoom();
	public abstract int getMaxZoom();
	
	protected String mFilePath;
	
	
	public Raster(final String pFilePath){
		
		this.mFilePath = pFilePath;
	}
	
	public String getFilePath(){
		return mFilePath;
	}
	

}
