package de.rooehler.rastertheque.processing.rendering;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.processing.RasterOp;
import de.rooehler.rastertheque.processing.RasterOps;
import de.rooehler.rastertheque.util.Hints;
import de.rooehler.rastertheque.util.Hints.Key;
import de.rooehler.rastertheque.util.ProgressListener;
/**
 * Superclass for AmplitudeRescalers which map raster values to
 * grey scale pixel values
 * 
 * @author Robert Oehler
 *
 */
public abstract class AmplitudeRescaler implements RasterOp{

	private static final int INT_KEY_MINMAX = 1006;	
	
	public static final Key KEY_MINMAX = new Hints.Key(INT_KEY_MINMAX){
		
		@Override
		public boolean isCompatibleValue(Object val) {
			return val != null && val instanceof double[];
		}
		
	};
	
	@Override
	public abstract Priority getPriority();

	@Override
	public abstract void execute(Raster raster, Map<Key, Serializable> params,
			Hints hints, ProgressListener listener);
	
	
	/**
	 * there are no default hints for this operation
	 */
	@Override
	public Hints getDefaultHints() {

		return new Hints(new HashMap<Key,Serializable>());
	}
	
	/**
	 * there are no default parameters
	 */
	@Override
	public Map<Key, Serializable> getDefaultParams() {
		
		return new HashMap<Key,Serializable>();
	}
	
	/**
	 * Amplitude Rescalers need to determine the range {min,max} of a raster
	 * if this parameter is provided, the step is skipped and the provided
	 * range is applied
	 */
	@Override
	public boolean validateParameters(Map<Key, Serializable> params) {

		if(params != null){
			if(params.containsKey(KEY_MINMAX)){
				
				double[] minmax = (double[]) params.get(KEY_MINMAX);
				return !Double.isNaN(minmax[0]) && !Double.isNaN(minmax[1]);									
			}
		}
		//also no params are valid
		return true;
	}

	@Override
	public String getOperationName() {
		
		return RasterOps.AMPLITUDE_RESCALING;
	}
}
