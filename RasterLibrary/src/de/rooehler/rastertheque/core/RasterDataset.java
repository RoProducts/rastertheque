package de.rooehler.rastertheque.core;

import java.util.List;

import android.graphics.Rect;

/**
 * RasterDataset extends Dataset to specify the needs of a raster
 * 
 * @author Robert Oehler
 *
 */

public interface RasterDataset extends Dataset{
	
	/**
	 * describes the Dimension of this dataset
	 * @return the rect containing the dimension of this raster
	 */
	Rect getDimension();
	
	/**
	 * the bands of this dataset
	 * @return List of Bands of this raster
	 */
	List<Band> getBands();
	
	/**
	 * performs a query against this raster dataset 
	 * resulting in a Raster
	 * @param query specifying the properties to read from the dataset
	 * @return the raster that was read from the data set
	 */
	Raster read(RasterQuery query);
}
