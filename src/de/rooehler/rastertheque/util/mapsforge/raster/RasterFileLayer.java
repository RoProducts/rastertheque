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

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.TileLayer;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.MapViewPosition;

public class RasterFileLayer extends TileLayer<RasterFileJob> {

	private RasterFileRenderer rasterFileRenderer;

	private RasterFileWorkerThread worker;

	private BBDataSet dataSet;

	public RasterFileLayer(TileCache tileCache, MapViewPosition mapViewPosition, boolean isTransparent,
			GraphicFactory graphicFactory, final BBDataSet pDataSet) {
		super(tileCache, mapViewPosition, graphicFactory.createMatrix(), isTransparent);

		rasterFileRenderer = new RasterFileRenderer(graphicFactory,pDataSet);

		this.dataSet = pDataSet;

	}

	@Override
	public synchronized void setDisplayModel(DisplayModel displayModel) {
		super.setDisplayModel(displayModel);
		if (displayModel != null) {
			this.worker = new RasterFileWorkerThread(this.tileCache, this.jobQueue, this.rasterFileRenderer, this);
			this.worker.start();
		} else {
			// if we do not have a displayModel any more we can stop rendering.
			if (this.worker != null) {
				this.worker.interrupt();
			}
		}
	}

	@Override
	protected RasterFileJob createJob(Tile tile) {
		return new RasterFileJob(tile, this.displayModel, this.dataSet, this.isTransparent);
	}

	@Override
	protected void onAdd() {

		this.rasterFileRenderer.start();
		this.worker.proceed();
		super.onAdd();
	}

	@Override
	protected void onRemove() {

		this.rasterFileRenderer.stop();
		this.worker.pause();
		super.onRemove();
	}

	@Override
	public void onDestroy() {
		this.rasterFileRenderer.destroy();
		this.worker.destroy();
		super.onDestroy();
	}

}
