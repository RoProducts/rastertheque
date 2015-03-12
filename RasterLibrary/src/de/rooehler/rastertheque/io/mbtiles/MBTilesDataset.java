package de.rooehler.rastertheque.io.mbtiles;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import org.osgeo.proj4j.CoordinateReferenceSystem;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.util.Log;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

import de.rooehler.rastertheque.core.Band;
import de.rooehler.rastertheque.core.DataType;
import de.rooehler.rastertheque.core.Driver;
import de.rooehler.rastertheque.core.NoData;
import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.core.RasterDataset;
import de.rooehler.rastertheque.core.RasterQuery;
import de.rooehler.rastertheque.processing.rendering.ColorMap;
import de.rooehler.rastertheque.proj.Proj;
/**
 * A MbTilesDataset wraps the access to a MBTiles database
 * 
 * @author Robert Oehler
 *
 */
public class MBTilesDataset implements RasterDataset {
	
	private final static String TAG = MBTilesDataset.class.getSimpleName();
	
	public static final int MBTILES_SIZE = 256;
	
	static final String METADATA = "metadata";

	private boolean isDBOpen = false;
	
	private String mSource;
	
	private SQLiteDatabase db;
	
	private Envelope mBounds;
	
	private String mDescription;
	
	private String mName;
	
	private int[] mZoomBounds;

	
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
	 * reads the defined query from the database
	 * 
	 * A conversion from the bytes read to a bitmap of MBTiles_SIZE X MBTiles_Size
	 * is applied
	 * 
	 * the bitmaps data is saved within the raster
	 * 
	 * if no data was available the rasters data is null
	 * 
	 */
	@Override
	public Raster read(RasterQuery query) {
		
		if(query instanceof MBTilesRasterQuery){
			int[] coords =  ((MBTilesRasterQuery) query).getTileCoords();
			byte zoom    =  ((MBTilesRasterQuery) query).getZoom();

			byte[] data = getTileAsBytes(String.valueOf(coords[0]), String.valueOf(coords[1]), Byte.toString(zoom));
			
			int width = MBTILES_SIZE;
			ByteBuffer bb = null;
			if(data != null){

				Bitmap decodedBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
				int[] mbTilesPixels = new int[MBTILES_SIZE * MBTILES_SIZE];
				if (decodedBitmap != null) {
					// copy all pixels from the decoded bitmap to the color array
					// the MBTILES database has always 256 x256 tiles
					decodedBitmap.getPixels(mbTilesPixels, 0, MBTILES_SIZE, 0, 0, MBTILES_SIZE, MBTILES_SIZE);
					decodedBitmap.recycle();
				} else {
					for (int i = 0; i < mbTilesPixels.length; i++) {
						mbTilesPixels[i] = 0xffffffff;
					}
				}
				bb = ByteBuffer.allocate(mbTilesPixels.length * DataType.INT.size());
				bb.order(ByteOrder.nativeOrder());
				bb.asIntBuffer().put(mbTilesPixels);

			}
						
			return new Raster(query.getBounds(),query.getCRS(),	new Rect(0,0,width,width),query.getBands(), bb,	null);
		}
		
		return null;
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
	
	/****************************************************************
	 * 
	 * CONVERSION METHODS TILE -> ENVELOPE
	 * LAT/LON -> Tile x,y
	 * according to
	 * 
	 * http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
	 * 
	 ***************************************************************/
	
	/**
	 * According to http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames#Java
	 * @param tx
	 * @param ty
	 * @param zoom
	 * @return
	 */
	public Coordinate slippy2latlon(int tx, int ty, int zoom){

		return new Coordinate(tile2lon(tx, zoom),tile2lat(ty, zoom));
	}
	
	/**
	 * converts x coordinate and zoom level to longitude
	 * @param x
	 * @param z
	 * @return
	 */
	public double tile2lon(int x, int z) {
		return x / Math.pow(2.0, z) * 360.0 - 180;
	}
	
	/**
	 * converts y coordinate and zoom level to latitude
	 * @param y
	 * @param z
	 * @return
	 */
	public double tile2lat(int y, int z) {
		double n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, z);
		return Math.toDegrees(Math.atan(Math.sinh(n)));
	}
	
