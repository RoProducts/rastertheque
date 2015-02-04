package de.rooehler.rastertheque.processing;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.processing.RenderingHints.Key;

public class RasterOp {
	
	
	
	public static Raster execute(
			Raster raster,
			Map <String,Serializable> params,
			RenderingHints hints,
			ProgressListener listener){
		
		if(hints == null){
			HashMap<Key,Object> hm = new HashMap<>();
			hm.put(RenderingHints.KEY_INTERPOLATION,
					RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			hm.put(RenderingHints.KEY_SYMBOLIZATION,
					RenderingHints.VALUE_AMPLITUDE_RESCALING);
			
			hints = new RenderingHints(hm);
		}
		
		
		listener.onProgress(5);
		
		return raster;
	}

}
