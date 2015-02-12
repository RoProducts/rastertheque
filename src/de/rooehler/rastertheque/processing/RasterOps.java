package de.rooehler.rastertheque.processing;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.core.util.CustomServiceLoader;
import de.rooehler.rastertheque.util.Hints;
import de.rooehler.rastertheque.util.Hints.Key;
import de.rooehler.rastertheque.util.ProgressListener;

@SuppressWarnings("unchecked")
public class RasterOps {
	
	
	public final static String RESIZE = "RESIZE";
	
	public final static String COLORMAP = "COLORMAP";
	
	public final static String AMPLITUDE_RESCALING = "AMPLITUDE_RESCALING";
	
	private static ArrayList<RasterOp> operations;
	
	static{
		operations = (ArrayList<RasterOp>) RasterOps.getRasterOps("org/rastertheque/processing/raster/",RasterOp.class);
	}
	
	@SuppressWarnings("rawtypes")
	public static ArrayList<?> getRasterOps(final String pathToService, final Class clazz){

		ArrayList<Object> drivers = new ArrayList<>();

		Iterator<?> it = CustomServiceLoader.load(clazz, pathToService).iterator();

		while(it.hasNext()){
			drivers.add(it.next());
		}

		return drivers;
	}


	public static void execute(Raster raster, final String operation, final HashMap<Key,Serializable> params, Hints hints, ProgressListener listener){
		
		
		// search for the operation	
		if(operation.equals(RESIZE) || operation.equals(COLORMAP) || operation.equals(AMPLITUDE_RESCALING)){
			
			final Key key = getKeyForOperation(operation);
			
			if(params != null && params.containsKey(key)){
				RasterOp  resampleImpl = (RasterOp) params.get(key);
				resampleImpl.execute(raster, params, hints, listener);
				return;
			}
			
			for(RasterOp op : operations){
				//TODO some priority which to choose ???
				if(op.getOperationName().equals(operation)){
					op.execute(raster, params, hints, listener);
					break;
				}
			}
			
			
		}else{
			
			throw new IllegalArgumentException("Invalid Operation");
		}

	}
	
	public static Key getKeyForOperation(String operation){
		
		if(operation.equals(RESIZE)){
			
			return Hints.KEY_RESAMPLER;
					
		}else if(operation.equals(COLORMAP)){
			
			return Hints.KEY_COLORMAP;
			
		}else if(operation.equals(AMPLITUDE_RESCALING)){
			
			return Hints.KEY_AMPLITUDE_RESCALING;
			
		}else{
			
			throw new IllegalArgumentException("Invalid Operation");
		}
	}

}
