package de.rooehler.rastertheque.core;

import java.util.List;

import com.vividsolutions.jts.geom.Envelope;


public interface RasterDataset extends Dataset{
	
	/**
	 * describes the Dimension of this dataset
	 * @return
	 */
	Envelope getDimension();
	
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
