package de.rooehler.rastertheque.processing.resampling;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

import android.graphics.Rect;
import android.util.Log;
import de.rooehler.jai.JaiInterpolate;
import de.rooehler.rastertheque.core.DataType;
import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.core.util.ByteBufferReader;
import de.rooehler.rastertheque.processing.Interpolation.ResampleMethod;
import de.rooehler.rastertheque.processing.RasterOp;
import de.rooehler.rastertheque.util.Hints;
import de.rooehler.rastertheque.util.Hints.Key;
import de.rooehler.rastertheque.util.ProgressListener;
/**
 * The JAIResampler makes use of the JAI Library to interpolate 
 * a resampled pixel inside the source neighbourhood
 * 
 * This implementation corresponds to MResampler
 * except for the interpolation part which is done by JAI
 * 
 * As JAI supports only int as Integer data format, other 
 * integer datatypes are wrapped by ints
 * 
 * @author Robert Oehler
 *
 */
public class JAIResampler extends Resampler implements RasterOp, Serializable  {

	
	private static final long serialVersionUID = -5579684036360533296L;

	@Override
	public void execute(Raster raster,Map<Key,Serializable> params, Hints hints, ProgressListener listener) {
		

		double scaleX = 0;
		double scaleY = 0;
		if(params != null && params.containsKey(KEY_SIZE)){
			Double[] factors = (Double[]) params.get(KEY_SIZE);
			scaleX = factors[0];
			scaleY = factors[1];
		}else{
			throw new IllegalArgumentException("no scale factors provided, cannot continue");
		}
		
		ResampleMethod method = ResampleMethod.BILINEAR;
		if(hints != null && hints.containsKey(Hints.KEY_INTERPOLATION)){
			method = (ResampleMethod) hints.get(Hints.KEY_INTERPOLATION);
		}

		final int srcWidth  = raster.getDimension().width();
		final int srcHeight = raster.getDimension().height();
		
		final int dstWidth = (int) (srcWidth * scaleX);
		final int dstHeight = (int) (srcHeight * scaleY);
		
		final ByteBufferReader reader = new ByteBufferReader(raster.getData().array(), ByteOrder.nativeOrder());
		final DataType dataType = raster.getBands().get(0).datatype();
		final int bandSize = srcWidth * srcHeight * dataType.size();
		final int readerSize = bandSize * raster.getBands().size();
		
		int interpolation = 0;
		switch (method) {
		case NEARESTNEIGHBOUR:
			interpolation = 0;
			break;
		case BILINEAR:
			interpolation = 1;
			break;
		case BICUBIC:
			interpolation = 2;
			break;
		}
				
		final int newBufferSize = dstWidth * dstHeight * raster.getBands().size() * raster.getBands().get(0).datatype().size();
		final ByteBuffer buffer = ByteBuffer.allocate(newBufferSize);
		buffer.order(ByteOrder.nativeOrder()); 
		
		int x, y, index;
		float x_ratio = ((float) (srcWidth - 1)) / dstWidth;
		float y_ratio = ((float) (srcHeight - 1)) / dstHeight;
		float x_diff, y_diff;
		final int bandAmount = raster.getBands().size();
		
		final float onePercent = raster.getBands().size() * dstHeight * dstWidth / 100f;
		float current = onePercent;
		int percent = 1;

		for(int h = 0; h < bandAmount; h++){
			
			final int dataSize = raster.getBands().get(h).datatype().size();
			final int bandIndex = h * srcHeight * srcWidth;
			
			for (int i = 0; i < dstHeight; i++) {
				for (int j = 0; j < dstWidth; j++) {

					// src pix coords
					x = (int) (x_ratio * j);
					y = (int) (y_ratio * i);

					// offsets from the current pos to the pos in the new array
					x_diff = (x_ratio * j) - x;
					y_diff = (y_ratio * i) - y;

					index = (y * srcWidth + x);
					
					//progress
					if(index * bandAmount > current){
						if(listener != null){							
							listener.onProgress(percent);
						}
						current += onePercent;
						percent++;
					}
					
					reader.seekToOffset(index * dataSize + bandIndex);
					
					try{

						switch(raster.getBands().get(h).datatype()) {

						case BYTE:
							final byte[] bytes = MResampler.getByteNeighbours(reader, readerSize, dataSize, srcWidth);	
							final int[] _bytes = new int[4];
							for(int ib = 0; ib < 4; ib++){
								_bytes[ib] = (int) bytes[ib];
							}
							final int interpolatedByte = JaiInterpolate.interpolateRawInts(_bytes, x_diff, y_diff, interpolation);

							buffer.put((byte)interpolatedByte);
							break;
						case CHAR:
							final char[] chars = MResampler.getCharNeighbours(reader, readerSize, dataSize, srcWidth);	
							final int[] _chars = new int[4];
							for(int ic = 0; ic < 4; ic++){
								_chars[ic] = (int) chars[ic];
							}
							final int interpolatedChar = JaiInterpolate.interpolateRawInts(_chars, x_diff, y_diff, interpolation);
							
							buffer.putChar((char)interpolatedChar);
							break;
						case SHORT:
							final short[] shorts = MResampler.getShortNeighbours(reader, readerSize, dataSize, srcWidth);			
							final int[] _shorts = new int[4];
							for(int is = 0; is < 4; is++){
								_shorts[is] = (int) shorts[is];
							}
							final int interpolatedShort = JaiInterpolate.interpolateRawInts(_shorts, x_diff, y_diff, interpolation);
							buffer.putShort((short)interpolatedShort);
							break;
						case INT:
							final int[] ints = MResampler.getIntNeighbours(reader, readerSize, dataSize, srcWidth);	
							final int interpolatedInt = JaiInterpolate.interpolateRawInts(ints, x_diff, y_diff, interpolation);

							buffer.putInt(interpolatedInt);
							break;
						case LONG:
							final long[] longs= MResampler.getLongNeighbours(reader, readerSize, dataSize, srcWidth);		
							final int[] _longs = new int[4];
							for(int il = 0; il < 4; il++){
								_longs[il] = (int) longs[il];
							}
							final int interpolatedLong = JaiInterpolate.interpolateRawInts(_longs, x_diff, y_diff, interpolation);

							buffer.putLong(interpolatedLong);
							break;
						case FLOAT:
							final float[] floats = MResampler.getFloatNeighbours(reader, readerSize, dataSize, srcWidth);	
							
							final float interpolatedFloat = JaiInterpolate.interpolateRawFloats(floats, x_diff, y_diff, interpolation);

							buffer.putFloat(interpolatedFloat);
							break;
						case DOUBLE:
							final double[] doubles = MResampler.getDoubleNeighbours(reader, readerSize, dataSize, srcWidth);	
							
							final double interpolatedDouble = JaiInterpolate.interpolateRawDoubles(doubles, x_diff, y_diff, interpolation);

							buffer.putDouble(interpolatedDouble);
							break;
						}
					}catch(IOException e){
						Log.e(JAIResampler.class.getSimpleName(), "IOException reading bytebufferedreader",e);
					}
				}
			}
		}
		raster.setDimension(new Rect(0, 0, dstWidth, dstHeight));
		
		raster.setData(buffer);
	}
	
	@Override
	public Priority getPriority() {
	
		return Priority.LOW;
	}

}
