package de.rooehler.rastertheque.io.gdal;

import java.util.List;

import org.osgeo.proj4j.CoordinateReferenceSystem;

import android.graphics.Rect;

import com.vividsolutions.jts.geom.Envelope;

import de.rooehler.rastertheque.core.Band;
import de.rooehler.rastertheque.core.DataType;
import de.rooehler.rastertheque.core.RasterQuery;

/**
 * A GDALRasterQuery extends a RasterQuery adding a
 * targetDimension with which it is possible
 * to let GDAL resample the query automatically
 * 
 * this is especially interesting when a very large raster
 * should be read and shrinked significantly
 * 
 * instead of allocating [large raster * large raster]
 * which may lead to an OutOfMemoryError easily
 * GDAL will resample the query and it is not necessary to 
 * allocate the read size but only the target size
 * 
 * @author Robert Oehler
 *
 */
public class GDALRasterQuery extends RasterQuery {
	
    /**
     * optional target dimension
     */
	Rect targetDimension;

	public GDALRasterQuery(Envelope pBounds,
			CoordinateReferenceSystem  pCrs,
			List<Band> pBands,
			Rect pDimension,
			DataType pDatatype,
			Rect pTargetDimension) {
		super(pBounds, pCrs, pBands, pDimension, pDatatype);
		
		this.targetDimension = pTargetDimension;
	}
	
	
    /**
     * the optional target dimension what will be used during GDAL reads
     * to apply an direct rescaling operation
     */
    public Rect getTargetDimension() {
		return targetDimension;
	}
    /**
     * sets the target dimension for GDAL reads
     */
    public void setTargetDimension(Rect targetDimension) {
		this.targetDimension = targetDimension;
	}

}
