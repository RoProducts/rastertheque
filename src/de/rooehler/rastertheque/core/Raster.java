package de.rooehler.rastertheque.core;

import java.nio.ByteBuffer;
import java.util.List;

import org.osgeo.proj4j.CoordinateReferenceSystem;

public class Raster {
	
    /**
     * The bounds of the raster.
     */
    BoundingBox bb;

    /**
     * The projection of the raster.
     */
    CoordinateReferenceSystem crs;

    /**
     * The dimensions of the raster.
     */
    Dimension dimension;

    /**
     * Bands contained in the raster.
     */
    List<Band> bands;

    /**
     * The raw raster data.
     */
    ByteBuffer data;

    /**
     * Nodata for this raster.
     */
    NoData nodata = NoData.NONE;

    
    
    public Raster(BoundingBox bb, CoordinateReferenceSystem crs, Dimension size, List<Band> bands, ByteBuffer data, NoData nodata) {
    	
    	this.bb = bb;
    	this.crs = crs;
    	this.dimension = size;
    	this.bands = bands;
    	this.data = data;
    	this.nodata = nodata;
    }
    public Raster(ByteBuffer data,  Dimension size,  List<Band> bands) {

		this.data = data;
		this.dimension = size;
		this.bands = bands;
	}

	/**
     * The bounds of the raster in world coordinates.
     */
    public BoundingBox getBoundingBox() {
        return bb;
    }

    /**
     * Sets the bounds of the raster in world coordinates.
     */
    public void setBoundingBox(BoundingBox boundingBox) {
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
    public Dimension getDimension() {
        return dimension;
    }

    /**
     * Sets the dimensions of the raster.
     */
    public void setDimension(Dimension size) {
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

    /**
     * The nodata for the raster.
     */
    public NoData getNodata() {
        return nodata;
    }

    /**
     * Sets the nodata for the raster.
     */
    public void setNodata(NoData nodata) {
        this.nodata = nodata;
    }

}
