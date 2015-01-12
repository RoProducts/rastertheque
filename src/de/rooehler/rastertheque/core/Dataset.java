package de.rooehler.rastertheque.core;

import org.osgeo.proj4j.CoordinateReferenceSystem;

import de.rooehler.rastertheque.core.util.Disposable;

public interface Dataset extends Disposable {
	
	Driver getDriver();
	
	String getName();
	
	String getDescription();
	
	String getSource();
	
	CoordinateReferenceSystem getCRS();
	
	BoundingBox getBoundingBox();	
	

}
