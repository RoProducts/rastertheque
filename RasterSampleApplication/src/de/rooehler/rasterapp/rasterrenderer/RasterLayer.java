package de.rooehler.rasterapp.rasterrenderer;

import java.io.File;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.TileLayer;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.MapViewPosition;

import de.rooehler.rasterapp.interfaces.IWorkStatus;
import android.content.Context;
/**
 * A RasterLayer extends a Mapsforge TileLayer
 * It's workerThread handles RasterJobs to render the raster file
 * 
 * Its rasterRenderer abstracts the raster's properties
 * 
 * @author Robert Oehler
 *
 */
public class RasterLayer extends TileLayer<RasterJob>  {

	private RasterRenderer rasterRenderer;

	private RasterWorkerThread worker;

	private File rasterFile;
	
	private IWorkStatus mStatus;
	
	final int mProcCount;

	public RasterLayer(Context context, TileCache tileCache, MapViewPosition mapViewPosition, boolean isTransparent,
			GraphicFactory graphicFactory, final RasterRenderer pRasterRenderer, final IWorkStatus status) {
		super(tileCache, mapViewPosition, graphicFactory.createMatrix(), isTransparent);

		this.rasterFile = new File(pRasterRenderer.getFilePath());

		this.rasterRenderer = pRasterRenderer;
		
		this.mStatus = status;
		
		this.mProcCount = Runtime.getRuntime().availableProcessors();
	}

	@Override
	public synchronized void setDisplayModel(DisplayModel displayModel) {
		super.setDisplayModel(displayModel);
		
		if (displayModel != null) {
			this.worker = new RasterWorkerThread(this.tileCache, this.jobQueue, this.rasterRenderer, this, mStatus);
			this.worker.start();
		} else {
			// if we do not have a displayModel any more we can stop rendering.
			if (this.worker != null) {
				this.worker.interrupt();
			}
		}
	}
	
	public RasterRenderer getRasterRenderer(){
		return this.rasterRenderer;
	}

	@Override
	protected RasterJob createJob(Tile tile) {
		return new RasterJob(tile, this.displayModel, this.rasterFile, this.isTransparent);
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
