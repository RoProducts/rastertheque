package de.rooehler.mapsforgerenderer.rasterrenderer;

import java.io.File;

import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.queue.Job;
import org.mapsforge.map.model.DisplayModel;
/**
 * A RasterJob contains information about the raster that is to render
 * To identify a RasterJob inside a cache information not only about the tile
 * but also the source has to be included into the hashcodevalue
 * 
 * @author Robert Oehler
 *
 */
public class RasterJob extends Job {

	public final DisplayModel displayModel;
	private final int hashCodeValue;
	private File mFile;

	protected RasterJob(Tile pTile, DisplayModel pDisplayModel, final File pFile, boolean pHasAlpha) {
		super(pTile, pHasAlpha);

		this.mFile = pFile;

		this.displayModel = pDisplayModel;

		this.hashCodeValue = calculateHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!super.equals(obj)) {
			return false;
		} else if (!(obj instanceof RasterJob)) {
			return false;
		}
		RasterJob other = (RasterJob) obj;
		if (!this.mFile.equals(other.mFile)) {
			return false;
		} else if (!this.displayModel.equals(other.displayModel)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return this.hashCodeValue;
	}

	private int calculateHashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + this.mFile.hashCode();
		return result;
	}

}
