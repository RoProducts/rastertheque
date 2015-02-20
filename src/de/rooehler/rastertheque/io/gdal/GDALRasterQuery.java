package de.rooehler.rastertheque.io.gdal;

import java.util.List;

import org.gdal.osr.SpatialReference;

import android.graphics.Rect;

import com.vividsolutions.jts.geom.Envelope;

import de.rooehler.rastertheque.core.Band;
import de.rooehler.rastertheque.core.DataType;
import de.rooehler.rastertheque.core.RasterQuery;

public class GDALRasterQuery extends RasterQuery {
	
    /**
     * optional target dimension
     */
	Rect targetDimension;

	public GDALRasterQuery(Envelope pBounds,
			SpatialReference pCrs,
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
