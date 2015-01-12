package de.rooehler.rastertheque.io.mbtiles;

import org.osgeo.proj4j.CoordinateReferenceSystem;

import android.content.Context;
import de.rooehler.rastertheque.core.BoundingBox;
import de.rooehler.rastertheque.core.Dataset;
import de.rooehler.rastertheque.core.Driver;
import de.rooehler.rastertheque.proj.Proj;

public class MBTilesDataset implements Dataset {
	
	private MBTilesDatabase db;

	private boolean isDBOpen = false;
	
	private Context mContext;
	
	private String mSource;

	
	public MBTilesDataset(final Context pContext,final String pFilePath){
		
		this.mSource = pFilePath;
		
		this.db = new MBTilesDatabase(mContext, mSource);
		
		this.mContext  = pContext;
	}
	
	public MBTilesDatabase getDB(){
		
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
	public CoordinateReferenceSystem getCRS() {
		
		 return Proj.EPSG_900913;
	}

	@Override
	public BoundingBox getBoundingBox() {
		start();
		final BoundingBox bb = db.getBoundingBox();
		close();
		return bb;
	}
	
	public int[] getMinMaxZoom(){
		
		start();
		final int[] minmax = db.getMinMaxZoom();
		close();
		return minmax;
	}

	@Override
	public Driver<?> getDriver() {
		
		return new MBTilesDriver(mContext);
	}

	@Override
	public String getName() {
		
		android.database.Cursor c = db.queryMetadata("name");
        try {
            if (c.moveToNext()) {
                return c.getString(0);
            }
        }
        finally {
            c.close();
        }

        return null;
	}

	@Override
	public String getDescription() {
		  android.database.Cursor c = db.queryMetadata("description");
	        try {
	            if (c.moveToNext()) {
	                return c.getString(0);
	            }
	        }
	        finally {
	            c.close();
	        }

	        return null;
	}

	@Override
	public String getSource() {
		return mSource;
	}
}
