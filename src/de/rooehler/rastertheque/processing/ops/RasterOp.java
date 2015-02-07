package de.rooehler.rastertheque.processing.ops;

import java.util.ArrayList;
import java.util.HashMap;

import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.processing.Renderer;
import de.rooehler.rastertheque.processing.RenderingHints.Key;
import de.rooehler.rastertheque.processing.Resampler;

public class RasterOp {
	
	
	@SuppressWarnings("serial")
	static ArrayList<String> operations = new ArrayList<String>(){{
		add("Resize");
		add("Render");
	}};
	
	
	@SuppressWarnings("unchecked")
	public static void execute(Raster raster, final String operation, final HashMap<Key,Object> params){



		ArrayList<Resampler> resamplers = (ArrayList<Resampler>) RasterOps.getRasterOps("org/rastertheque/processing/raster/",Resampler.class);

		ArrayList<Renderer> renderers = (ArrayList<Renderer>) RasterOps.getRasterOps("org/rastertheque/processing/raster/", Renderer.class);

	}
	
	
	
	public ArrayList<String> getAvailableOps(){
		
		return operations;		
	}

}
