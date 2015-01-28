package de.rooehler.rastertheque.processing.resampling;

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.util.Log;
import de.rooehler.rastertheque.core.DataType;
import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.core.util.ByteBufferReader;
import de.rooehler.rastertheque.processing.RawResampler;
import de.rooehler.rastertheque.processing.Resampler.ResampleMethod;

public class MRawResampler implements RawResampler {

	@Override
	public void resample(Raster raster, ResampleMethod method) {


		if(Double.compare(raster.getBoundingBox().getWidth(), raster.getDimension().getWidth()) == 0 &&
		   Double.compare(raster.getBoundingBox().getHeight(), raster.getDimension().getHeight()) == 0){
			return;
		}

		
		final int srcWidth = (int) raster.getBoundingBox().getWidth();
		final int srcHeight = (int) raster.getBoundingBox().getHeight();
		
		final int dstWidth = (int) raster.getDimension().getWidth();
		final int dstHeight = (int) raster.getDimension().getHeight();
		
		final ByteBufferReader reader = new ByteBufferReader(raster.getData().array(), ByteOrder.nativeOrder());
		
		int x, y, index;
		float x_ratio = ((float) (srcWidth - 1)) / dstWidth;
		float y_ratio = ((float) (srcHeight - 1)) / dstHeight;
		float x_diff, y_diff;
		
		final int oldBufferSize = ((int)srcWidth) * ((int)srcHeight) * raster.getBands().size() * raster.getBands().get(0).datatype().size();

		final int newBufferSize = ((int)dstWidth) * ((int)dstHeight) * raster.getBands().size() * raster.getBands().get(0).datatype().size();
				
		final ByteBuffer buffer = ByteBuffer.allocate(newBufferSize);
		buffer.order(ByteOrder.nativeOrder()); 
		
		for(int h = 0; h <raster.getBands().size(); h++){
			for (int i = 0; i < dstHeight; i++) {
				for (int j = 0; j < dstWidth; j++) {

					// src pix coords
					x = (int) (x_ratio * j);
					y = (int) (y_ratio * i);

					// offsets from the current pos to the pos in the new array
					x_diff = (x_ratio * j) - x;
					y_diff = (y_ratio * i) - y;

					// current pos
					index = y * srcWidth + x;

					Object[] values = getNeighbors(reader, index, oldBufferSize , srcWidth, raster.getBands().get(h).datatype());
					
					final int nearestX = (int) Math.rint(x + x_diff);
					final int nearestY = (int) Math.rint(y + y_diff);
					
					switch(raster.getBands().get(h).datatype()) {
					case BYTE:
						
						Byte[] bytes = (Byte[]) values;
						byte interpolatedByte = 0;
						switch(method){
						case BICUBIC:
							throw new IllegalArgumentException("not implemeneted currently");
							
						case BILINEAR:
							interpolatedByte = 
									(byte) (bytes[0] * (1 - x_diff) * (1 - y_diff) +
							bytes[1] * (x_diff) * (1 - y_diff) +
							bytes[2] * (y_diff) * (1 - x_diff) +
							bytes[3] * (x_diff * y_diff));
							break;
						case NEARESTNEIGHBOUR:

							if(nearestX == x && nearestY == y){
								interpolatedByte = bytes[0];
							}else if(nearestX == x && nearestY == (y + 1)){
								interpolatedByte = bytes[2];
							}else if(nearestX == (x + 1) && nearestY ==  y){
								interpolatedByte = bytes[1];
							}else if(nearestX == (x + 1) && nearestY == (y + 1)){
								interpolatedByte = bytes[3];
							}else{
								throw new IllegalArgumentException("Nearest Neighbour calc is wrong");
							}
							break;						
						}
						//Log.d(MRawResampler.class.getSimpleName(), String.format("x %d y %d resampled %d", j, i , interpolatedByte));
						buffer.put(interpolatedByte);
												
						break;
					case CHAR:
						
						Character[] chars = (Character[]) values;
						char interpolatedValue = 0;
						switch(method){
						case BICUBIC:
							throw new IllegalArgumentException("not implemeneted currently");
							
						case BILINEAR:
							
							interpolatedValue = 
							(char) (chars[0] * (1 - x_diff) * (1 - y_diff) +
							chars[1] * (x_diff) * (1 - y_diff) +
							chars[2] * (y_diff) * (1 - x_diff) +
							chars[3] * (x_diff * y_diff));
							break;
						case NEARESTNEIGHBOUR:
							if(nearestX == x && nearestY == y){
								interpolatedValue = chars[0];
							}else if(nearestX == x && nearestY == (y + 1)){
								interpolatedValue = chars[2];
							}else if(nearestX == (x + 1) && nearestY ==  y){
								interpolatedValue = chars[1];
							}else if(nearestX == (x + 1) && nearestY == (y + 1)){
								interpolatedValue = chars[3];
							}else{
								throw new IllegalArgumentException("Nearest Neighbour calc is wrong");
							}
							break;
						}
						
						buffer.putChar(interpolatedValue);
						
						break;
					case DOUBLE:
						
						Double[] doubles = (Double[]) values;
						double interpolatedDouble = 0;
						switch(method){
						case BICUBIC:
							throw new IllegalArgumentException("not implemeneted currently");
							
						case BILINEAR:
		
							// Yb = Ab(1-w)(1-h) + Bb(w)(1-h) + Cb(h)(1-w) + Db(wh)
							interpolatedDouble = 
							doubles[0] * (1 - x_diff) * (1 - y_diff) +
							doubles[1] * (x_diff) * (1 - y_diff) +
							doubles[2] * (y_diff) * (1 - x_diff) +
							doubles[3] * (x_diff * y_diff);
							break;
						case NEARESTNEIGHBOUR:
							if(nearestX == x && nearestY == y){
								interpolatedDouble = doubles[0];
							}else if(nearestX == x && nearestY == (y + 1)){
								interpolatedDouble = doubles[2];
							}else if(nearestX == (x + 1) && nearestY ==  y){
								interpolatedDouble = doubles[1];
							}else if(nearestX == (x + 1) && nearestY == (y + 1)){
								interpolatedDouble = doubles[3];
							}else{
								throw new IllegalArgumentException("Nearest Neighbour calc is wrong");
							}
							break;						
						}
						
						buffer.putDouble(interpolatedDouble);
						break;
						
					case FLOAT:
												
						Float[] floats = (Float[]) values;
						float interpolatedFloat = 0;
						switch(method){
						case BICUBIC:
							throw new IllegalArgumentException("not implemeneted currently");
							
						case BILINEAR:
							
							interpolatedFloat = 
							floats[0] * (1 - x_diff) * (1 - y_diff) +
							floats[1] * (x_diff) * (1 - y_diff) +
							floats[2] * (y_diff) * (1 - x_diff) +
							floats[3] * (x_diff * y_diff);
							break;
						case NEARESTNEIGHBOUR:
							if(nearestX == x && nearestY == y){
								interpolatedFloat = floats[0];
							}else if(nearestX == x && nearestY == (y + 1)){
								interpolatedFloat = floats[2];
							}else if(nearestX == (x + 1) && nearestY ==  y){
								interpolatedFloat = floats[1];
							}else if(nearestX == (x + 1) && nearestY == (y + 1)){
								interpolatedFloat = floats[3];
							}else{
								throw new IllegalArgumentException("Nearest Neighbour calc is wrong");
							}
							break;
						}
						
						buffer.putFloat(interpolatedFloat);
						
						break;
					case INT:
						
						Integer[] ints = (Integer[]) values;
						int interpolatedInt = 0;
						switch(method){
						case BICUBIC:
							throw new IllegalArgumentException("not implemeneted currently");
							
						case BILINEAR:
							
							interpolatedInt = 
							(int) (ints[0] * (1 - x_diff) * (1 - y_diff) +
							ints[1] * (x_diff) * (1 - y_diff) +
							ints[2] * (y_diff) * (1 - x_diff) +
							ints[3] * (x_diff * y_diff));
							break;
						case NEARESTNEIGHBOUR:
							if(nearestX == x && nearestY == y){
								interpolatedInt = ints[0];
							}else if(nearestX == x && nearestY == (y + 1)){
								interpolatedInt = ints[2];
							}else if(nearestX == (x + 1) && nearestY ==  y){
								interpolatedInt = ints[1];
							}else if(nearestX == (x + 1) && nearestY == (y + 1)){
								interpolatedInt = ints[3];
							}else{
								throw new IllegalArgumentException("Nearest Neighbour calc is wrong");
							}
							break;
						}
						
						buffer.putInt(interpolatedInt);
						
						break;
					case LONG:
						
						Long[] longs = (Long[]) values;
						long interpolatedLong = 0;
						switch(method){
						case BICUBIC:
							throw new IllegalArgumentException("not implemeneted currently");
							
						case BILINEAR:
							
							interpolatedLong = 
							(long) (longs[0] * (1 - x_diff) * (1 - y_diff) +
							longs[1] * (x_diff) * (1 - y_diff) +
							longs[2] * (y_diff) * (1 - x_diff) +
							longs[3] * (x_diff * y_diff));
							break;
						case NEARESTNEIGHBOUR:
							if(nearestX == x && nearestY == y){
								interpolatedLong = longs[0];
							}else if(nearestX == x && nearestY == (y + 1)){
								interpolatedLong = longs[2];
							}else if(nearestX == (x + 1) && nearestY ==  y){
								interpolatedLong = longs[1];
							}else if(nearestX == (x + 1) && nearestY == (y + 1)){
								interpolatedLong = longs[3];
							}else{
								throw new IllegalArgumentException("Nearest Neighbour calc is wrong");
							}
							break;
						}
						
						buffer.putLong(interpolatedLong);
						
						break;
					case SHORT:
						
						Short[] shorts = (Short[]) values;
						short interpolatedShort= 0;
						switch(method){
						case BICUBIC:
							throw new IllegalArgumentException("not implemeneted currently");
							
						case BILINEAR:
							
							interpolatedShort = 
							(short) (shorts[0] * (1 - x_diff) * (1 - y_diff) +
							shorts[1] * (x_diff) * (1 - y_diff) +
							shorts[2] * (y_diff) * (1 - x_diff) +
							shorts[3] * (x_diff * y_diff));
							break;
						case NEARESTNEIGHBOUR:
							if(nearestX == x && nearestY == y){
								interpolatedShort = shorts[0];
							}else if(nearestX == x && nearestY == (y + 1)){
								interpolatedShort = shorts[2];
							}else if(nearestX == (x + 1) && nearestY ==  y){
								interpolatedShort = shorts[1];
							}else if(nearestX == (x + 1) && nearestY == (y + 1)){
								interpolatedShort = shorts[3];
							}else{
								throw new IllegalArgumentException("Nearest Neighbour calc is wrong");
							}
							break;
						}
						
						buffer.putShort(interpolatedShort);
						
						break;
					
					}

					


				}
			}
		}
		raster.setData(buffer);

	}
	
