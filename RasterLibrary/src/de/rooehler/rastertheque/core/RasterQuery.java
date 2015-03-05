package de.rooehler.rastertheque.core;

import java.util.List;

import org.osgeo.proj4j.CoordinateReferenceSystem;

import android.graphics.Rect;

import com.vividsolutions.jts.geom.Envelope;
/**
 * A RasterQuery specifies what to read from a 
 * raster dataset
 * 
 * @author Robert Oehler
 *
 */
public class RasterQuery {
	
    /**
     * spatial extent of query.
     */
    Envelope bounds;

    /**
     * projection of query.
     */
    CoordinateReferenceSystem crs;

    /**
     * band selection
     */
    List<Band> bands;

    /**
     * Target size for the raster.
     */
    Rect dimension;

    /**
     * Data type that band values should be packed into.
     */
    DataType datatype; 
    
    
    public RasterQuery(
    		final Envelope pBounds,
    		final CoordinateReferenceSystem  pCrs,
    		final List<Band> pBands,
    		final Rect pDimension,
    		final DataType pDatatype) {
    	
		this.bounds = pBounds;
		this.crs = pCrs;
		this.bands = pBands;
		this.dimension = pDimension;
		this.datatype = pDatatype;
	}

	/**
     * Sets the bands to read from the raster dataset
     * @param bands Band indexes (0 based).
     */
    public void setBands(List<Band> bands) {
        this.bands = bands;
    }


    /**
     * The bands to read from the raster dataset
     */
    public List<Band> getBands() {
        return bands;
    }

    /**
     * Sets the bounding box of the query in model coordinates
     * <p>
     * The bounds should be interpreted in terms of {@link #crs()}. If no crs has been
     * set the bounds should be interpreted in terms of the native crs of the data being
     * queried.
     * </p>
     * @param bounds the query bounds
     */
    public RasterQuery setBounds(Envelope bounds) {
        this.bounds = bounds;
        return this;
    }

    /**
     * The bounding box of the query in model coordinates
     */
    public Envelope getBounds() {
        return bounds;
    }

    /**
     * Sets the crs of the query.
     */
    public void setCRS(CoordinateReferenceSystem  crs) {
        this.crs = crs;
    }

    /**
     * The query crs
     *
     * @see #crs(org.osgeo.proj4j.CoordinateReferenceSystem)
     */
    public CoordinateReferenceSystem getCRS() {
        return crs;
    }

    /**
     * Sets the dimension of the raster
     *
     * @param raster dimension
     */
    public void setDimension(Rect pDimension) {
        this.dimension = pDimension;
    }

    /**
     * the dimension of the raster in raster coordinates
     */
    public Rect getDimension() {
        return dimension;
    }

    /**
     * Sets the data type of the raster
     *
     * @param datatype the data type.
     */
    public void setDatatype(DataType datatype) {
        this.datatype = datatype;
    }

    /**
     * The type of the returned buffer from a raster query.
     * <p>
     * Raster datasets are expected to pack data from all bands being
     * query into a single buffer element of this type.
     * </p>
     */
    public DataType getDataType() {
        return datatype;
    }

}