	/**
	 * converts tile coordinates to the bounds the tile covers
	 * @param x coordinate
	 * @param y coordinate
	 * @param z toom level
	 * @return the bounds of the tile
	 */
	public Envelope tile2boundingBox(final int x, final int y, final int zoom) {

		return new Envelope(
				tile2lon(x + 1, zoom),
				tile2lon(x, zoom),
				tile2lat(y + 1, zoom),
				tile2lat(y, zoom)
				);
	}
	/**
	 * converts longitude and zoom level to a slippy tile x coordinate
	 * @param lat
	 * @param z
	 * @return
	 */
	public int long2tilex(double lon, int z) 
	{ 
		return (int)(Math.floor((lon + 180.0) / 360.0 * Math.pow(2.0, z))); 
	}
	
	/**
	 * converts latitude and zoom level to a slippy tile y coordinate
	 * @param lat
	 * @param z
	 * @return
	 */
	public int lat2tiley(double lat, int z)
	{ 
		return (int)(Math.floor((1.0 - Math.log( Math.tan(lat * Math.PI/180.0) + 1.0 / Math.cos(lat * Math.PI/180.0)) / Math.PI) / 2.0 * Math.pow(2.0, z))); 
	}
	
	/**
	 * MBTiles are always in Google Mercator
	 */
	@Override
	public CoordinateReferenceSystem getCRS() {
		
		 return Proj.EPSG_900913;
	}

	/**
	 * returns the bounds of this dataset
	 */
	@Override
	public Envelope getBoundingBox() {

		if(mBounds == null){
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

				mBounds = new  Envelope(minlon, maxlon, minlat, maxlat);

			} catch (NullPointerException e) {
				Log.e(TAG, "NPE retrieving boundingbox from db", e);
				return null;
			}
		}
		
		return mBounds;
		
	}

	/**
	 * accesses the databases metadata
	 * to retrieve min and max zoom of this 
	 * mbtiles dataset
	 * @return an array in the form int[]{min,max}
	 */
	public int[] getMinMaxZoom(){
		
		if(mZoomBounds == null){

			mZoomBounds = new int[2];
			try {
				Cursor c = this.db.rawQuery("select value from metadata where name=?", new String[] { "minzoom" });
				if (!c.moveToFirst()) {
					c.close();
					return null;
				}
				mZoomBounds[0] = c.getInt(c.getColumnIndex("value"));
				c.close();
				c = this.db.rawQuery("select value from metadata where name=?", new String[] { "maxzoom" });

				if (!c.moveToFirst()) {
					c.close();
					return null;
				}
				mZoomBounds[1] = c.getInt(c.getColumnIndex("value"));

				c.close();

			} catch (NullPointerException e) {
				Log.e(TAG, "NPE retrieving boundingbox from db", e);
				return null;
			}
		}
		return mZoomBounds;
	}

	@Override
	public Driver getDriver() {
		
		return new MBTilesDriver();
	}

	@Override
	public String getName() {
		
		if(mName == null){

			android.database.Cursor c = db.query(METADATA, new String[]{"value"}, "name = ?", new String[]{"name"}, null, null, null);
			try {
				if (c.moveToNext()) {
					mName =  c.getString(0);
				}
			}
			finally {
				c.close();
			}
		}
		return mName;
	}

	@Override
	public String getDescription() {

		if(mDescription == null){

			android.database.Cursor c = db.query(METADATA, new String[]{"value"}, "name = ?", new String[]{"description"}, null, null, null);
			try {
				if (c.moveToNext()) {
					mDescription = c.getString(0);
				}
			}
			finally {
				c.close();
			}
		}
		return mDescription;
	}

	@Override
	public String getSource() {
		return mSource;
	}

	@Override
	public Rect getDimension() {

		return new Rect(0,0,MBTILES_SIZE,MBTILES_SIZE);
	}

	@Override
	public List<Band> getBands() {

		List<Band> bands = new ArrayList<Band>();
		
		Band b = new Band() {
			
			@Override
			public NoData nodata() {
				return NoData.NONE;
			}
			
			@Override
			public String name() {
				return "MBTiles band";
			}
			
			@Override
			public DataType datatype() {
				return DataType.INT;
			}
			
			@Override
			public ColorMap colorMap() {
				return null;
			}
			
			@Override
			public Color color() {
				return Color.OTHER;
			}
		};
		bands.add(b);
		
		return bands;
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
	private byte[] getTileAsBytes(String x, String y, String z) {
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
}
