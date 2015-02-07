package de.rooehler.rastertheque.processing.ops;

import java.util.Map;

import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.processing.ProgressListener;
import de.rooehler.rastertheque.processing.Renderer;
import de.rooehler.rastertheque.processing.RenderingHints;
import de.rooehler.rastertheque.processing.RenderingHints.Key;
import de.rooehler.rastertheque.processing.rendering.MRenderer;

public class RenderOp {

	
	private static final int INT_KEY_RENDERER = 11;	
	
	private static final int INT_KEY_FILEPATH = 22;	
	
	private static final int INT_KEY_RGB_BANDS = 33;	
	
	public static final Key KEY_RENDERER = new RenderingHints.Key(INT_KEY_RENDERER){
		
		@Override
		public boolean isCompatibleValue(Object val) {
			return val != null && val instanceof Renderer;
		}
		
	};
	
	public static final Key KEY_FILEPATH = new RenderingHints.Key(INT_KEY_FILEPATH){
		
		@Override
		public boolean isCompatibleValue(Object val) {
			return val != null && val instanceof String;
		}
		
	};
	
	public static final Key KEY_RGB_BANDS = new RenderingHints.Key(INT_KEY_RGB_BANDS){
		
		@Override
		public boolean isCompatibleValue(Object val) {
			return val != null && val instanceof Boolean;
		}
		
	};
	
	public static int[] render(Raster raster,Map <Key,Object> params,RenderingHints hints,ProgressListener listener){
		
		//TODO -> we don't really want to create a new renderer for every requested raster
		
		//How to use this static stuff using the same renderer and how to change the renderer when the base file changes ?
		
		if(hints == null){
			//default -> amplitude scaling
			hints = new RenderingHints(
					RenderingHints.KEY_SYMBOLIZATION,
					RenderingHints.VALUE_AMPLITUDE_RESCALING);

		}
		
		if(params == null || (!params.containsKey(KEY_RENDERER))){
			//if no renderer is set, use the default impl
			String filepath = null;
			//check if file for colormap is available
			if(params != null && params.containsKey(KEY_FILEPATH)){
				filepath = (String) params.get(KEY_FILEPATH);
			}
			
			MRenderer renderer = null;
			
			if(filepath != null){
				//if filepath, do colormap
				renderer = new MRenderer(filepath, true);
			}else{
				//otherwise amplitude scaling
				renderer = new MRenderer(null, false);
			}
			
			params.put(KEY_RENDERER, renderer);
		}
		
		final Renderer renderer = (Renderer) params.get(KEY_RENDERER);
		
		
		return renderer.render(raster, params, hints, listener);
	}

}
