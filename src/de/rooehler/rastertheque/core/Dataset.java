package de.rooehler.rastertheque.core;

import org.gdal.osr.SpatialReference;
import org.osgeo.proj4j.CoordinateReferenceSystem;

import de.rooehler.rastertheque.core.util.Disposable;

import com.vividsolutions.jts.geom.Envelope;

public interface Dataset extends Disposable {
	
	Driver getDriver();
	
	String getName();
	
	String getDescription();
	
	String getSource();
	
	SpatialReference getCRS();
	
	Envelope getBoundingBox();	
	

}
