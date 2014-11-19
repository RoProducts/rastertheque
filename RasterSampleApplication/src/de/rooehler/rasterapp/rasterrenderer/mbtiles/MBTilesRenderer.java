package de.rooehler.rasterapp.rasterrenderer.mbtiles;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.model.Tile;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.util.Log;
import de.rooehler.rasterapp.rasterrenderer.RasterJob;
import de.rooehler.rasterapp.rasterrenderer.RasterRenderer;
import de.rooehler.rastertheque.io.mbtiles.MBTilesRaster;
import de.rooehler.rastertheque.processing.Interpolator;

public class MBTilesRenderer implements RasterRenderer{

	private final static String TAG = MBTilesRenderer.class.getSimpleName();

	private static final int MBTILES_SIZE = 256;
	
	private GraphicFactory graphicFactory;
	
	private MbTilesDatabase db;

	private boolean isDBOpen = false;
	
	private final MBTilesRaster mRaster;

	public MBTilesRenderer(final Context pContext, GraphicFactory graphicFactory, final MBTilesRaster pRaster) {
		
		this.mRaster = pRaster;
		
		this.db = new MbTilesDatabase(pContext, mRaster.getFilePath());

		this.graphicFactory = graphicFactory;
	}

	/**
	 * called from MBTilesWorkerThread : executes a mapgeneratorJob and modifies the @param bitmap which will be the
	 * result according to the parameters inside @param mapGeneratorJob
	 */
	@Override
	public TileBitmap executeJob(RasterJob job) {

		final Tile tile = job.tile;

		final int tileSize = tile.tileSize;

		long localTileX = tile.tileX;
		long localTileY = tile.tileY;

		TileBitmap bitmap = this.graphicFactory.createTileBitmap(job.displayModel.getTileSize(), job.hasAlpha);

		// conversion needed to fit the MbTiles coordinate system
		final int[] tmsTileXY = googleTile2TmsTile(localTileX, localTileY, tile.zoomLevel);

		// Log.d(TAG, String.format("Tile requested %d %d is now %d %d", tile.tileX, tile.tileY, tmsTileXY[0],
		// tmsTileXY[1]));

		byte[] rasterBytes = null;
		android.graphics.Bitmap decodedBitmap = null;
		int[] pixels = new int[tileSize * tileSize];
		int[] mbTilesPixels = new int[MBTILES_SIZE * MBTILES_SIZE];

		rasterBytes = this.db.getTileAsBytes(String.valueOf(tmsTileXY[0]), String.valueOf(tmsTileXY[1]),
				Byte.toString(tile.zoomLevel));

		if (rasterBytes == null) {

			Log.d(TAG, String.format("Tile not available %d", tileSize));
			// got nothing,make white pixels for lower zoom levels
			for (int i = 0; i < mbTilesPixels.length; i++) {
				mbTilesPixels[i] = 0xff << 24 | (0xff << 16) | (0xff << 8) | 0xff;
			}
		} else {

			decodedBitmap = BitmapFactory.decodeByteArray(rasterBytes, 0, rasterBytes.length);

			// check if the input stream could be decoded into a bitmap
			if (decodedBitmap != null) {
				Log.d(TAG, String.format("Tile found %d", tileSize));
				// copy all pixels from the decoded bitmap to the color array
				// the MBTILES database has always 256 x256 tiles
				decodedBitmap.getPixels(mbTilesPixels, 0, MBTILES_SIZE, 0, 0, MBTILES_SIZE, MBTILES_SIZE);
				decodedBitmap.recycle();
			} else {
				for (int i = 0; i < mbTilesPixels.length; i++) {
					mbTilesPixels[i] = 0xffffffff;
				}
			}
		}

		if (tileSize != MBTILES_SIZE) {

			Interpolator.resampleBilinear(mbTilesPixels, MBTILES_SIZE, pixels, tileSize);

		} else {

			pixels = mbTilesPixels;
		}
		// copy all pixels from the color array to the tile bitmap
		bitmap.setPixels(pixels, tileSize);

		return bitmap;
	}


	@Override
	public void start() {

		if (!this.isDBOpen) {
			this.db.openDataBase();
			this.isDBOpen = true;
		}

	}
	@Override
	public void stop() {

		if (this.isDBOpen) {
			this.db.close();
			this.isDBOpen = false;
		}
	}
	@Override
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

	public static int[] googleTile2TmsTile(long tx, long ty, byte zoom) {
		return new int[] { (int) tx, (int) ((Math.pow(2, zoom) - 1) - ty) };
	}

	@Override
	public String getFilePath() {
		
		return this.mRaster.getFilePath();
		
	}

}
