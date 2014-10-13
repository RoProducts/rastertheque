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

import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.queue.JobQueue;
import org.mapsforge.map.layer.renderer.MapWorker;
import org.mapsforge.map.util.PausableThread;

public class MBTilesWorkerThread extends PausableThread {

	private static final boolean DEBUG_TIMING = false;
	private static final Logger LOGGER = Logger.getLogger(MapWorker.class.getName());
	private final AtomicLong totalExecutions;
	// for timing only
	private final AtomicLong totalTime;

	private final Layer layer;
	private final TileCache tileCache;
	private final MBTilesRenderer mbTilesRenderer;
	private final JobQueue<MBTilesJob> jobQueue;

	public MBTilesWorkerThread(TileCache tileCache, JobQueue<MBTilesJob> jobQueue, MBTilesRenderer mbTilesRenderer,
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
		this.mbTilesRenderer = mbTilesRenderer;
		this.jobQueue = jobQueue;
	}

	@Override
	protected void doWork() throws InterruptedException {
		MBTilesJob rendererJob = this.jobQueue.get();
		try {
			if (!this.tileCache.containsKey(rendererJob)) {
				renderTile(rendererJob);
			}
		} finally {
			this.jobQueue.remove(rendererJob);
		}

	}

	private void renderTile(MBTilesJob rendererJob) {
		long start;
		if (DEBUG_TIMING) {
			start = System.currentTimeMillis();
		}

		TileBitmap bitmap = this.mbTilesRenderer.executeJob(rendererJob);

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
