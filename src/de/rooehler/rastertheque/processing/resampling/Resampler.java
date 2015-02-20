package de.rooehler.rastertheque.processing.resampling;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.vividsolutions.jts.geom.Envelope;

import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.processing.RasterOp;
import de.rooehler.rastertheque.processing.RasterOps;
import de.rooehler.rastertheque.processing.Interpolation.ResampleMethod;
import de.rooehler.rastertheque.util.Hints;
import de.rooehler.rastertheque.util.Hints.Key;
import de.rooehler.rastertheque.util.ProgressListener;

public abstract class Resampler implements RasterOp {

	private static final int INT_KEY_SIZE = 1006;	
	
	public static final Key KEY_SIZE = new Hints.Key(INT_KEY_SIZE){
		
		@Override
		public boolean isCompatibleValue(Object val) {
			return val != null && val instanceof Envelope;
		}
		
	};

	
	@Override
	public abstract Priority getPriority();

	@Override
	public abstract void execute(Raster raster, Map<Key, Serializable> params,
			Hints hints, ProgressListener listener);
	
	@Override
	public Hints getDefaultHints() {

		return new Hints(Hints.KEY_INTERPOLATION, ResampleMethod.BILINEAR);
	}
	
	@Override
	public Map<Key, Serializable> getDefaultParams() {
	
		HashMap<Key, Serializable> params = new HashMap<>();
		params.put(KEY_SIZE, new Double[]{1.0d,1.0d});
		
		return params;
	}
	
	@Override
	public boolean validateParameters(Map<Key, Serializable> params) {
	
		if(params == null){
			return false;
		}
		
		if(params.containsKey(KEY_SIZE)){
			if(!(params.get(KEY_SIZE) instanceof Double[])){
				return false;
			}else{
				Double[] factor = (Double[]) params.get(KEY_SIZE);
				if(factor.length == 2 && factor[0] != null && factor[1] != null){
					if(!Double.isNaN(factor[0]) && !Double.isNaN(factor[1])){
						return true;
					}
				}else{
					return false;
				}
			}
		}else{
			return false;
		}
		return false;
	}
	
	@Override
	public  String getOperationName() {
		
		return RasterOps.RESIZE;
	}


}
