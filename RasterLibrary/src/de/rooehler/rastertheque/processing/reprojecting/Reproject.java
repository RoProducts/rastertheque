package de.rooehler.rastertheque.processing.reprojecting;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.osgeo.proj4j.CoordinateReferenceSystem;

import android.util.Log;
import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.processing.Interpolation.ResampleMethod;
import de.rooehler.rastertheque.processing.RasterOp;
import de.rooehler.rastertheque.processing.RasterOps;
import de.rooehler.rastertheque.proj.Proj;
import de.rooehler.rastertheque.util.Constants;
import de.rooehler.rastertheque.util.Hints;
import de.rooehler.rastertheque.util.Hints.Key;
import de.rooehler.rastertheque.util.ProgressListener;
/**
 * A super class for all reproject operations
 * 
 * @author Robert Oehler
 *
 */
public abstract class Reproject implements RasterOp {
	
	private static final int INT_KEY_REPROJECT_TARGET_CRS = 1111;

	public static final Key KEY_REPROJECT_TARGET_CRS = new Hints.Key(INT_KEY_REPROJECT_TARGET_CRS){
		@Override
		public boolean isCompatibleValue(Object val) {
			return val != null && val instanceof String;
		}
	};
	
	//implemented by subclasses
	@Override
	public abstract Priority getPriority();

	//implemented by subclasses
	@Override
	public abstract void execute(Raster raster, Map<Key, Serializable> params,Hints hints, ProgressListener listener);
	
	/**
	 * the default interpolation method for a reproject operation is
	 * ResampleMethod.BILINEAR
	 * 
	 * to use another interpolation method provide hints containing them
	 */
	@Override
	public Hints getDefaultHints() {
		
		return new Hints(Hints.KEY_INTERPOLATION, ResampleMethod.BILINEAR);
	}

	/**
	 * returns the default params for a reproject operation
	 * 
	 * a target projection is mandatory as parameter
	 * 
	 * it can be provided as "well-known text" format 
	 * or as Proj parameter String
	 * 
	 */
	@Override
	public Map<Key, Serializable> getDefaultParams() {
		
		HashMap<Key,Serializable> params = new HashMap<>();
		
		params.put(KEY_REPROJECT_TARGET_CRS, Constants.EPSG_4326);
		
		return params;
	}

	/**
	 * a reproject operation must provide a target projection
	 * 
	 * if the provided params contain such an array and the values are valid
	 * (not NAN) true is returned
	 * otherwise false 
	 */
	@Override
	public boolean validateParameters(Map<Key, Serializable> params) {
		
		if(params == null || !params.containsKey(Reproject.KEY_REPROJECT_TARGET_CRS)){
			return false;
		}
		
		String wkt = (String) params.get(Reproject.KEY_REPROJECT_TARGET_CRS);
		//if this is a proj parameter string convert to wkt
		if(wkt != null && wkt.startsWith("+proj")){
			wkt = Proj.proj2wkt(wkt);
		}
		//try to create a CoordinateReferenceSystem from it
		if(wkt!= null){				
			try{
				CoordinateReferenceSystem crs = Proj.crs(wkt);
				return crs != null;
			}catch(RuntimeException e){
				Log.e(Reproject.class.getSimpleName(), "error parsing target projection String "+wkt);
				return false;
			}
		}else{
			return false;
		}
		
	}

	/**
	 * The name of the reproject operation
	 */
	@Override
	public String getOperationName() {
		return RasterOps.REPROJECT;
	}

}
