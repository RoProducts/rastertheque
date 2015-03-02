package de.rooehler.rastertheque.processing.reprojecting;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import de.rooehler.rastertheque.processing.RasterOp;
import de.rooehler.rastertheque.processing.RasterOps;
import de.rooehler.rastertheque.util.Constants;
import de.rooehler.rastertheque.util.Hints;
import de.rooehler.rastertheque.util.Hints.Key;

public abstract class Reproject implements RasterOp {
	
	private static final int INT_KEY_REPROJECT_TARGET_CRS = 1111;

	public static final Key KEY_REPROJECT_TARGET_CRS = new Hints.Key(INT_KEY_REPROJECT_TARGET_CRS){
		@Override
		public boolean isCompatibleValue(Object val) {
			return val != null && val instanceof String;
		}
	};
	
	@Override
	public Hints getDefaultHints() {
		
		return new Hints(new HashMap<Key,Serializable>());
	}

	@Override
	public Map<Key, Serializable> getDefaultParams() {
		
		HashMap<Key,Serializable> params = new HashMap<>();
		
		params.put(KEY_REPROJECT_TARGET_CRS, Constants.EPSG_4326);
		
		return params;
	}

	@Override
	public boolean validateParameters(Map<Key, Serializable> params) {
		
		return params != null && params.containsKey(KEY_REPROJECT_TARGET_CRS);
	}

	
	@Override
	public String getOperationName() {
		return RasterOps.REPROJECT;
	}

}
