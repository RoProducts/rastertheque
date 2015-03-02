package de.rooehler.rastertheque.processing.rendering;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.core.util.ByteBufferReader;
import de.rooehler.rastertheque.core.util.ByteBufferReaderUtil;
import de.rooehler.rastertheque.processing.RasterOp;
import de.rooehler.rastertheque.processing.RasterOps;
import de.rooehler.rastertheque.util.Hints;
import de.rooehler.rastertheque.util.Hints.Key;
import de.rooehler.rastertheque.util.ProgressListener;

public class MColorMap implements RasterOp, Serializable{


	private static final long serialVersionUID = 1184127428068286145L;

	/**
	 * generates an array of colored pixels for a buffer of 
	 * raster pixels according to a priorly loaded ColorMap
	 * if the colorMap is not created priorly by either setting 
	 * it or by placing a .sld file of the same name as the
	 * raster file in the same directory like the raster 
	 * file an exception is thrown
	 * 
	 * @param pBuffer the buffer to read from
	 * @param pixelAmount amount of raster pixels
	 * @param dataType the dataType of the raster pixels
	 * @return the array of color pixels
	 */

	@Override
	public void execute(Raster raster, Map<Key, Serializable> params, Hints hints, ProgressListener listener) {
		
		if(raster.getBands().get(0).colorMap() == null){
			throw new IllegalArgumentException("no colorMap available");
		}
		
		final ColorMap map = raster.getBands().get(0).colorMap();
		
		final ByteBufferReader reader = new ByteBufferReader(raster.getData().array(), ByteOrder.nativeOrder());
		final int raster_width  = raster.getDimension().width();
		final int raster_height = raster.getDimension().height();
		
		final int pixelAmount = raster_width * raster_height;
		
        int[] pixels = new int[pixelAmount];
        
        for (int i = 0; i < pixelAmount; i++) {

        	double d = ByteBufferReaderUtil.getValue(reader, raster.getBands().get(0).datatype());

    		pixels[i] = map.getColorAccordingToValue(d);

        }

        ByteBuffer buffer = ByteBuffer.allocate(pixels.length * 4);
		
		buffer.asIntBuffer().put(pixels);
		
		raster.setData(buffer);
		
	}
	

	@Override
	public String getOperationName() {
		
		return RasterOps.COLORMAP;
	}
	
	
	@Override
	public Priority getPriority() {
	
		return Priority.HIGHEST;
	}
	
	@Override
	public Hints getDefaultHints() {

		return new Hints(new HashMap<Key,Serializable>());
	}
	
	@Override
	public Map<Key, Serializable> getDefaultParams() {
		
		return new HashMap<Key,Serializable>();
	}
	
	@Override
	public boolean validateParameters(Map<Key, Serializable> params) {

		return true;
	}
}
