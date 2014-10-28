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

import org.gdal.gdal.Dataset;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.queue.Job;
import org.mapsforge.map.model.DisplayModel;

public class RasterFileJob extends Job {

	public final DisplayModel displayModel;
	private Dataset dataset;
	private final int hashCodeValue;

	protected RasterFileJob(Tile tile, DisplayModel displayModel, final Dataset pDataSet, boolean hasAlpha) {
		super(tile, hasAlpha);


		this.dataset = pDataSet;

		this.displayModel = displayModel;

		this.hashCodeValue = calculateHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!super.equals(obj)) {
			return false;
		} else if (!(obj instanceof RasterFileJob)) {
			return false;
		}
		RasterFileJob other = (RasterFileJob) obj;
		if (!this.dataset.equals(other.dataset)) {
			return false;
		} else if (!this.displayModel.equals(other.displayModel)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return this.hashCodeValue;
	}

	private int calculateHashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + this.dataset.hashCode();
		return result;
	}

}
