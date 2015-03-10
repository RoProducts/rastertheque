package de.rooehler.mapsforgerenderer.rasterrenderer.mbtiles;

import java.io.Serializable;
import java.util.HashMap;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.model.Tile;

import android.graphics.Rect;
import de.rooehler.mapsforgerenderer.rasterrenderer.RasterJob;
import de.rooehler.mapsforgerenderer.rasterrenderer.RasterRenderer;
import de.rooehler.rastertheque.core.DataType;
import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.core.RasterQuery;
import de.rooehler.rastertheque.io.mbtiles.MBTilesDataset;
import de.rooehler.rastertheque.io.mbtiles.MBTilesRasterQuery;
import de.rooehler.rastertheque.processing.RasterOps;
import de.rooehler.rastertheque.processing.resampling.Resampler;
import de.rooehler.rastertheque.util.Hints.Key;
/**
 * Implementation of a renderer of MBTiles for Mapsforge
 * 
 * 
 * @author Robert Oehler
 *
 */
public class MBTilesMapsforgeRenderer implements RasterRenderer{

	private GraphicFactory graphicFactory;
	
	private final MBTilesDataset mDataset;
	

	public MBTilesMapsforgeRenderer(GraphicFactory graphicFactory, final MBTilesDataset pDataset) {
		
		this.mDataset = pDataset;
		
		this.graphicFactory = graphicFactory;
	}

	/**
	 * called from RasterWorkerThread : executes a rasterJob querying the MBTilesDataset
	 * if data is returned it is resized if necessary and converted to a bitmap which is returned
	 * if no data is returned (no underlying MBTile available)
	 * a white tile is returned
	 */
	@Override
	public TileBitmap executeJob(RasterJob job) {

		final Tile tile = job.tile;

		final int tileSize = tile.tileSize;

		long localTileX = tile.tileX;
		long localTileY = tile.tileY;

		TileBitmap bitmap = this.graphicFactory.createTileBitmap(job.displayModel.getTileSize(), job.hasAlpha);

		// conversion needed to fit the MBTiles coordinate system
		final int[] tmsTileXY = mDataset.googleTile2TmsTile(localTileX, localTileY, tile.zoomLevel);

		int[] pixels = new int[tileSize * tileSize];

		//create the query
		RasterQuery query = new MBTilesRasterQuery(
				mDataset.tile2boundingBox(tile.tileX, tile.tileY, tile.zoomLevel),
				mDataset.getCRS(),
				mDataset.getBands(),
				new Rect(0,0,MBTilesDataset.MBTILES_SIZE,MBTilesDataset.MBTILES_SIZE),
				DataType.INT,
				tmsTileXY,
				tile.zoomLevel);
		
		//read
		Raster raster = mDataset.read(query);
		
		//if no data available, return tile containing white pixels
		if (raster.getData() == null) {

			for (int i = 0; i < pixels.length; i++) {
				pixels[i] = 0xff << 24 | (0xff << 16) | (0xff << 8) | 0xff;
			}
			bitmap.setPixels(pixels, tileSize);
			return bitmap;
		} 
		
		//if target tilesize is not the size of the raster
		if (tileSize != raster.getDimension().width()) {
						
			final double scale = tileSize / (double) raster.getDimension().width();
			
			HashMap<Key,Serializable> resampleParams = new HashMap<>();

			resampleParams.put(Resampler.KEY_SIZE, new Double[]{scale,scale});

			RasterOps.execute(raster, RasterOps.RESIZE, resampleParams, null, null);

		} 
		
		raster.getData().asIntBuffer().get(pixels);

		// copy all pixels from the color array to the tile bitmap
		bitmap.setPixels(pixels, tileSize);

		return bitmap;
	}


	@Override
	public void start() {
		
	}
	@Override
	public void stop() {
		
	}
	@Override
	public boolean isWorking() {

		return this.mDataset.isWorking();
	}

	@Override
	public String getFilePath() {
		
		return this.mDataset.getSource();
		
	}

	@Override
	public void destroy() {		
		this.mDataset.close();
	}

}
