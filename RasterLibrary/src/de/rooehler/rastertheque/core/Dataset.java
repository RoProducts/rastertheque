package de.rooehler.rastertheque.core;

import org.osgeo.proj4j.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Envelope;

import de.rooehler.rastertheque.core.util.Disposable;
/**
 * A dataset models a 
 * 
 * @author Robert Oehler
 *
 */
public interface Dataset extends Disposable {
	
	/**
	 * Driver with which the dataset was opened
	 * @return the driver
	 */
	Driver getDriver();
	
	/**
	 * name of the dataset
	 * @return the name
	 */
	String getName();
	
	/**
	 * description of the dataset
	 * @return the description
	 */
	String getDescription();
	
	/**
	 * source of the dataset in terms of a filepath
	 * @return the filePath
	 */
	String getSource();
	
	/**
	 * the coordinate reference system of the dataset
	 * @return the crs
	 */
	CoordinateReferenceSystem getCRS();
	
	/**
	 * the domain of this dataset, in terms of spatial extent
	 * @return the bounds
	 */
	Envelope getBoundingBox();	
	

}
