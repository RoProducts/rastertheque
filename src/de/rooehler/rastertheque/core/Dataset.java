package de.rooehler.rastertheque.core;

import org.gdal.osr.SpatialReference;

import com.vividsolutions.jts.geom.Envelope;

import de.rooehler.rastertheque.core.util.Disposable;
/**
 * Interface for all Datasets
 * 
 * @author Robert Oehler
 *
 */
public interface Dataset extends Disposable {
	
	Driver getDriver();
	
	String getName();
	
	String getDescription();
	
	String getSource();
	
	SpatialReference getCRS();
	
	Envelope getBoundingBox();	
	

}
