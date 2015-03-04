package de.rooehler.rastertheque.core;

import java.nio.ByteBuffer;
import java.util.Hashtable;
import java.util.List;

import org.gdal.osr.SpatialReference;
import org.osgeo.proj4j.CoordinateReferenceSystem;

import android.graphics.Rect;

import com.vividsolutions.jts.geom.Envelope;
/**
 * Raster contains the result of
 * a read process against a dataset
 * 
 * @author Robert Oehler
 *
 */
public class Raster {
	
    /**
     * The bounds of the raster.
     */
    Envelope bounds;

    /**
     * The projection of the raster.
     */
    CoordinateReferenceSystem crs;

    /**
     * The dimensions of the raster.
     */
    Rect dimension;

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
    
    /**
     * Metadata for this raster;
     */
    Hashtable<?, ?> metadata;

    
    public Raster(Envelope pBounds, CoordinateReferenceSystem pCrs, Rect pSize, List<Band> pBands, ByteBuffer pData, Hashtable<?, ?> pMetaData) {
    	
    	this.bounds = pBounds;
    	this.crs = pCrs;
    	this.dimension = pSize;
    	this.bands = pBands;
    	this.data = pData;
    	this.metadata = pMetaData;
    }

	/**
     * The bounds of the raster in world coordinates.
     */
    public Envelope getBoundingBox() {
        return bounds;
    }

    /**
     * Sets the bounds of the raster in world coordinates.
     */
    public void setBoundingBox(Envelope pBoundingBox) {
        this.bounds = pBoundingBox;
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
    public void setCRS(CoordinateReferenceSystem pCrs) {
        this.crs = pCrs;
    }

    /**
     * The dimensions of the raster.
     */
    public Rect getDimension() {
        return dimension;
    }

    /**
     * Sets the dimensions of the raster.
     */
    public void setDimension(Rect pSize) {
        this.dimension = pSize;
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
    public void setData(ByteBuffer pData) {
        this.data = pData;
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
    public void setBands(List<Band> pBands) {
        this.bands = pBands;
    }
    

    /**
     * The nodata for the raster.
     */
    public NoData nodata() {
        return nodata;
    }

    /**
     * Sets the nodata for the raster.
     */
    public Raster setNodata(NoData pNodata) {
        this.nodata = pNodata;
        return this;
    }
    
    /**
     * The metadata for this raster
     */
    public Hashtable<?, ?> metadata(){
    	return metadata;
    }
    
    /**
     * Sets the metadata for this raster
     */
    public void setMetadata(Hashtable<?, ?> pMetadata){
    	
    	this.metadata = pMetadata;
    }
    
    public double[] getGeoTransform(){
    	
    	if(this.bounds == null || this.dimension == null){
    		return null;
    	}
    	
    	return new double[]{
    			this.bounds.getMinX(), /* top left x */
    			this.bounds.getWidth() / this.dimension.width(), /* w-e pixel resolution */
				0,  /* 0 */
				this.bounds.getMaxY() , /* top left y */
				0, /* 0 */
				- (this.bounds.getHeight() / this.dimension.height()) /* n-s pixel resolution (negative value) */
    	};
    }

}
