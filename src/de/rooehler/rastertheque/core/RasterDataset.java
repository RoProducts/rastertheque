package de.rooehler.rastertheque.core;

import java.util.List;

import de.rooehler.rastertheque.core.model.Dimension;


public interface RasterDataset extends Dataset{
	
	/**
	 * describes the Dimension of this dataset
	 * @return
	 */
	Dimension getDimension();
	
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
