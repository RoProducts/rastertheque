package de.rooehler.rastertheque.util.mapsforge.raster;

import org.gdal.gdal.Dataset;
import org.mapsforge.core.model.BoundingBox;

public class BBDataSet extends Dataset{
	
	
	protected BBDataSet(long cPtr, boolean cMemoryOwn) {
		super(cPtr, cMemoryOwn);
	}

	private BoundingBox bb;
	

	public BoundingBox getBB() {
		return bb;
	}

	public void addBoundingBox(final BoundingBox pBB) {
		this.bb = pBB;
	}

}
