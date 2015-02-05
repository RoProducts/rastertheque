package de.rooehler.rastertheque.processing;

import java.util.Map;

import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.processing.RenderingHints.Key;
import de.rooehler.rastertheque.processing.rendering.MRenderer;

public class RenderOp implements Render{

	
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
	
	public int[] render(
			Raster raster,
			Map <Key,Object> params,
			RenderingHints hints,
			ProgressListener listener){
		
		if(hints == null){

			hints = new RenderingHints(
					RenderingHints.KEY_SYMBOLIZATION,
					RenderingHints.VALUE_AMPLITUDE_RESCALING);

		}
		
		if(params == null || (!params.containsKey(KEY_RENDERER))){
			String filepath = null;
			if(params != null && params.containsKey(KEY_FILEPATH)){
				filepath = (String) params.get(KEY_FILEPATH);
			}
			
			MRenderer renderer = null;
			
			if(filepath != null){
				renderer = new MRenderer(filepath, true);
			}else{
				renderer = new MRenderer(null, false);
			}
			
			if(params != null && params.containsKey(KEY_RGB_BANDS)){
				renderer.useRGBBands((Boolean)params.get(KEY_RGB_BANDS));
			}
			
			params.put(KEY_RENDERER, renderer);
		}
		
		final Renderer renderer = (Renderer) params.get(KEY_RENDERER);
		
		
		return renderer.render(raster);
	}

}
