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

import java.io.File;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.TileLayer;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.MapViewPosition;

import android.content.Context;

public class MBTilesLayer extends TileLayer<MBTilesJob> {

	private MBTilesRenderer mbTilesRenderer;

	private MBTilesWorkerThread worker;

	private File mapFile;

	public MBTilesLayer(Context context, TileCache tileCache, MapViewPosition mapViewPosition, boolean isTransparent,
			GraphicFactory graphicFactory, final String pDBPath) {
		super(tileCache, mapViewPosition, graphicFactory.createMatrix(), isTransparent);

		this.mapFile = new File(pDBPath);

		this.mbTilesRenderer = new MBTilesRenderer(context, graphicFactory, pDBPath);
	}

	@Override
	public synchronized void setDisplayModel(DisplayModel displayModel) {
		super.setDisplayModel(displayModel);
		if (displayModel != null) {
			this.worker = new MBTilesWorkerThread(this.tileCache, this.jobQueue, this.mbTilesRenderer, this);
			this.worker.start();
		} else {
			// if we do not have a displayModel any more we can stop rendering.
			if (this.worker != null) {
				this.worker.interrupt();
			}
		}
	}

	@Override
	protected MBTilesJob createJob(Tile tile) {
		return new MBTilesJob(tile, this.displayModel, this.mapFile, this.isTransparent);
	}

	@Override
	protected void onAdd() {

		this.mbTilesRenderer.start();
		this.worker.proceed();
		super.onAdd();
	}

	@Override
	protected void onRemove() {

		this.mbTilesRenderer.stop();
		this.worker.pause();
		super.onRemove();
	}

	@Override
	public void onDestroy() {
		this.mbTilesRenderer.destroy();
		this.worker.destroy();
		super.onDestroy();
	}

}
