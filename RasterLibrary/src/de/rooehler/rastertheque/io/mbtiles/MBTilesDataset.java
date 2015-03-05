package de.rooehler.rastertheque.io.mbtiles;

import java.io.File;

import org.osgeo.proj4j.CoordinateReferenceSystem;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.vividsolutions.jts.geom.Envelope;

import de.rooehler.rastertheque.core.Dataset;
import de.rooehler.rastertheque.core.Driver;
import de.rooehler.rastertheque.proj.Proj;
/**
 * A MbTilesDataset wraps the access to a MBTiles database
 * 
 * @author Robert Oehler
 *
 */
public class MBTilesDataset implements Dataset {
	
	private final static String TAG = MBTilesDataset.class.getSimpleName();
	
	static final String METADATA = "metadata";

	private boolean isDBOpen = false;
	
	private String mSource;
	
	private SQLiteDatabase db;

	
	public MBTilesDataset(final String pFilePath){
		
		this.mSource = pFilePath;
		
		this.db = SQLiteDatabase.openOrCreateDatabase(new File(pFilePath), null);
		 
		this.isDBOpen = true;
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

	/**
	 * MBTiles are always in Google Mercator
	 */
	@Override
	public CoordinateReferenceSystem getCRS() {
		
		 return Proj.EPSG_900913;
	}

	@Override
	public Envelope getBoundingBox() {
		
		try {
			final Cursor c = this.db.rawQuery("select value from metadata where name=?", new String[] { "bounds" });
			if (!c.moveToFirst()) {
				c.close();
				return null;
			}
			final String box = c.getString(c.getColumnIndex("value"));

			String[] split = box.split(",");
			if (split.length != 4) {
				return null;
			}
			double minlon = Double.parseDouble(split[0]);
			double minlat = Double.parseDouble(split[1]);
			double maxlon = Double.parseDouble(split[2]);
			double maxlat = Double.parseDouble(split[3]);
			c.close();

			return new Envelope(minlon, maxlon, minlat, maxlat);

		} catch (NullPointerException e) {
			Log.e(TAG, "NPE retrieving boundingbox from db", e);
			return null;
		}
		
	}
	
	/**
	 * queries the database for the data of an raster image
	 * 
	 * @param x
	 *            the x coordinate
	 * @param y
	 *            the y coordinate
	 * @param z
	 *            the z coordinate
	 * @return the data, if available for these coordinates
	 */
	public byte[] getTileAsBytes(String x, String y, String z) {
		try {
			final Cursor c = this.db.rawQuery(
					"select tile_data from tiles where tile_column=? and tile_row=? and zoom_level=?", new String[] {
							x, y, z });
			if (!c.moveToFirst()) {
				c.close();
				return null;
			}
			byte[] bb = c.getBlob(c.getColumnIndex("tile_data"));

			c.close();
			
			return bb;
		} catch (NullPointerException e) {
			Log.e(TAG, "NPE getTileAsBytes", e);
			return null;
		} catch (SQLiteException e) {
			Log.e(TAG, "SQLiteException getTileAsBytes", e);
			return null;
		}
	}
	/**
	 * accesses the databases metadata
	 * to retrieve min and max zoom of this 
	 * mbtiles dataset
	 * @return an array in the form int[]{min,max}
	 */
	public int[] getMinMaxZoom(){
		
		
		int[] zoomValues = new int[2];
		try {
			Cursor c = this.db.rawQuery("select value from metadata where name=?", new String[] { "minzoom" });
			if (!c.moveToFirst()) {
				c.close();
				return null;
			}
			zoomValues[0] = c.getInt(c.getColumnIndex("value"));
			c.close();
			c = this.db.rawQuery("select value from metadata where name=?", new String[] { "maxzoom" });

			if (!c.moveToFirst()) {
				c.close();
				return null;
			}
			zoomValues[1] = c.getInt(c.getColumnIndex("value"));

			c.close();

			return zoomValues;

		} catch (NullPointerException e) {
			Log.e(TAG, "NPE retrieving boundingbox from db", e);
			return null;
		}
	}

	@Override
	public Driver getDriver() {
		
		return new MBTilesDriver();
	}

	@Override
	public String getName() {
		
		 android.database.Cursor c = db.query(METADATA, new String[]{"value"}, "name = ?", new String[]{"name"}, null, null, null);
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
		 android.database.Cursor c = db.query(METADATA, new String[]{"value"}, "name = ?", new String[]{"description"}, null, null, null);
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
