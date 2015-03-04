package de.rooehler.mapsforgerenderer.interfaces;

/**
 * Interface which is used to give visual feedback about the
 * progress of the visualization of map tiles
 * 
 * @author Robert Oehler
 *
 */
public interface IWorkStatus {
	
	public void isRendering();
	
	public void renderingFinished();

}
