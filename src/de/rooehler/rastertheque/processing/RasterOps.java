package de.rooehler.rastertheque.processing;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.util.Log;
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
	public final static String REPROJECT = "REPROJECT";

	private static Map<String,List<RasterOp>> operations;

	static{
		operations = (Map<String,List<RasterOp>>) getRasterOps("org/rastertheque/processing/raster/");
	}

	/**
	 * executes a RasterOp
	 * 
	 * @param raster
	 * @param operation
	 * @param params
	 * @param hints
	 * @param listener
	 */
	public static void execute(Raster raster, final String operation, final HashMap<Key,Serializable> params, Hints hints, ProgressListener listener){


		RasterOp selectedOp = null;

		for(String key : operations.keySet()){

			if(key.equals(operation)){
				List<RasterOp> ops = operations.get(key);
				for(RasterOp op : ops){
					if(selectedOp == null || op.getPriority().ordinal() > selectedOp.getPriority().ordinal()){
						selectedOp = op;
					}
				}
				break;
			}
		}
		if(selectedOp != null){
			
			selectedOp.execute(raster, params, hints, listener);

		}else{

			throw new UnsupportedOperationException("No Implementation found for operation "+operation);
		}

	}
	/**
	 * retrieves a Map<OperationName,List<RasterOp>> of the available RasterOps
	 * 
	 * @param pathToService the path where the configuration file of the RasterOp service resides
	 * @return the Map <String,List<RasterOp>>
	 */
	private static Map<String,List<RasterOp>> getRasterOps(final String pathToService){

		Map<String,List<RasterOp>> ops = new HashMap<>();

		Iterator<RasterOp> it = CustomServiceLoader.load(RasterOp.class, pathToService).iterator();

		while(it.hasNext()){

			final RasterOp op = it.next();

			if(ops.containsKey(op.getOperationName())){

				ops.get(op.getOperationName()).add(op);

			}else{

				ArrayList<RasterOp> list = new ArrayList<>();
				list.add(op);
				ops.put(op.getOperationName(), list);

			}
		}

		return ops;
	}

}