	/**
	 * retrieve an array of neighbor values for a given position in the array
	 * consisting in the pixels x, x + 1, x + rasterWidth, x + rasterWidth + 1
	 *   
	 * @param reader the reader to read from
	 * @param position the position inside the array
	 * @param readerSize the size of the reader -> up to which position can be read
	 * @param rasterWidth the width of a row in the raster
	 * @param dataType the datatype according to which the data is read
	 * @return the neighbors of this pixel
	 */
	private Object[] getNeighbors(final ByteBufferReader reader,final int position,final int readerSize,final int rasterWidth, final DataType dataType){

		final int dataSize = dataType.size();
		
		reader.seekToOffset(position * dataSize);
		
		try{
			switch(dataType) {
			
			case CHAR:
				final Object[] chars = (Object[])  Array.newInstance(Character.class, 4);
				chars[0] = reader.readChar();
				if(reader.getPos() < readerSize){					
					chars[1] = reader.readChar();
				}else{
					chars[1] = chars[0];
				}
				
				if(reader.getPos() + (rasterWidth * dataSize  - 2 * dataSize) < readerSize){
					//reader is at index + 2 * dataSize
				    reader.seekToOffset(reader.getPos() + (rasterWidth * dataSize - 2 * dataSize)); 
				    chars[2] = reader.readChar();
				    if(reader.getPos() < readerSize){
				    	chars[3] = reader.readChar();
				    }else{
				    	chars[3] = chars[2];				    	
				    }
				}else{
					chars[2] = chars[0];	
					chars[3] = chars[1];
				}
				return chars;
			case BYTE:
				final Object[] bytes = (Object[])  Array.newInstance(Byte.class, 4);
				bytes[0] = reader.readByte();
				if(reader.getPos() < readerSize){					
					bytes[1] = reader.readByte();
				}else{
					bytes[1] = bytes[0];
				}
				
				if(reader.getPos() + (rasterWidth * dataSize - 2 * dataSize) < readerSize){
					//reader is at index + 2 * dataSize
					reader.seekToOffset(reader.getPos() + (rasterWidth * dataSize - 2 * dataSize)); 
				    bytes[2] = reader.readByte();	
				    if(reader.getPos() < readerSize){
				    	bytes[3] = reader.readByte();
				    }else{
				    	bytes[3] = bytes[2];	
				    }
				}else{
					bytes[2] = bytes[0];	
					bytes[3] = bytes[1];
				}
				
				return bytes;
			case SHORT:
				final Object[] shorts = (Object[])  Array.newInstance(Short.class, 4);
				shorts[0] = reader.readShort();
				if(reader.getPos() < readerSize){					
					shorts[1] = reader.readShort();
				}else{
					shorts[1] = shorts[0];
				}
				
				if(reader.getPos() + (rasterWidth * dataSize - 2 * dataSize) < readerSize){
					//reader is at index + 2 * dataSize
					reader.seekToOffset(reader.getPos() + (rasterWidth * dataSize - 2 * dataSize)); 
				    shorts[2] = reader.readShort();	
				    if(reader.getPos() < readerSize){
				    	shorts[3] = reader.readShort();
				    }else{
				    	shorts[3] = shorts[2];	
				    }
				}else{
					shorts[2] = shorts[0];	
					shorts[3] = shorts[1];
				}
				return shorts;
				
			case INT:
				final Object[] ints = (Object[])  Array.newInstance(Integer.class, 4);
				ints[0] = reader.readInt();
				if(reader.getPos() < readerSize){					
					ints[1] = reader.readInt();
				}else{
					ints[1] = ints[0];
				}
				
				if(reader.getPos() + (rasterWidth * dataSize - 2 * dataSize) < readerSize){
					reader.seekToOffset(reader.getPos() + (rasterWidth * dataSize - 2 * dataSize)); 
				    ints[2] = reader.readInt();	
				    if(reader.getPos() < readerSize){
				    	ints[3] = reader.readInt();
				    }else{
				    	ints[3] = ints[2];	
				    }
				}else{
					ints[2] = ints[0];	
					ints[3] = ints[1];
				}

				return ints;
				
			case LONG:
				final Object[] longs = (Object[])  Array.newInstance(Long.class, 4);
				longs[0] = reader.readLong();
				if(reader.getPos() < readerSize){					
					longs[1] = reader.readLong();
				}else{
					longs[1] = longs[0];
				}
				
				if(reader.getPos() + (rasterWidth * dataSize - 2 * dataSize) < readerSize){
					reader.seekToOffset(reader.getPos() + (rasterWidth * dataSize - 2 * dataSize)); 
				    longs[2] = reader.readLong();	
				    if(reader.getPos() < readerSize){
				    	longs[3] = reader.readLong();
				    }else{
				    	longs[3] = longs[2];	
				    }
				}else{
					longs[2] = longs[0];	
					longs[3] = longs[1];
				}
				return longs;
				
			case FLOAT:
				final Object[] floats = (Object[])  Array.newInstance(Float.class, 4);
				floats[0] = reader.readFloat();
				if(reader.getPos() < readerSize){					
					floats[1] = reader.readFloat();
				}else{
					floats[1] = floats[0];
				}
				
				if(reader.getPos() + (rasterWidth * dataSize - 2 * dataSize) < readerSize){
					reader.seekToOffset(reader.getPos() + (rasterWidth * dataSize - 2 * dataSize)); 
				    floats[2] = reader.readFloat();	
				    if(reader.getPos() < readerSize){
				    	floats[3] = reader.readFloat();
				    }else{
				    	floats[3] = floats[2];	
				    }
				}else{
					floats[2] = floats[0];	
					floats[3] = floats[1];
				}
				return floats;
				
			case DOUBLE:
				final Object[] doubles = (Object[])  Array.newInstance(Double.class, 4);
				doubles[0] = reader.readDouble();
				if(reader.getPos() < readerSize){					
					doubles[1] = reader.readDouble();
				}else{
					doubles[1] = doubles[0];
				}
				
				if(reader.getPos() + (rasterWidth * dataSize - 2 * dataSize) < readerSize){
					reader.seekToOffset(reader.getPos() + (rasterWidth * dataSize - 2 * dataSize)); 
				    doubles[2] = reader.readDouble();	
				    if(reader.getPos() < readerSize){
				    	doubles[3] = reader.readDouble();
				    }else{
				    	doubles[3] = doubles[2];	
				    }
				}else{
					doubles[2] = doubles[0];	
					doubles[3] = doubles[1];
					reader.seekToOffset(-dataSize); //go one back
				}
				
				return doubles;
			}
		}catch(IOException  e){
			Log.e(MRawResampler.class.getSimpleName(), "error reading from byteBufferedReader");
			return null;
		}
		return null;
	}

}

