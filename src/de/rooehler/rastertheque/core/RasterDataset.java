package de.rooehler.rastertheque.core;

import java.util.List;

import android.graphics.Rect;


public interface RasterDataset extends Dataset{
	
	/**
	 * describes the Dimension of this dataset
	 * @return
	 */
	Rect getDimension();
	
	/**
	 * the bands of this dataset
	 * @return
	 */
	List<Band> getBands();
	
	/**
	 * performs a query against a rasterdataset 
	 * resulting in a Raster
	 * @param query
	 * @return
	 */
	Raster read(RasterQuery query);

}
