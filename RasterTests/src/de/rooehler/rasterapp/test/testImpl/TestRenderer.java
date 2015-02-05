package de.rooehler.rasterapp.test.testImpl;

import java.util.Map;

import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.processing.ProgressListener;
import de.rooehler.rastertheque.processing.Render;
import de.rooehler.rastertheque.processing.RenderingHints;
import de.rooehler.rastertheque.processing.RenderingHints.Key;

public class TestRenderer implements Render{

	@Override
	public int[] render(Raster raster, Map<Key, Object> params,
			RenderingHints hints, ProgressListener listener) {
		// TODO Auto-generated method stub
		return null;
	}

}
