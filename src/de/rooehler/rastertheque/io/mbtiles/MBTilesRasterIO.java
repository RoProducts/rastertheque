package de.rooehler.rastertheque.io.mbtiles;

import android.content.Context;
import de.rooehler.rastertheque.core.Raster;

public class MBTilesRasterIO extends Raster {
	
	private MbTilesDatabase db;

	private boolean isDBOpen = false;
	
	private Context mContext;
	
	public MBTilesRasterIO(final Context pContext,final String pFilePath){
		
		super(pFilePath);
		
		this.db = new MbTilesDatabase(mContext, mFilePath);
		
		this.mContext  = pContext;
	}
	
	public MbTilesDatabase getDB(){
		
		return this.db;
	}


	@Override
	public int getMinZoom() {
		return 10;
	}

	@Override
	public int getMaxZoom() {
		return 16;
	}
	
	public void start() {

		if (!this.isDBOpen) {
			this.db.openDataBase();
			this.isDBOpen = true;
		}

	}

	public void stop() {

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
			stop();
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



}
