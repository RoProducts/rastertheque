package de.rooehler.rastertheque.processing;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.core.util.CustomServiceLoader;
import de.rooehler.rastertheque.util.Hints;
import de.rooehler.rastertheque.util.Hints.Key;
import de.rooehler.rastertheque.util.ProgressListener;

/**
 * Facade class for RasterOp operations
 * 
 * @author Robert Oehler
 *
 */
public class RasterOps {

	/**
	 * Currently implemented operations
	 */
	public final static String RESIZE = "RESIZE";
	public final static String COLORMAP = "COLORMAP";
	public final static String AMPLITUDE_RESCALING = "AMPLITUDE_RESCALING";
	public final static String REPROJECT = "REPROJECT";

	private static Map<String,List<RasterOp>> operations;

	/**
	 * Load available implementation of the RasterOp interface
	 */
	static{
		operations = (Map<String,List<RasterOp>>) getRasterOps("org/rastertheque/processing/raster/");
	}

	/**
	 * executes a RasterOp, defined by its @param operation name
	 * according to @param params, with optional hints 
	 * the progress of the operation is reported via @param listener
	 * 
	 * this method retrieves all implementations of @class RasterOp which are available
	 * in a configuration file "de.rooehler.rastertheque.processing.RasterOp"
	 * 
	 * @see https://docs.oracle.com/javase/6/docs/api/java/util/ServiceLoader.html for a description of the concept
	 * 
	 * they are inspected for the desired operation
	 * if there are implementations found, the one with the highest priority is selected
	 * and executed
	 * 
	 * @param raster the raster to manipulate
	 * @param operation the operation to exexute
	 * @param params a map of parameters - can be null depending on the operation
	 * @param hints a map of hints - can be null
	 * @param listener  progress listener - can be null
	 */
	public static void execute(Raster raster, String operation, HashMap<Key, Serializable> params, Hints hints, ProgressListener listener){


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
