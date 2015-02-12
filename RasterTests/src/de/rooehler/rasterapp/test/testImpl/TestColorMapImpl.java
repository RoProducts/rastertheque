package de.rooehler.rasterapp.test.testImpl;

import java.io.Serializable;
import java.util.Map;

import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.processing.RasterOp;
import de.rooehler.rastertheque.util.Hints;
import de.rooehler.rastertheque.util.Hints.Key;
import de.rooehler.rastertheque.util.ProgressListener;


public class TestColorMapImpl implements RasterOp {

	@Override
	public void execute(Raster raster, Map <Key,Serializable> params, Hints hints, ProgressListener listener) {

	}

	@Override
	public String getOperationName() {
		
		return "render";
	}

}
