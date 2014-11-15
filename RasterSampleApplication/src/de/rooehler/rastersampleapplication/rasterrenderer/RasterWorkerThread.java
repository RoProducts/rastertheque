package de.rooehler.rastersampleapplication.rasterrenderer;

import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.queue.JobQueue;
import org.mapsforge.map.layer.renderer.MapWorker;
import org.mapsforge.map.util.PausableThread;


public class RasterWorkerThread extends PausableThread {
	
	private static final boolean DEBUG_TIMING = false;
	private static final Logger LOGGER = Logger.getLogger(MapWorker.class.getName());
	private final AtomicLong totalExecutions;
	// for timing only
	private final AtomicLong totalTime;

	private final Layer layer;
	private final TileCache tileCache;
	private final RasterRenderer rasterRenderer;
	private final JobQueue<RasterJob> jobQueue;
	
	public RasterWorkerThread(TileCache tileCache, JobQueue<RasterJob> jobQueue, RasterRenderer pRasterRenderer,
			Layer layer) {

		if (DEBUG_TIMING) {
			totalTime = new AtomicLong();
			totalExecutions = new AtomicLong();
		} else {
			totalTime = null;
			totalExecutions = null;
		}

		this.layer = layer;
		this.tileCache = tileCache;
		this.rasterRenderer = pRasterRenderer;
		this.jobQueue = jobQueue;
	}

	@Override
	protected void doWork() throws InterruptedException {
		RasterJob rendererJob = this.jobQueue.get();
		try {
			if (!this.tileCache.containsKey(rendererJob)) {
				renderTile(rendererJob);
			}
		} finally {
			this.jobQueue.remove(rendererJob);
		}
	}

	private void renderTile(RasterJob rendererJob) {
		long start;
		if (DEBUG_TIMING) {
			start = System.currentTimeMillis();
		}

		TileBitmap bitmap = this.rasterRenderer.executeJob(rendererJob);

		if (DEBUG_TIMING) {
			long end = System.currentTimeMillis();
			long te = this.totalExecutions.incrementAndGet();
			long tt = this.totalTime.addAndGet(end - start);
			if (te % 10 == 0) {
				LOGGER.log(Level.INFO, "TIMING " + Long.toString(te) + " " + Double.toString(tt / te));
			}
		}

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

}
