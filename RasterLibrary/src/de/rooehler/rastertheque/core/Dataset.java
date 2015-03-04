package de.rooehler.rastertheque.core;

import org.osgeo.proj4j.CoordinateReferenceSystem;

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
	
	CoordinateReferenceSystem getCRS();
	
	Envelope getBoundingBox();	
	

}
