package de.rooehler.rastertheque.processing;

import java.io.Serializable;
import java.util.Map;

import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.util.Hints;
import de.rooehler.rastertheque.util.Hints.Key;
import de.rooehler.rastertheque.util.ProgressListener;


/**
 * A RasterOp models an abstract operation on a Raster
 * 
 * @author Robert Oehler
 *
 */

public interface RasterOp {
	
	
	enum Priority{
		LOW,
		NORMAL,
		HIGH,
		HIGHEST
	}
	/**
	 * default hints for this operation
	 */
	Hints getDefaultHints();
	/**
	 * default parameters for this operation
	 */
	Map<Key,Serializable> getDefaultParams();
	
	/**
	 * validates the map of parameters
	 * @param params the parameters to validate
	 * @return true if the parameters are valid
	 */
	boolean validateParameters(Map<Key,Serializable> params);
	
	/**
	 * a priority for this operation over other operations of the same name
	 * @return the priority
	 */
	Priority getPriority();
	
	/**
	 * the name of this operation
	 * @return the name of this operation
	 */
	String getOperationName();

	/**
	 * executes the RasterOp, manipulating the raster object
	 * @param raster the raster to work on
	 * @param params the parameters to apply
	 * @param hints the hints to use - can be null
	 * @param listener the listener to report progress - can be null
	 */
	void execute(Raster raster, Map <Key,Serializable> params, Hints hints, ProgressListener listener);
	
	
}
