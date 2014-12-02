package de.rooehler.rastertheque.core;

import org.osgeo.proj4j.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Envelope;


public interface RasterDataSet {
		
	String getSource();
	
	CoordinateReferenceSystem getCRS();
	
	Envelope getEnvelope();
	
	void close();
	

}
