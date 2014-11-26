package de.rooehler.rastertheque.core;

import java.nio.ByteBuffer;

import org.osgeo.proj4j.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Envelope;


public interface RasterDataSet {
	
	String getSource();
	
	CoordinateReferenceSystem getCRS();
	
	Envelope getEnvelope();
	

}