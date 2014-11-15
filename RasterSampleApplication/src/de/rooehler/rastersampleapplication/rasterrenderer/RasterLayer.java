package de.rooehler.rastersampleapplication.rasterrenderer;

import java.io.File;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.TileLayer;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.MapViewPosition;

import android.content.Context;

public class RasterLayer extends TileLayer<RasterJob>  {

	private RasterRenderer rasterRenderer;

	private RasterWorkerThread worker;

	private File mapFile;

	public RasterLayer(Context context, TileCache tileCache, MapViewPosition mapViewPosition, boolean isTransparent,
			GraphicFactory graphicFactory, final RasterRenderer pRasterRenderer) {
		super(tileCache, mapViewPosition, graphicFactory.createMatrix(), isTransparent);

		this.mapFile = new File(pRasterRenderer.getFilePath());

		this.rasterRenderer = pRasterRenderer;
	}

	@Override
	public synchronized void setDisplayModel(DisplayModel displayModel) {
		super.setDisplayModel(displayModel);
		if (displayModel != null) {
			this.worker = new RasterWorkerThread(this.tileCache, this.jobQueue, this.rasterRenderer, this);
			this.worker.start();
		} else {
			// if we do not have a displayModel any more we can stop rendering.
			if (this.worker != null) {
				this.worker.interrupt();
			}
		}
	}

	@Override
	protected RasterJob createJob(Tile tile) {
		return new RasterJob(tile, this.displayModel, this.mapFile, this.isTransparent);
	}

	@Override
	protected void onAdd() {

		this.rasterRenderer.start();
		this.worker.proceed();
		super.onAdd();
	}

	@Override
	protected void onRemove() {

		this.rasterRenderer.stop();
		this.worker.pause();
		super.onRemove();
	}

	@Override
	public void onDestroy() {
		this.rasterRenderer.destroy();
		this.worker.destroy();
		super.onDestroy();
	}
}
