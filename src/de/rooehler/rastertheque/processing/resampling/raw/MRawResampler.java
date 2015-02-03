package de.rooehler.rastertheque.processing.resampling.raw;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.util.Log;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.core.util.ByteBufferReader;
import de.rooehler.rastertheque.processing.RawResampler;

public class MRawResampler implements RawResampler {

	@Override
	public void resample(Raster raster,Envelope dstDimension, ResampleMethod method) {


		if(Double.compare(raster.getBoundingBox().getWidth(), dstDimension.getWidth()) == 0 &&
				Double.compare(raster.getBoundingBox().getHeight(), dstDimension.getHeight()) == 0){
			return;
		}

		final int srcWidth = (int) raster.getBoundingBox().getWidth();
		final int srcHeight = (int) raster.getBoundingBox().getHeight();

		final int dstWidth = (int) dstDimension.getWidth();
		final int dstHeight = (int) dstDimension.getHeight();

		final ByteBufferReader reader = new ByteBufferReader(raster.getData().array(), ByteOrder.nativeOrder());

		int x, y, index;
		float x_ratio = ((float) (srcWidth - 1)) / dstWidth;
		float y_ratio = ((float) (srcHeight - 1)) / dstHeight;
		float x_diff, y_diff;

		final int readerSize = ((int)srcWidth) * ((int)srcHeight) * raster.getBands().size() * raster.getBands().get(0).datatype().size();

		final int newBufferSize = ((int)dstWidth) * ((int)dstHeight) * raster.getBands().size() * raster.getBands().get(0).datatype().size();

		try{

			final ByteBuffer buffer = ByteBuffer.allocate(newBufferSize);
			buffer.order(ByteOrder.nativeOrder()); 

			for(int h = 0; h < raster.getBands().size(); h++){
				final int dataSize = raster.getBands().get(h).datatype().size();
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
						
						reader.seekToOffset(index * dataSize);

						final int nearestX = (int) Math.rint(x + x_diff);
						final int nearestY = (int) Math.rint(y + y_diff);

						switch(raster.getBands().get(h).datatype()) {
						
						case BYTE:

							byte[] bytes = new byte[4];
							bytes[0] = reader.readByte();
							if(reader.getPos() < readerSize){					
								bytes[1] = reader.readByte();
							}else{
								bytes[1] = bytes[0];
							}

							if(reader.getPos() + (srcWidth * dataSize - 2 * dataSize) < readerSize){
								//reader is at index + 2 * dataSize
								reader.seekToOffset(reader.getPos() + (srcWidth * dataSize - 2 * dataSize)); 
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

							byte interpolatedByte = 0;
							switch(method){
							case BICUBIC:

								final double a = 0.0d;
								
								Coordinate coord = new Coordinate(x_ratio * j,y_ratio * i);

								final double _x = coord.x;
								final double _y = coord.y;
								
								final int x0 = (int) Math.floor(_x);	//use floor to handle negative coordinates too
								final int y0 = (int) Math.floor(_y);
								byte bicubic_byte = 0;

								double q = 0;
								for (int _j = 0; _j < 4; _j++) {
									final int v = y0 - 1 + _j;
									double  p = 0;
									
									for (int _i = 0; _i < 4; _i++) {
										final int u = x0 - 1 + _i;
										final int _index = v * srcWidth + u;
										
										if( v < 0){
											if(u >= 0){
												reader.seekToOffset(u * dataSize);
												bicubic_byte = reader.readByte();
											}else{
												reader.seekToOffset(0 * dataSize);
												bicubic_byte = reader.readByte();
											}
										}else if(u < 0){
											reader.seekToOffset(v * srcWidth * dataSize);
											bicubic_byte = reader.readByte();
										}else if(v >= srcWidth){
											if(u < srcWidth){
												reader.seekToOffset((srcWidth - 1 * srcWidth + u) * dataSize);
												bicubic_byte = reader.readByte();							
											}else{
												reader.seekToOffset((srcWidth - 1 * srcWidth + (srcWidth - 1)) * dataSize);
												bicubic_byte = reader.readByte();													
											}
										}else if(u >= srcWidth){
											reader.seekToOffset((v * srcWidth + (srcWidth - 1)) * dataSize);
											bicubic_byte = reader.readByte();	
										}else{
											reader.seekToOffset(_index * dataSize);
											bicubic_byte = reader.readByte();
										}

									    p = p + bicubic_byte * cubic(x - u, a);
									}

									q = q + p * cubic(y - v, a);
								}

								interpolatedByte = (byte) q;
								break;
								
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

							char[] chars =  new char[4];
							chars[0] = reader.readChar();
							if(reader.getPos() < readerSize){					
								chars[1] = reader.readChar();
							}else{
								chars[1] = chars[0];
							}

							if(reader.getPos() + (srcWidth * dataSize  - 2 * dataSize) < readerSize){
								//reader is at index + 2 * dataSize
								reader.seekToOffset(reader.getPos() + (srcWidth * dataSize - 2 * dataSize)); 
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

							char interpolatedValue = 0;
							switch(method){
							case BICUBIC:
								
								final double a = 0.0d;
								
								Coordinate coord = new Coordinate(x_ratio * j,y_ratio * i);

								final double _x = coord.x;
								final double _y = coord.y;
								
								final int x0 = (int) Math.floor(_x);	//use floor to handle negative coordinates too
								final int y0 = (int) Math.floor(_y);
								char bicubic_char = 0;

								double q = 0;
								for (int _j = 0; _j < 4; _j++) {
									final int v = y0 - 1 + _j;
									double  p = 0;
									
									for (int _i = 0; _i < 4; _i++) {
										final int u = x0 - 1 + _i;
										final int _index = v * srcWidth + u;
										
										if( v < 0){
											if(u >= 0){
												reader.seekToOffset(u * dataSize);
												bicubic_char = reader.readChar();
											}else{
												reader.seekToOffset(0 * dataSize);
												bicubic_char = reader.readChar();
											}
										}else if(u < 0){
											reader.seekToOffset(v * srcWidth * dataSize);
											bicubic_char = reader.readChar();
										}else if(v >= srcWidth){
											if(u < srcWidth){
												reader.seekToOffset((srcWidth - 1 * srcWidth + u) * dataSize);
												bicubic_char = reader.readChar();							
											}else{
												reader.seekToOffset((srcWidth - 1 * srcWidth + (srcWidth - 1)) * dataSize);
												bicubic_char = reader.readChar();													
											}
										}else if(u >= srcWidth){
											reader.seekToOffset((v * srcWidth + (srcWidth - 1)) * dataSize);
											bicubic_char = reader.readChar();	
										}else{
											reader.seekToOffset(_index * dataSize);
											bicubic_char = reader.readChar();
										}

									    p = p + bicubic_char * cubic(x - u, a);
									}

									q = q + p * cubic(y - v, a);
								}

								interpolatedValue = (char) q;
								break;
								
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

							double[] doubles = new double[4];
							doubles[0] = reader.readDouble();
							if(reader.getPos() < readerSize){					
								doubles[1] = reader.readDouble();
							}else{
								doubles[1] = doubles[0];
							}

							if(reader.getPos() + (srcWidth * dataSize - 2 * dataSize) < readerSize){
								reader.seekToOffset(reader.getPos() + (srcWidth * dataSize - 2 * dataSize)); 
								doubles[2] = reader.readDouble();	
								if(reader.getPos() < readerSize){
									doubles[3] = reader.readDouble();
								}else{
									doubles[3] = doubles[2];	
								}
							}else{
								doubles[2] = doubles[0];	
								doubles[3] = doubles[1];
							}

							double interpolatedDouble = 0;

							switch(method){
							case BICUBIC:
								
								final double a = 0.0d;
								
								Coordinate coord = new Coordinate(x_ratio * j,y_ratio * i);

								final double _x = coord.x;
								final double _y = coord.y;
								
								final int x0 = (int) Math.floor(_x);	//use floor to handle negative coordinates too
								final int y0 = (int) Math.floor(_y);
								double bicubic_double = 0;

								double q = 0;
								for (int _j = 0; _j < 4; _j++) {
									final int v = y0 - 1 + _j;
									double  p = 0;
									
									for (int _i = 0; _i < 4; _i++) {
										final int u = x0 - 1 + _i;
										final int _index = v * srcWidth + u;
										
										if( v < 0){
											if(u >= 0){
												reader.seekToOffset(u * dataSize);
												bicubic_double = reader.readDouble();
											}else{
												reader.seekToOffset(0 * dataSize);
												bicubic_double = reader.readDouble();
											}
										}else if(u < 0){
											reader.seekToOffset(v * srcWidth * dataSize);
											bicubic_double = reader.readDouble();
										}else if(v >= srcWidth){
											if(u < srcWidth){
												reader.seekToOffset((srcWidth - 1 * srcWidth + u) * dataSize);
												bicubic_double = reader.readDouble();							
											}else{
												reader.seekToOffset((srcWidth - 1 * srcWidth + (srcWidth - 1)) * dataSize);
												bicubic_double = reader.readDouble();													
											}
										}else if(u >= srcWidth){
											reader.seekToOffset((v * srcWidth + (srcWidth - 1)) * dataSize);
											bicubic_double = reader.readDouble();	
										}else{
											reader.seekToOffset(_index * dataSize);
											bicubic_double = reader.readDouble();
										}

									    p = p + bicubic_double * cubic(x - u, a);
									}

									q = q + p * cubic(y - v, a);
								}

								interpolatedDouble = q;
								break;
								
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

							float[] floats = new float[4];
							floats[0] = reader.readFloat();
							if(reader.getPos() < readerSize){					
								floats[1] = reader.readFloat();
							}else{
								floats[1] = floats[0];
							}

							if(reader.getPos() + (srcWidth * dataSize - 2 * dataSize) < readerSize){
								reader.seekToOffset(reader.getPos() + (srcWidth * dataSize - 2 * dataSize)); 
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

							float interpolatedFloat = 0;

							switch(method){
							case BICUBIC:
								
								final double a = 0.0d;
								
								Coordinate coord = new Coordinate(x_ratio * j,y_ratio * i);

								final double _x = coord.x;
								final double _y = coord.y;
								
								final int x0 = (int) Math.floor(_x);	//use floor to handle negative coordinates too
								final int y0 = (int) Math.floor(_y);
								float bicubic_float = 0;

								double q = 0;
								for (int _j = 0; _j < 4; _j++) {
									final int v = y0 - 1 + _j;
									double  p = 0;
									
									for (int _i = 0; _i < 4; _i++) {
										final int u = x0 - 1 + _i;
										final int _index = v * srcWidth + u;
										
										if( v < 0){
											if(u >= 0){
												reader.seekToOffset(u * dataSize);
												bicubic_float = reader.readFloat();
											}else{
												reader.seekToOffset(0 * dataSize);
												bicubic_float = reader.readFloat();
											}
										}else if(u < 0){
											reader.seekToOffset(v * srcWidth * dataSize);
											bicubic_float = reader.readFloat();
										}else if(v >= srcWidth){
											if(u < srcWidth){
												reader.seekToOffset((srcWidth - 1 * srcWidth + u) * dataSize);
												bicubic_float = reader.readFloat();							
											}else{
												reader.seekToOffset((srcWidth - 1 * srcWidth + (srcWidth - 1)) * dataSize);
												bicubic_float = reader.readFloat();													
											}
										}else if(u >= srcWidth){
											reader.seekToOffset((v * srcWidth + (srcWidth - 1)) * dataSize);
											bicubic_float = reader.readFloat();	
										}else{
											reader.seekToOffset(_index * dataSize);
											bicubic_float = reader.readFloat();
										}

									    p = p + bicubic_float * cubic(x - u, a);
									}

									q = q + p * cubic(y - v, a);
								}

								interpolatedFloat = (float) q;
								break;
								
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

							int[] ints = new int[4];
							ints[0] = reader.readInt();
							if(reader.getPos() < readerSize){					
								ints[1] = reader.readInt();
							}else{
								ints[1] = ints[0];
							}

							if(reader.getPos() + (srcWidth * dataSize - 2 * dataSize) < readerSize){
								reader.seekToOffset(reader.getPos() + (srcWidth * dataSize - 2 * dataSize)); 
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

							int interpolatedInt = 0;
							switch(method){
							case BICUBIC:
								
								final double a = 0.0d;
								
								Coordinate coord = new Coordinate(x_ratio * j,y_ratio * i);

								final double _x = coord.x;
								final double _y = coord.y;
								
								final int x0 = (int) Math.floor(_x);	//use floor to handle negative coordinates too
								final int y0 = (int) Math.floor(_y);
								float bicubic_int = 0;

								double q = 0;
								for (int _j = 0; _j < 4; _j++) {
									final int v = y0 - 1 + _j;
									double  p = 0;
									
									for (int _i = 0; _i < 4; _i++) {
										final int u = x0 - 1 + _i;
										final int _index = v * srcWidth + u;
										
										if( v < 0){
											if(u >= 0){
												reader.seekToOffset(u * dataSize);
												bicubic_int = reader.readInt();
											}else{
												reader.seekToOffset(0 * dataSize);
												bicubic_int = reader.readInt();
											}
										}else if(u < 0){
											reader.seekToOffset(v * srcWidth * dataSize);
											bicubic_int = reader.readInt();
										}else if(v >= srcWidth){
											if(u < srcWidth){
												reader.seekToOffset((srcWidth - 1 * srcWidth + u) * dataSize);
												bicubic_int = reader.readInt();							
											}else{
												reader.seekToOffset((srcWidth - 1 * srcWidth + (srcWidth - 1)) * dataSize);
												bicubic_int = reader.readInt();													
											}
										}else if(u >= srcWidth){
											reader.seekToOffset((v * srcWidth + (srcWidth - 1)) * dataSize);
											bicubic_int = reader.readInt();	
										}else{
											reader.seekToOffset(_index * dataSize);
											bicubic_int = reader.readInt();
										}

									    p = p + bicubic_int * cubic(x - u, a);
									}

									q = q + p * cubic(y - v, a);
								}

								interpolatedInt = (int) q;
								break;
								
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

							long[] longs = new long[4];
							longs[0] = reader.readLong();
							if(reader.getPos() < readerSize){					
								longs[1] = reader.readLong();
							}else{
								longs[1] = longs[0];
							}

							if(reader.getPos() + (srcWidth * dataSize - 2 * dataSize) < readerSize){
								reader.seekToOffset(reader.getPos() + (srcWidth * dataSize - 2 * dataSize)); 
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

							long interpolatedLong = 0;

							switch(method){
							case BICUBIC:
								
								final double a = 0.0d;
								
								Coordinate coord = new Coordinate(x_ratio * j,y_ratio * i);

								final double _x = coord.x;
								final double _y = coord.y;
								
								final int x0 = (int) Math.floor(_x);	//use floor to handle negative coordinates too
								final int y0 = (int) Math.floor(_y);
								long bicubic_long = 0;

								double q = 0;
								for (int _j = 0; _j < 4; _j++) {
									final int v = y0 - 1 + _j;
									double  p = 0;
									
									for (int _i = 0; _i < 4; _i++) {
										final int u = x0 - 1 + _i;
										final int _index = v * srcWidth + u;
										
										if( v < 0){
											if(u >= 0){
												reader.seekToOffset(u * dataSize);
												bicubic_long = reader.readLong();
											}else{
												reader.seekToOffset(0 * dataSize);
												bicubic_long = reader.readLong();
											}
										}else if(u < 0){
											reader.seekToOffset(v * srcWidth * dataSize);
											bicubic_long = reader.readLong();
										}else if(v >= srcWidth){
											if(u < srcWidth){
												reader.seekToOffset((srcWidth - 1 * srcWidth + u) * dataSize);
												bicubic_long = reader.readLong();							
											}else{
												reader.seekToOffset((srcWidth - 1 * srcWidth + (srcWidth - 1)) * dataSize);
												bicubic_long = reader.readLong();													
											}
										}else if(u >= srcWidth){
											reader.seekToOffset((v * srcWidth + (srcWidth - 1)) * dataSize);
											bicubic_long = reader.readLong();	
										}else{
											reader.seekToOffset(_index * dataSize);
											bicubic_long = reader.readLong();
										}

									    p = p + bicubic_long * cubic(x - u, a);
									}

									q = q + p * cubic(y - v, a);
								}

								interpolatedLong = (long) q;
								break;
								
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

							short[] shorts = new short[4];
							shorts[0] = reader.readShort();
							if(reader.getPos() < readerSize){					
								shorts[1] = reader.readShort();
							}else{
								shorts[1] = shorts[0];
							}

							if(reader.getPos() + (srcWidth * dataSize - 2 * dataSize) < readerSize){
								//reader is at index + 2 * dataSize
								reader.seekToOffset(reader.getPos() + (srcWidth * dataSize - 2 * dataSize)); 
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
							short interpolatedShort= 0;
							switch(method){
							case BICUBIC:								

								final double a = 0.0d;
								
								Coordinate coord = new Coordinate(x_ratio * j,y_ratio * i);

								final double _x = coord.x;
								final double _y = coord.y;
								
								final int x0 = (int) Math.floor(_x);	//use floor to handle negative coordinates too
								final int y0 = (int) Math.floor(_y);
								short bicubic_short = 0;

								double q = 0;
								for (int _j = 0; _j < 4; _j++) {
									final int v = y0 - 1 + _j;
									double  p = 0;
									
									for (int _i = 0; _i < 4; _i++) {
										final int u = x0 - 1 + _i;
										final int _index = v * srcWidth + u;
										
										if( v < 0){
											if(u >= 0){
												reader.seekToOffset(u * dataSize);
												bicubic_short = reader.readShort();
											}else{
												reader.seekToOffset(0 * dataSize);
												bicubic_short = reader.readShort();
											}
										}else if(u < 0){
											reader.seekToOffset(v * srcWidth * dataSize);
											bicubic_short = reader.readShort();
										}else if(v >= srcWidth){
											if(u < srcWidth){
												reader.seekToOffset((srcWidth - 1 * srcWidth + u) * dataSize);
												bicubic_short = reader.readShort();							
											}else{
												reader.seekToOffset((srcWidth - 1 * srcWidth + (srcWidth - 1)) * dataSize);
												bicubic_short = reader.readShort();													
											}
										}else if(u >= srcWidth){
											reader.seekToOffset((v * srcWidth + (srcWidth - 1)) * dataSize);
											bicubic_short = reader.readShort();	
										}else{
											reader.seekToOffset(_index * dataSize);
											bicubic_short = reader.readShort();
										}

									    p = p + bicubic_short * cubic(x - u, a);
									}

									q = q + p * cubic(y - v, a);
								}

								interpolatedShort = (short) q;
								break;
								
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
			
			raster.setDimension(dstDimension);
			
			raster.setData(buffer);
			
		}catch(IOException e){
			Log.e(MRawResampler.class.getSimpleName(), "Error reading raster values",e);
		}

	}
	
	private double cubic(double r, double a) {
		if (r < 0) r = -r;
		double w = 0;
		if (r < 1) 
			w = (a+2)*r*r*r - (a+3)*r*r + 1;
		else if (r < 2) 
			w = a*r*r*r - 5*a*r*r + 8*a*r - 4*a;
		return w;
	}
}

