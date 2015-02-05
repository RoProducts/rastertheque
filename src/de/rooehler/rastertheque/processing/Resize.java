package de.rooehler.rastertheque.processing;

import java.util.Map;

import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.processing.RenderingHints.Key;

public interface Resize {
	
	void resize(Raster raster,
			Map <Key,Object> params,
			RenderingHints hints,
			ProgressListener listener);

}
