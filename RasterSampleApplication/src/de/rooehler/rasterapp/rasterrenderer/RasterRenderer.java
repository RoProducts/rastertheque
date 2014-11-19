package de.rooehler.rasterapp.rasterrenderer;

import org.mapsforge.core.graphics.TileBitmap;
/**
 * A RasterRenderer is the base interface for a renderer of raster data
 * 
 * @author Robert Oehler
 *
 */

public interface RasterRenderer {

	/**
	 * render this particular job and return the rendered TileBitmap
	 */
	public TileBitmap executeJob(RasterJob job);
	/**
	 * prepare for rendering
	 */
	public void start();
	/**
	 * close resources
	 */
	public void stop();
	/**
	 * 
	 * @return a flag if this is actually working, i.e. has opened resources @see open @see close
	 */
	public boolean isWorking();
	/**
	 * 
	 * @return the absolute path to this renderers resource
	 */
	public String getFilePath();
	/**
	 * destroy all created objects
	 */
	public void destroy();

}
