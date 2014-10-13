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
package de.rooehler.rastertheque.util.mapsforge.raster;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Tile;

import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.util.Log;

public class RasterFileRenderer {

	private final static String TAG = RasterFileRenderer.class.getSimpleName();

	private GraphicFactory graphicFactory;
	
	private BBDataSet dataSet;

	private boolean isWorking = false;

	public RasterFileRenderer(GraphicFactory graphicFactory, final BBDataSet pDataset) {

		this.graphicFactory = graphicFactory;

		this.dataSet = pDataset;
	}

	/**
	 * called from RasterFileWorkerThread : executes a mapgeneratorJob and modifies the @param bitmap which will be the
	 * result according to the parameters inside @param mapGeneratorJob
	 */
	public TileBitmap executeJob(RasterFileJob job) {

		final Tile tile = job.tile;

		final int tileSize = job.tileSize;

		long localTileX = tile.tileX;
		long localTileY = tile.tileY;

		TileBitmap bitmap = this.graphicFactory.createTileBitmap(job.displayModel.getTileSize(), job.hasAlpha);

		// TODO correct implementation
		
		
        // area of raster to load
        Rect r = RasterHelper.getRect(this.dataSet);  // raster space
        BoundingBox bbox = dataSet.getBB();   // world space
       
        // figure out the buffer type if not specified
        DataType datatype = DataType.BYTE;
        
        ByteBuffer buffer = ByteBuffer.allocateDirect(tileSize*tileSize*datatype.size());
        
        buffer.order(ByteOrder.nativeOrder());
        
        int[] bands = new int[dataSet.GetRasterCount()];
        
            // single band, read in same units as requested buffer
        dataSet.ReadRaster_Direct(r.left, r.top, r.width(), r.height(), tileSize, tileSize,RasterHelper.toGDAL(datatype), buffer, bands, 0, 0, 0);
       
		

//		byte[] rasterBytes = null;
//		android.graphics.Bitmap decodedBitmap = null;
//		int[] pixels = new int[tileSize * tileSize];
//		int[] mbTilesPixels = new int[MBTILES_SIZE * MBTILES_SIZE];
//
//		rasterBytes = this.db.getTileAsBytes(String.valueOf(tmsTileXY[0]), String.valueOf(tmsTileXY[1]),
//				Byte.toString(tile.zoomLevel));
//
//		if (rasterBytes == null) {
//
//			Log.d(TAG, String.format("Tile not available %d", tileSize));
//			// got nothing,make white pixels for lower zoom levels
//			for (int i = 0; i < mbTilesPixels.length; i++) {
//				mbTilesPixels[i] = 0xff << 24 | (0xff << 16) | (0xff << 8) | 0xff;
//			}
//		} else {
//
//			decodedBitmap = BitmapFactory.decodeByteArray(rasterBytes, 0, rasterBytes.length);
//
//			// check if the input stream could be decoded into a bitmap
//			if (decodedBitmap != null) {
//				Log.d(TAG, String.format("Tile found %d", tileSize));
//				// copy all pixels from the decoded bitmap to the color array
//				// the MBTILES database has always 256 x256 tiles
//				decodedBitmap.getPixels(mbTilesPixels, 0, MBTILES_SIZE, 0, 0, MBTILES_SIZE, MBTILES_SIZE);
//				decodedBitmap.recycle();
//			} else {
//				for (int i = 0; i < mbTilesPixels.length; i++) {
//					mbTilesPixels[i] = 0xffffffff;
//				}
//			}
//		}
//
//		if (tileSize != MBTILES_SIZE) {
//
//			resizeBilinear(mbTilesPixels, MBTILES_SIZE, pixels, tileSize);
//
//		} else {
//
//			pixels = mbTilesPixels;
//		}
//		// copy all pixels from the color array to the tile bitmap
//		bitmap.setPixels(pixels, tileSize);

		return bitmap;
	}

	public void start() {

		this.isWorking = true;

	}

	public void stop() {

		this.isWorking = false;

	}

	public boolean isWorking() {

		return this.isWorking;
	}

	/**
	 * closes and destroys any resources needed
	 */
	public void destroy() {

		stop();

	}

}
