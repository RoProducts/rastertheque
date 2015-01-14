package de.rooehler.rastertheque.core;

import java.util.List;

import org.osgeo.proj4j.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Envelope;

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
    Envelope size;

    /**
     * Data type that band values should be packed into.
     */
    DataType datatype;
    
    
    
    public RasterQuery(Envelope bounds, CoordinateReferenceSystem crs,	List<Band> bands, Envelope size, DataType datatype) {
		this.bounds = bounds;
		this.crs = crs;
		this.bands = bands;
		this.size = size;
		this.datatype = datatype;
	}

	/**
     * Sets the bands to read from the raster dataset.
     * <p>
     * Specifying <tt>null</tt> or an empty array means all bands.
     * </p>
     * @param bands Band indexes (0 based).
     *
     * @return This object.
     */
    public void setBands(List<Band> bands) {
        this.bands = bands;
    }


    /**
     * The bands to read from the raster dataset.
     */
    public List<Band> getBands() {
        return bands;
    }

    /**
     * Sets the bounds constraint of the query.
     * <p>
     * The bounds should be interpreted in terms of {@link #crs()}. If no crs has been
     * set the bounds should be interpreted in terms of the native crs of the data being
     * queried.
     * </p>
     * @param bounds The query bounds, specifying <tt>null</tt> means the bounds of the entire dataset.
     * @return This object.
     */
    public RasterQuery setBounds(Envelope bounds) {
        this.bounds = bounds;
        return this;
    }

    /**
     * Bounds constraints on the query, may be <code>null</code> meaning no bounds constraint.
     *
     * @see #bounds(com.vividsolutions.jts.geom.Envelope)
     */
    public Envelope getBounds() {
        return bounds;
    }

    /**
     * Sets the crs of the query.
     * <p>
     * Datasets handling queries must check this relative to the native crs to determine if
     * re-projection must occur.
     * </p>
     */
    public void setCRS(CoordinateReferenceSystem crs) {
        this.crs = crs;
    }

    /**
     * The query crs, may be <code>null</code> meaning same crs as the data being queried.
     *
     * @see #crs(org.osgeo.proj4j.CoordinateReferenceSystem)
     */
    public CoordinateReferenceSystem getCRS() {
        return crs;
    }

    /**
     * Sets the target size of the raster to be read.
     *
     * @param size Raster dimensions.
     */
    public void setSize(Envelope size) {
        this.size = size;
    }

    /**
     * Target size for the raster being read.
     */
    public Envelope getSize() {
        return size;
    }

    /**
     * Sets the type of the returned buffer from a raster query.
     *
     * @param datatype The buffer type.
     *
     * @return This object.
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
