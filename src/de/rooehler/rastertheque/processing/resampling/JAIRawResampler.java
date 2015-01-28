package de.rooehler.rastertheque.processing.resampling;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.util.Log;
import de.rooehler.raster_jai.JaiInterpolate;
import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.core.util.ByteBufferReader;
import de.rooehler.rastertheque.processing.RawResampler;
import de.rooehler.rastertheque.processing.Resampler.ResampleMethod;

public class JAIRawResampler implements RawResampler {

	@Override
	public void resample(Raster raster, ResampleMethod method) {
		
		if(Double.compare(raster.getBoundingBox().getWidth(),  raster.getDimension().getWidth()) == 0 &&
		   Double.compare(raster.getBoundingBox().getHeight(), raster.getDimension().getHeight()) == 0){
			return;
		}
		
		final int srcWidth = (int) raster.getBoundingBox().getWidth();
		final int srcHeight = (int) raster.getBoundingBox().getHeight();
		
		final int dstWidth = (int) raster.getDimension().getWidth();
		final int dstHeight = (int) raster.getDimension().getHeight();
		
		final ByteBufferReader reader = new ByteBufferReader(raster.getData().array(), ByteOrder.nativeOrder());
		
		
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
		
		final int oldBufferSize = ((int)srcWidth) * ((int)srcHeight) * raster.getBands().size() * raster.getBands().get(0).datatype().size();

		final int newBufferSize = ((int)dstWidth) * ((int)dstHeight) * raster.getBands().size() * raster.getBands().get(0).datatype().size();
		
		final ByteBuffer buffer = ByteBuffer.allocate(newBufferSize);
		buffer.order(ByteOrder.nativeOrder()); 
		
		int x, y, index;
		float x_ratio = ((float) (srcWidth - 1)) / dstWidth;
		float y_ratio = ((float) (srcHeight - 1)) / dstHeight;
		float x_diff, y_diff;

		for(int h = 0; h <raster.getBands().size(); h++){
			
			final int dataSize = raster.getBands().get(h).datatype().size();
			
			for (int i = 0; i < dstHeight; i++) {
				for (int j = 0; j < dstWidth; j++) {

					// src pix coords
					x = (int) (x_ratio * j);
					y = (int) (y_ratio * i);

					// offsets from the current pos to the pos in the new array
					x_diff = (x_ratio * j) - x;
					y_diff = (y_ratio * i) - y;

					index = (y * srcWidth + x);
					
					reader.seekToOffset(index * dataSize);
					
					try{

						switch(raster.getBands().get(h).datatype()) {

						case CHAR:
							final int[] chars = new int[4];
							chars[0] = reader.readChar();
							if(reader.getPos() < oldBufferSize){					
								chars[1] = reader.readChar();
							}else{
								chars[1] = chars[0];
							}

							if(reader.getPos() + (srcWidth * dataSize  - 2 * dataSize) < oldBufferSize){
								//reader is at index + 2 * dataSize
								reader.seekToOffset(reader.getPos() + (srcWidth * dataSize - 2 * dataSize)); 
								chars[2] = reader.readChar();
								if(reader.getPos() < oldBufferSize){
									chars[3] = reader.readChar();
								}else{
									chars[3] = chars[2];				    	
								}
							}else{
								chars[2] = chars[0];	
								chars[3] = chars[1];
							}
							final int interpolatedChar = JaiInterpolate.interpolateRawInts(chars, x_diff, y_diff, interpolation);

							buffer.putChar((char)interpolatedChar);
							break;
						case BYTE:
							final int[] bytes = new int[4];
							bytes[0] = reader.readByte();
							if(reader.getPos() < oldBufferSize){					
								bytes[1] = reader.readByte();
							}else{
								bytes[1] = bytes[0];
							}

							if(reader.getPos() + (srcWidth * dataSize - 2 * dataSize) < oldBufferSize){
								//reader is at index + 2 * dataSize
								reader.seekToOffset(reader.getPos() + (srcWidth * dataSize - 2 * dataSize)); 
								bytes[2] = reader.readByte();	
								if(reader.getPos() < oldBufferSize){
									bytes[3] = reader.readByte();
								}else{
									bytes[3] = bytes[2];	
								}
							}else{
								bytes[2] = bytes[0];	
								bytes[3] = bytes[1];
							}

							final int interpolatedByte = JaiInterpolate.interpolateRawInts(bytes, x_diff, y_diff, interpolation);

							buffer.put((byte)interpolatedByte);
							break;
						case SHORT:
							final int[] shorts = new int[4];
							shorts[0] = reader.readShort();
							if(reader.getPos() < oldBufferSize){					
								shorts[1] = reader.readShort();
							}else{
								shorts[1] = shorts[0];
							}

							if(reader.getPos() + (srcWidth * dataSize - 2 * dataSize) < oldBufferSize){
								//reader is at index + 2 * dataSize
								reader.seekToOffset(reader.getPos() + (srcWidth * dataSize - 2 * dataSize)); 
								shorts[2] = reader.readShort();	
								if(reader.getPos() < oldBufferSize){
									shorts[3] = reader.readShort();
								}else{
									shorts[3] = shorts[2];	
								}
							}else{
								shorts[2] = shorts[0];	
								shorts[3] = shorts[1];
							}

							final int interpolatedShort = JaiInterpolate.interpolateRawInts(shorts, x_diff, y_diff, interpolation);

							buffer.putShort((short)interpolatedShort);
							break;
						case INT:
							final int[] ints = new int[4];
							ints[0] = reader.readInt();
							if(reader.getPos() < oldBufferSize){					
								ints[1] = reader.readInt();
							}else{
								ints[1] = ints[0];
							}

							if(reader.getPos() + (srcWidth * dataSize - 2 * dataSize) < oldBufferSize){
								reader.seekToOffset(reader.getPos() + (srcWidth * dataSize - 2 * dataSize)); 
								ints[2] = reader.readInt();	
								if(reader.getPos() < oldBufferSize){
									ints[3] = reader.readInt();
								}else{
									ints[3] = ints[2];	
								}
							}else{
								ints[2] = ints[0];	
								ints[3] = ints[1];
							}
							final int interpolatedInt = JaiInterpolate.interpolateRawInts(ints, x_diff, y_diff, interpolation);

							buffer.putInt(interpolatedInt);
							break;
						case LONG:
							final int[] longs = new int[4];
							longs[0] = (int) reader.readLong();
							if(reader.getPos() < oldBufferSize){					
								longs[1] = (int) reader.readLong();
							}else{
								longs[1] = longs[0];
							}

							if(reader.getPos() + (srcWidth * dataSize - 2 * dataSize) < oldBufferSize){
								reader.seekToOffset(reader.getPos() + (srcWidth * dataSize - 2 * dataSize)); 
								longs[2] = (int) reader.readLong();	
								if(reader.getPos() < oldBufferSize){
									longs[3] = (int) reader.readLong();
								}else{
									longs[3] = longs[2];	
								}
							}else{
								longs[2] = longs[0];	
								longs[3] = longs[1];
							}
							final int interpolatedLong = JaiInterpolate.interpolateRawInts(longs, x_diff, y_diff, interpolation);

							buffer.putLong(interpolatedLong);
							break;
						case FLOAT:
							final float[] floats = new float[4];
							floats[0] = reader.readFloat();
							if(reader.getPos() < oldBufferSize){					
								floats[1] = reader.readFloat();
							}else{
								floats[1] = floats[0];
							}

							if(reader.getPos() + (srcWidth * dataSize - 2 * dataSize) < oldBufferSize){
								reader.seekToOffset(reader.getPos() + (srcWidth * dataSize - 2 * dataSize)); 
								floats[2] = reader.readFloat();	
								if(reader.getPos() < oldBufferSize){
									floats[3] = reader.readFloat();
								}else{
									floats[3] = floats[2];	
								}
							}else{
								floats[2] = floats[0];	
								floats[3] = floats[1];
							}
							final float interpolatedFloat = JaiInterpolate.interpolateRawFloats(floats, x_diff, y_diff, interpolation);

							buffer.putFloat(interpolatedFloat);

							break;
						case DOUBLE:
							final double[] doubles = new double[4];
							doubles[0] = reader.readDouble();
							if(reader.getPos() < oldBufferSize){					
								doubles[1] = reader.readDouble();
							}else{
								doubles[1] = doubles[0];
							}

							if(reader.getPos() + (srcWidth * dataSize - 2 * dataSize) < oldBufferSize){
								reader.seekToOffset(reader.getPos() + (srcWidth * dataSize - 2 * dataSize)); 
								doubles[2] = reader.readDouble();	
								if(reader.getPos() < oldBufferSize){
									doubles[3] = reader.readDouble();
								}else{
									doubles[3] = doubles[2];	
								}
							}else{
								doubles[2] = doubles[0];	
								doubles[3] = doubles[1];
								reader.seekToOffset(-dataSize); //go one back
							}
							final double interpolatedDouble = JaiInterpolate.interpolateRawDoubles(doubles, x_diff, y_diff, interpolation);

							buffer.putDouble(interpolatedDouble);
							break;
						}
					}catch(IOException e){
						Log.e(JAIRawResampler.class.getSimpleName(), "IOException reading bytebufferedreader",e);
					}
				}
			}
		}
		raster.setData(buffer);
	}

}
