package de.rooehler.rastertheque.io.mbtiles;

import java.util.List;

import org.osgeo.proj4j.CoordinateReferenceSystem;

import android.graphics.Rect;

import com.vividsolutions.jts.geom.Envelope;

import de.rooehler.rastertheque.core.Band;
import de.rooehler.rastertheque.core.DataType;
import de.rooehler.rastertheque.core.RasterQuery;

/**
 * A MBTilesRasterQuery extends a rasterquery by the x,y and z coordinates
 * which are used to extract a MBTile from its database
 * 
 * @author Robert Oehler
 *
 */

public class MBTilesRasterQuery extends RasterQuery{
	
	private int[] tileCoords;
	
	private byte zoom;
	
	public MBTilesRasterQuery(Envelope pBounds,
			CoordinateReferenceSystem  pCrs,
			List<Band> pBands,
			Rect pDimension,
			DataType pDatatype,
			int[] pTileCoords,
			byte pZoom) {
		super(pBounds, pCrs, pBands, pDimension, pDatatype);
		
		this.tileCoords = pTileCoords;
		this.zoom = pZoom;
	}

	/**
	 * @return the tileCoords
	 */
	public int[] getTileCoords() {
		return tileCoords;
	}
	/**
	 * return the zoom
	 * @return
	 */
	public byte getZoom() {
		return zoom;
	}


}
