package de.rooehler.rastertheque.processing;

import java.util.HashMap;
import java.util.Map;

import com.vividsolutions.jts.geom.Envelope;

import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.processing.RenderingHints.Key;
import de.rooehler.rastertheque.processing.Resampler.ResampleMethod;
import de.rooehler.rastertheque.processing.resampling.JAIResampler;
import de.rooehler.rastertheque.processing.resampling.MResampler;
import de.rooehler.rastertheque.processing.resampling.OpenCVResampler;

public class ResizeOp implements Resize{
	
	private static final int INT_KEY_RESAMPLER = 5;
	
	private static final int INT_VALUE_RESAMPLER_OPENCV = 6;	
	private static final int INT_VALUE_RESAMPLER_M = 7;	
	private static final int INT_VALUE_RESAMPLER_JAI= 8;	
	
	private static final int INT_KEY_SIZE = 5;	
	
	public static final Object VALUE_RESAMPLER_OPENCV =	INT_VALUE_RESAMPLER_OPENCV;
	public static final Object VALUE_RESAMPLER_MIMPL  =	INT_VALUE_RESAMPLER_M;
	public static final Object VALUE_RESAMPLER_JAI    =	INT_VALUE_RESAMPLER_JAI;
	
	public static final Key KEY_SIZE = new RenderingHints.Key(INT_KEY_SIZE){
		
		@Override
		public boolean isCompatibleValue(Object val) {
			return val != null && val instanceof Envelope;
		}
		
	};
	
	public static final Key KEY_RESAMPLER = new RenderingHints.Key(INT_KEY_RESAMPLER){
		
		@Override
		public boolean isCompatibleValue(Object val) {
			return val.equals(VALUE_RESAMPLER_JAI) || val.equals(VALUE_RESAMPLER_MIMPL) || val.equals(VALUE_RESAMPLER_JAI);
		}
		
	};
	@Override
	public void resize(
			Raster raster,
			Map <Key,Object> params,
			RenderingHints hints,
			ProgressListener listener){
		
		
		if(params == null || (!params.containsKey(KEY_SIZE)) || (!(params.get(KEY_SIZE) instanceof Envelope))){
			throw new IllegalArgumentException("must specify target dimension as param");
		}
		
		if(hints == null){

			hints = new RenderingHints(
					RenderingHints.KEY_INTERPOLATION,
					RenderingHints.VALUE_INTERPOLATION_BILINEAR);

		}
		
		if(!params.containsKey(KEY_RESAMPLER)){
			params.put(KEY_RESAMPLER, VALUE_RESAMPLER_OPENCV);
		}
		
		final Envelope dstDim = (Envelope) params.get(KEY_SIZE);
		
		final Resampler resampler = getResamplerFromParams(params.get(KEY_RESAMPLER));
		
		final ResampleMethod m = getResampleMethodFromHints(hints.get(RenderingHints.KEY_INTERPOLATION));
		
		resampler.resample(raster, dstDim, m , listener);
		
	}

	private static ResampleMethod getResampleMethodFromHints(Object object) {
		
		//TODO how to get plugged objects ?
		
		if(object.equals(RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)){
			return ResampleMethod.NEARESTNEIGHBOUR;
		}else if (object.equals(RenderingHints.VALUE_INTERPOLATION_BILINEAR)){
			return ResampleMethod.BILINEAR;
		}else if(object.equals(RenderingHints.VALUE_INTERPOLATION_BICUBIC)){
			return ResampleMethod.BICUBIC;
		}
		
		return null;
	}

	private static Resampler getResamplerFromParams(Object object) {
		
		//TODO how to get plugged objects ?
		
		if(object.equals(VALUE_RESAMPLER_OPENCV)){
			return new OpenCVResampler();
		}else if(object.equals(VALUE_RESAMPLER_MIMPL)){
			return new MResampler();
		}else if(object.equals(VALUE_RESAMPLER_JAI)){
			return new JAIResampler();
		}
		
		return null;
	}

}
