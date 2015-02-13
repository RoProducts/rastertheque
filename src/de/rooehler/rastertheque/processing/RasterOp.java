package de.rooehler.rastertheque.processing;

import java.io.Serializable;
import java.util.Map;

import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.util.Hints;
import de.rooehler.rastertheque.util.Hints.Key;
import de.rooehler.rastertheque.util.ProgressListener;



public interface RasterOp {
	
	
	enum Priority{
		LOW,
		NORMAL,
		HIGH,
		HIGHEST
	}
	
	Hints getDefaultHints();
	
	Map<Key,Serializable> getDefaultParams();
	
	boolean validateParameters(Map<Key,Serializable> params);
	
	Priority getPriority();
	
	String getOperationName();

	void execute(Raster raster, Map <Key,Serializable> params, Hints hints, ProgressListener listener);
	
	
}
