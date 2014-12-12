package de.rooehler.rasterapp.rasterrenderer;

import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.queue.JobQueue;
import org.mapsforge.map.util.PausableThread;

import de.rooehler.rasterapp.interfaces.IWorkStatus;
/**
 * A RasterWorkerThread handles RasterJobs in its JobQueue
 * 
 * This follows the default Mapsforge implementation
 * 
 * @author Robert Oehler
 *
 */

public class RasterWorkerThread extends PausableThread {
	
	private final Layer layer;
	private final TileCache tileCache;
	private final RasterRenderer rasterRenderer;
	private final JobQueue<RasterJob> jobQueue;
	private final IWorkStatus mStatus;
	
	
	public RasterWorkerThread(TileCache tileCache, JobQueue<RasterJob> jobQueue, RasterRenderer pRasterRenderer, Layer layer, IWorkStatus status) {

		this.layer = layer;
		this.tileCache = tileCache;
		this.rasterRenderer = pRasterRenderer;
		this.jobQueue = jobQueue;
		this.mStatus = status;
	}

	@Override
	protected void doWork() throws InterruptedException {
		RasterJob rendererJob = this.jobQueue.get();
		try {
			if (!this.tileCache.containsKey(rendererJob)) {
		
				mStatus.isRendering();
				renderTile(rendererJob);
			}
		} finally {
			this.jobQueue.remove(rendererJob);
		}
		if(this.jobQueue.size() == 0){
			mStatus.renderingFinished();
		}
	}

	private void renderTile(RasterJob rendererJob) {
	
		TileBitmap bitmap = this.rasterRenderer.executeJob(rendererJob);

		if (!isInterrupted() && bitmap != null) {
			this.tileCache.put(rendererJob, bitmap);
			this.layer.requestRedraw();
		}
		if (bitmap != null) {
			bitmap.decrementRefCount();
		}
	}

	@Override
	protected ThreadPriority getThreadPriority() {
		return ThreadPriority.BELOW_NORMAL;
	}

	@Override
	protected boolean hasWork() {
		return true;
	}

	@Override
	public void destroy() {
		
		this.interrupt();
	}
}
