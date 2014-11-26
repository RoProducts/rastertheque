package de.rooehler.rasterapp.rasterrenderer.mbtiles;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.model.Tile;

import android.graphics.BitmapFactory;
import de.rooehler.rasterapp.rasterrenderer.RasterJob;
import de.rooehler.rasterapp.rasterrenderer.RasterRenderer;
import de.rooehler.rastertheque.io.mbtiles.MBTilesRasterIO;
import de.rooehler.rastertheque.processing.resampling.MBilinearInterpolator;

public class MBTilesMapsforgeRenderer implements RasterRenderer{

	private final static String TAG = MBTilesMapsforgeRenderer.class.getSimpleName();

	private static final int MBTILES_SIZE = 256;
	
	private GraphicFactory graphicFactory;
	
	private final MBTilesRasterIO mRaster;

	public MBTilesMapsforgeRenderer(GraphicFactory graphicFactory, final MBTilesRasterIO pRaster) {
		
		this.mRaster = pRaster;
		
		this.graphicFactory = graphicFactory;
	}

	/**
	 * called from RasterWorkerThread : executes a rasterJob extracting data
	 * from the db and returns the database tile if available otherwise a white tile
	 */
	@Override
	public TileBitmap executeJob(RasterJob job) {

		final Tile tile = job.tile;

		final int tileSize = tile.tileSize;

		long localTileX = tile.tileX;
		long localTileY = tile.tileY;

		TileBitmap bitmap = this.graphicFactory.createTileBitmap(job.displayModel.getTileSize(), job.hasAlpha);

		// conversion needed to fit the MbTiles coordinate system
		final int[] tmsTileXY = mRaster.googleTile2TmsTile(localTileX, localTileY, tile.zoomLevel);

		byte[] rasterBytes = null;
		android.graphics.Bitmap decodedBitmap = null;
		int[] pixels = new int[tileSize * tileSize];
		int[] mbTilesPixels = new int[MBTILES_SIZE * MBTILES_SIZE];

		rasterBytes = mRaster.getDB().getTileAsBytes(String.valueOf(tmsTileXY[0]), String.valueOf(tmsTileXY[1]), Byte.toString(tile.zoomLevel));

		if (rasterBytes == null) {

			// got nothing,make white pixels for lower zoom levels
			for (int i = 0; i < mbTilesPixels.length; i++) {
				mbTilesPixels[i] = 0xff << 24 | (0xff << 16) | (0xff << 8) | 0xff;
			}
		} else {

			decodedBitmap = BitmapFactory.decodeByteArray(rasterBytes, 0, rasterBytes.length);

			// check if the input stream could be decoded into a bitmap
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
		}

		if (tileSize != MBTILES_SIZE) {

			MBilinearInterpolator.resampleBilinear(mbTilesPixels, MBTILES_SIZE, pixels, tileSize);

		} else {

			pixels = mbTilesPixels;
		}
		// copy all pixels from the color array to the tile bitmap
		bitmap.setPixels(pixels, tileSize);

		return bitmap;
	}


	@Override
	public void start() {

		this.mRaster.start();

	}
	@Override
	public void stop() {

		this.mRaster.stop();
	}
	@Override
	public boolean isWorking() {

		return this.mRaster.isWorking();
	}

	@Override
	public String getFilePath() {
		
		return this.mRaster.getSource();
		
	}

	@Override
	public void destroy() {
		
		this.mRaster.destroy();
		
	}

}
