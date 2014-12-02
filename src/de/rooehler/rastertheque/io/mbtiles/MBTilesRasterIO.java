package de.rooehler.rastertheque.io.mbtiles;

import org.osgeo.proj4j.CoordinateReferenceSystem;

import android.content.Context;

import com.vividsolutions.jts.geom.Envelope;

import de.rooehler.rastertheque.core.RasterDataSet;
import de.rooehler.rastertheque.proj.Proj;

public class MBTilesRasterIO implements RasterDataSet {
	
	private MbTilesDatabase db;

	private boolean isDBOpen = false;
	
	private Context mContext;
	
	private String mSource;
	

	
	public MBTilesRasterIO(final Context pContext,final String pFilePath){
		
		this.mSource = pFilePath;
		
		this.db = new MbTilesDatabase(mContext, mSource);
		
		this.mContext  = pContext;
	}
	
	public MbTilesDatabase getDB(){
		
		return this.db;
	}
	
	public void start() {

		if (!this.isDBOpen) {
			this.db.openDataBase();
			this.isDBOpen = true;
		}

	}
	@Override
	public void close() {

		if (this.isDBOpen) {
			this.db.close();
			this.isDBOpen = false;
		}
	}

	public boolean isWorking() {

		return this.isDBOpen;
	}


	/**
	 * closes and destroys any resources needed
	 */
	public void destroy() {

		if (this.db != null) {
			close();
			this.db = null;
		}
	}
	/**
	 * Converts Google tile coordinates to TMS Tile coordinates.
	 * <p>
	 * Code copied from: http://code.google.com/p/gmap-tile-generator/
	 * </p>
	 * 
	 * @param tx
	 *            the x tile number.
	 * @param ty
	 *            the y tile number.
	 * @param zoom
	 *            the current zoom level.
	 * @return the converted values.
	 */

	public int[] googleTile2TmsTile(long tx, long ty, byte zoom) {
		return new int[] { (int) tx, (int) ((Math.pow(2, zoom) - 1) - ty) };
	}

	@Override
	public String getSource() {
		
		return mSource;
	}

	@Override
	public CoordinateReferenceSystem getCRS() {
		
		 return Proj.EPSG_900913;
	}

	@Override
	public Envelope getEnvelope() {

		return db.getEnvelope();
	}



}
