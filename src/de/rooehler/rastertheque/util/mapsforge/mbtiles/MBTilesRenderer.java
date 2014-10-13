/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.rooehler.rastertheque.util.mapsforge.mbtiles;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.model.Tile;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.util.Log;

public class MBTilesRenderer {

	private final static String TAG = MBTilesRenderer.class.getSimpleName();

	private static final int MBTILES_SIZE = 256;

	private MbTilesDatabase db;

	private boolean isDBOpen = false;

	private GraphicFactory graphicFactory;

	public MBTilesRenderer(final Context pContext, GraphicFactory graphicFactory, final String pDBPath) {

		this.db = new MbTilesDatabase(pContext, pDBPath);

		this.graphicFactory = graphicFactory;

	}

	/**
	 * called from MBTilesWorkerThread : executes a mapgeneratorJob and modifies the @param bitmap which will be the
	 * result according to the parameters inside @param mapGeneratorJob
	 */
	public TileBitmap executeJob(MBTilesJob job) {

		final Tile tile = job.tile;

		final int tileSize = job.tileSize;

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

			resizeBilinear(mbTilesPixels, MBTILES_SIZE, pixels, tileSize);

		} else {

			pixels = mbTilesPixels;
		}
		// copy all pixels from the color array to the tile bitmap
		bitmap.setPixels(pixels, tileSize);

		return bitmap;
	}

	/**
	 * Bilinear interpolation http://en.wikipedia.org/wiki/Bilinear_interpolation
	 * 
	 * @param srcPixels
	 * @param srcSize
	 * @param dstPixels
	 * @param dstSize
	 */
	public void resizeBilinear(int srcPixels[], int srcSize, int dstPixels[], int dstSize) {

		int a, b, c, d, x, y, index;
		float x_ratio = ((float) (srcSize - 1)) / dstSize;
		float y_ratio = ((float) (srcSize - 1)) / dstSize;
		float x_diff, y_diff, blue, red, green;
		int offset = 0;

		for (int i = 0; i < dstSize; i++) {
			for (int j = 0; j < dstSize; j++) {

				// src pix coords
				x = (int) (x_ratio * j);
				y = (int) (y_ratio * i);

				// offsets from the current pos to the pos in the new array
				x_diff = (x_ratio * j) - x;
				y_diff = (y_ratio * i) - y;

				// current pos
				index = (y * srcSize + x);

				a = srcPixels[index];
				b = srcPixels[index + 1];
				c = srcPixels[index + srcSize];
				d = srcPixels[index + srcSize + 1];

				// having the four pixels, interpolate

				// blue element
				// Yb = Ab(1-w)(1-h) + Bb(w)(1-h) + Cb(h)(1-w) + Db(wh)
				blue = (a & 0xff) * (1 - x_diff) * (1 - y_diff) + (b & 0xff) * (x_diff) * (1 - y_diff) + (c & 0xff)
						* (y_diff) * (1 - x_diff) + (d & 0xff) * (x_diff * y_diff);

				// green element
				// Yg = Ag(1-w)(1-h) + Bg(w)(1-h) + Cg(h)(1-w) + Dg(wh)
				green = ((a >> 8) & 0xff) * (1 - x_diff) * (1 - y_diff) + ((b >> 8) & 0xff) * (x_diff) * (1 - y_diff)
						+ ((c >> 8) & 0xff) * (y_diff) * (1 - x_diff) + ((d >> 8) & 0xff) * (x_diff * y_diff);

				// red element
				// Yr = Ar(1-w)(1-h) + Br(w)(1-h) + Cr(h)(1-w) + Dr(wh)
				red = ((a >> 16) & 0xff) * (1 - x_diff) * (1 - y_diff) + ((b >> 16) & 0xff) * (x_diff) * (1 - y_diff)
						+ ((c >> 16) & 0xff) * (y_diff) * (1 - x_diff) + ((d >> 16) & 0xff) * (x_diff * y_diff);

				dstPixels[offset++] = 0xff000000 | ((((int) red) << 16) & 0xff0000) | ((((int) green) << 8) & 0xff00)
						| ((int) blue);
			}
		}
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

	public static int[] googleTile2TmsTile(long tx, long ty, byte zoom) {
		return new int[] { (int) tx, (int) ((Math.pow(2, zoom) - 1) - ty) };
	}

}
