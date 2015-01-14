package de.rooehler.rastertheque.core;

import java.nio.ByteBuffer;
import java.util.List;

import org.osgeo.proj4j.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Envelope;

public class Raster {
	
    /**
     * The bounds of the raster.
     */
    Envelope bb;

    /**
     * The projection of the raster.
     */
    CoordinateReferenceSystem crs;

    /**
     * The dimensions of the raster.
     */
    Envelope dimension;

    /**
     * Bands contained in the raster.
     */
    List<Band> bands;

    /**
     * The raw raster data.
     */
    ByteBuffer data;

    
    
    public Raster(Envelope bb, CoordinateReferenceSystem crs, Envelope size, List<Band> bands, ByteBuffer data) {
    	
    	this.bb = bb;
    	this.crs = crs;
    	this.dimension = size;
    	this.bands = bands;
    	this.data = data;
    }

	/**
     * The bounds of the raster in world coordinates.
     */
    public Envelope getBoundingBox() {
        return bb;
    }

    /**
     * Sets the bounds of the raster in world coordinates.
     */
    public void setBoundingBox(Envelope boundingBox) {
        this.bb = boundingBox;
    }

    /**
     * The world projection of the raster.
     */
    public CoordinateReferenceSystem getCRS() {
        return crs;
    }

    /**
     * Sets the world projection of the raster.
     */
    public void setCRS(CoordinateReferenceSystem crs) {
        this.crs = crs;
    }

    /**
     * The dimensions of the raster.
     */
    public Envelope getDimension() {
        return dimension;
    }

    /**
     * Sets the dimensions of the raster.
     */
    public void setDimension(Envelope size) {
        this.dimension = size;
    }

    /**
     * The raw data for the raster.
     */
    public ByteBuffer getData() {
        return data;
    }

    /**
     * Sets the raw data for the raster.
     */
    public void setData(ByteBuffer data) {
        this.data = data;
    }

    /**
     * The bands contained in the raster.
     */
    public List<Band> getBands() {
        return bands;
    }

    /**
     * Sets the bands contained in the raster.
     */
    public void setBands(List<Band> bands) {
        this.bands = bands;
    }

}
