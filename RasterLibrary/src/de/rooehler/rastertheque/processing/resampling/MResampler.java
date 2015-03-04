package de.rooehler.rastertheque.processing.resampling;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

import android.graphics.Rect;
import android.util.Log;

import com.vividsolutions.jts.geom.Coordinate;

import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.core.util.ByteBufferReader;
import de.rooehler.rastertheque.processing.Interpolation.ResampleMethod;
import de.rooehler.rastertheque.processing.RasterOp;
import de.rooehler.rastertheque.util.Hints;
import de.rooehler.rastertheque.util.Hints.Key;
import de.rooehler.rastertheque.util.ProgressListener;
/**
 * Implementation of the Resampling operation
 * following algorithms and samples found in :
 * 
 * Burger, Wilhelm & Burge, Mark James :
 * Digital Image Processing. Springer, 2008
 * 
 * Currently the interpolation methods
 * 
 * NEARESTNEIGHBOUR
 * BILINEAR
 * BICUBIC
 * 
 * are realised
 * 
 * @author Robert Oehler
 *
 */
public class MResampler extends Resampler implements RasterOp, Serializable  {


	private static final long serialVersionUID = -5891230160742468189L;

	/**
	 * executes the operation on the @param raster according to the @params
	 * using the optional @param hints
	 * the  @listener reports the progress in terms of percent (1-99)
	 */
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

		final int srcWidth  = raster.getDimension().right - raster.getDimension().left;
		final int srcHeight = raster.getDimension().bottom - raster.getDimension().top;

		final int dstWidth = (int) (srcWidth * scaleX);
		final int dstHeight = (int) (srcHeight * scaleY);

		final ByteBufferReader reader = new ByteBufferReader(raster.getData().array(), ByteOrder.nativeOrder());

		int x, y, index;
		float x_ratio = ((float) (srcWidth - 1)) / dstWidth;
		float y_ratio = ((float) (srcHeight - 1)) / dstHeight;
		float x_diff, y_diff;

		final int readerSize = ((int)srcWidth) * ((int)srcHeight) * raster.getBands().size() * raster.getBands().get(0).datatype().size();

		final int newBufferSize = ((int)dstWidth) * ((int)dstHeight) * raster.getBands().size() * raster.getBands().get(0).datatype().size();

		final int bandAmount = raster.getBands().size();
		final float onePercent = bandAmount * dstHeight * dstWidth / 100f;
		float current = onePercent;
		int percent = 1;
		try{

			final ByteBuffer buffer = ByteBuffer.allocate(newBufferSize);
			buffer.order(ByteOrder.nativeOrder()); 

			for(int h = 0; h < bandAmount; h++){
				final int dataSize = raster.getBands().get(h).datatype().size();
				final int bandIndex = h * srcHeight * srcWidth;

				for (int i = 0; i < dstHeight; i++) {
					for (int j = 0; j < dstWidth; j++) {

						// src pix coords
						x = (int) (x_ratio * j);
						y = (int) (y_ratio * i);

						// offsets from the current pos to the pos in the new image
						x_diff = (x_ratio * j) - x;
						y_diff = (y_ratio * i) - y;

						// current pos
						index = y * srcWidth + x;

						if(index * bandAmount > current){
							if(listener != null){								
								listener.onProgress(percent);
							}
							current += onePercent;
							percent++;
						}

						reader.seekToOffset(index * dataSize + bandIndex);

						final int nearestX = (int) Math.rint(x + x_diff);
						final int nearestY = (int) Math.rint(y + y_diff);

						switch(raster.getBands().get(h).datatype()) {

						case BYTE:

							byte[] bytes = getByteNeighbours(reader, readerSize, dataSize, srcWidth);					
							byte interpolatedByte = 0;

							switch (method) {
							case NEARESTNEIGHBOUR:
								interpolatedByte = interpolateBytesNN(bytes, nearestX, nearestY, x, y);
								break;
							case BILINEAR:
								interpolatedByte = interpolateBytesBilinear(bytes, x_diff, y_diff);
								break;
							case BICUBIC:
								Coordinate coord = new Coordinate(x_ratio * j,y_ratio * i);
								interpolatedByte = interpolateBytesBicubic(bytes, coord, reader, x, y, srcWidth, srcHeight, dataSize);
								break;

							default:
								break;
							}
							//Log.d(MRawResampler.class.getSimpleName(), String.format("x %d y %d resampled %d", j, i , interpolatedByte));
							buffer.put(interpolatedByte);
							break;
						case CHAR:

							char[] chars = getCharNeighbours(reader,readerSize,dataSize, srcWidth);
							char interpolatedValue = 0;

							switch(method){
							case BICUBIC:
								Coordinate coord = new Coordinate(x_ratio * j,y_ratio * i);
								interpolatedValue = interpolateCharsBicubic(chars, coord, reader, x, y, srcWidth, srcHeight, dataSize);				
								break;						
							case BILINEAR:
								interpolatedValue = interpolateCharsBilinear(chars, x_diff, y_diff);
								break;						
							case NEARESTNEIGHBOUR:					
								interpolatedValue = interpolateCharsNN(chars, nearestX, nearestY, x, y);
								break;
							}
							buffer.putChar(interpolatedValue);
							break;
						case DOUBLE:

							double[] doubles = getDoubleNeighbours(reader,readerSize,dataSize,srcWidth);
							double interpolatedDouble = 0;

							switch(method){
							case BICUBIC:								
								Coordinate coord = new Coordinate(x_ratio * j,y_ratio * i);
								interpolatedDouble = interpolateDoublesBicubic(doubles, coord, reader, x, y, srcWidth, srcHeight, dataSize);
								break;							
							case BILINEAR:
								// Yb = Ab(1-w)(1-h) + Bb(w)(1-h) + Cb(h)(1-w) + Db(wh)
								interpolatedDouble = interpolateDoublesBilinear(doubles, x_diff, y_diff);
								break;
							case NEARESTNEIGHBOUR:
								interpolatedDouble = interpolateDoublesNN(doubles, nearestX, nearestY, x, y);
								break;						
							}
							buffer.putDouble(interpolatedDouble);
							break;

						case FLOAT:

							float[] floats = getFloatNeighbours(reader,readerSize,dataSize,srcWidth);
							float interpolatedFloat = 0;

							switch(method){
							case BICUBIC:						
								Coordinate coord = new Coordinate(x_ratio * j,y_ratio * i);
								interpolatedFloat = interpolateFloatsBicubic(floats, coord, reader, x, y, srcWidth, srcHeight, dataSize);
								break;					
							case BILINEAR:
								interpolatedFloat = interpolateFloatsBilinear(floats, x_diff, y_diff);
								break;
							case NEARESTNEIGHBOUR:	
								interpolatedFloat = interpolateFloatsNN(floats, nearestX, nearestY, x, y);
								break;
							}
							buffer.putFloat(interpolatedFloat);
							break;
						case INT:

							int[] ints = getIntNeighbours(reader,readerSize,dataSize,srcWidth);
							int interpolatedInt = 0;

							switch(method){
							case BICUBIC:							
								Coordinate coord = new Coordinate(x_ratio * j,y_ratio * i);
								interpolatedInt = interpolateIntsBicubic(ints, coord, reader, x, y, srcWidth, srcHeight, dataSize);
								break;					
							case BILINEAR:
								interpolatedInt = interpolateIntsBilinear(ints, x_diff, y_diff);
								break;
							case NEARESTNEIGHBOUR:		
								interpolatedInt = interpolateIntsNN(ints, nearestX, nearestY, x, y);
								break;
							}
							buffer.putInt(interpolatedInt);
							break;
						case LONG:

							long[] longs = getLongNeighbours(reader,readerSize,dataSize,srcWidth);
							long interpolatedLong = 0;

							switch(method){
							case BICUBIC:					
								Coordinate coord = new Coordinate(x_ratio * j,y_ratio * i);
								interpolatedLong = interpolateLongsBicubic(longs, coord, reader, x, y, srcWidth, srcHeight, dataSize);
								break;							
							case BILINEAR:
								interpolatedLong = interpolateLongsBilinear(longs, x_diff, y_diff);
								break;						
							case NEARESTNEIGHBOUR:							
								interpolatedLong = interpolateLongsNN(longs, nearestX, nearestY, x, y);
								break;
							}
							buffer.putLong(interpolatedLong);
							break;
						case SHORT:

							short[] shorts = getShortNeighbours(reader,readerSize,dataSize,srcWidth);
							short interpolatedShort= 0;

							switch(method){
							case BICUBIC:								
								Coordinate coord = new Coordinate(x_ratio * j,y_ratio * i);
								interpolatedShort = interpolateShortsBicubic(shorts, coord, reader, x, y, srcWidth, srcHeight, dataSize);
								break;							
							case BILINEAR:
								interpolatedShort = interpolateShortsBilinear(shorts, x_diff, y_diff);
								break;							
							case NEARESTNEIGHBOUR:							
								interpolatedShort = interpolateShortsNN(shorts, nearestX, nearestY, x, y);
								break;
							}
							buffer.putShort(interpolatedShort);
							break;
						}
					}
				}
			}

			raster.setDimension(new Rect(0, 0, dstWidth, dstHeight));

			raster.setData(buffer);

		}catch(IOException e){
			Log.e(MResampler.class.getSimpleName(), "Error reading raster values",e);
		}
	}

	/////////////////**************SHORTS*************/////////////////////////////	

	public static short[] getShortNeighbours(ByteBufferReader reader, int readerSize,int dataSize, int srcWidth) throws IOException {
		short[] shorts = new short[4];
		shorts[0] = reader.readShort();
		if(reader.getPos() < readerSize){					
			shorts[1] = reader.readShort();
		}else{
			shorts[1] = shorts[0];
		}

		if(reader.getPos() + (srcWidth * dataSize - 2 * dataSize) < readerSize){
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
		return shorts;
	}

	public static short interpolateShortsBicubic(short[] shorts, Coordinate coord, ByteBufferReader reader, int x, int y, int srcWidth, int srcHeight, int dataSize) throws IOException{

		final double a = 0.0d;
		final double _x = coord.x;
		final double _y = coord.y;

		final int x0 = (int) Math.floor(_x);
		final int y0 = (int) Math.floor(_y);
		short bicubic_short = 0;

		double q = 0;
		for (int _j = 0; _j < 4; _j++) {
			final int v = y0 - 1 + _j;
			double  p = 0;

			for (int _i = 0; _i < 4; _i++) {
				final int u = x0 - 1 + _i;
				final int _index = v * srcWidth + u;

				seekTo(reader, _index, dataSize, u, v, srcWidth, srcHeight);
				bicubic_short = reader.readShort();

				p = p + bicubic_short * cubic(x - u, a);
			}

			q = q + p * cubic(y - v, a);
		}

		return (short) q;

	}

	public static short interpolateShortsBilinear(short[] shorts, float x_diff, float y_diff){

		return (short)  Math.rint((shorts[0] * (1 - x_diff) * (1 - y_diff) +
				shorts[1] * (x_diff) * (1 - y_diff) +
				shorts[2] * (y_diff) * (1 - x_diff) +
				shorts[3] * (x_diff * y_diff)));

	}

	public static short interpolateShortsNN(short[] shorts, int nearestX, int nearestY, int x, int y){

		if(nearestX == x && nearestY == y){
			return shorts[0];
		}else if(nearestX == x && nearestY == (y + 1)){
			return shorts[2];
		}else if(nearestX == (x + 1) && nearestY ==  y){
			return shorts[1];
		}else if(nearestX == (x + 1) && nearestY == (y + 1)){
			return shorts[3];
		}
		return 0;
	}

	/////////////////**************LONGS*************/////////////////////////////

	public static long[] getLongNeighbours(ByteBufferReader reader, int readerSize,int dataSize, int srcWidth)throws IOException {
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
		return longs;
	}

	public static long interpolateLongsBicubic(long[] longs, Coordinate coord, ByteBufferReader reader, int x, int y, int srcWidth, int srcHeight, int dataSize) throws IOException{

		final double a = 0.0d;
		final double _x = coord.x;
		final double _y = coord.y;

		final int x0 = (int) Math.floor(_x);
		final int y0 = (int) Math.floor(_y);
		long bicubic_long = 0;

		double q = 0;
		for (int _j = 0; _j < 4; _j++) {
			final int v = y0 - 1 + _j;
			double  p = 0;

			for (int _i = 0; _i < 4; _i++) {
				final int u = x0 - 1 + _i;
				final int _index = v * srcWidth + u;

				seekTo(reader, _index, dataSize, u, v, srcWidth, srcHeight);
				bicubic_long = reader.readLong();

				p = p + bicubic_long * cubic(x - u, a);
			}

			q = q + p * cubic(y - v, a);
		}

		return (long) q;

	}

	public static long interpolateLongsBilinear(long[] longs, float x_diff, float y_diff){

		return 	(long)  Math.rint((longs[0] * (1 - x_diff) * (1 - y_diff) +
				longs[1] * (x_diff) * (1 - y_diff) +
				longs[2] * (y_diff) * (1 - x_diff) +
				longs[3] * (x_diff * y_diff)));

	}
	public static long interpolateLongsNN(long[] longs, int nearestX, int nearestY, int x, int y){

		if(nearestX == x && nearestY == y){
			return longs[0];
		}else if(nearestX == x && nearestY == (y + 1)){
			return longs[2];
		}else if(nearestX == (x + 1) && nearestY ==  y){
			return longs[1];
		}else if(nearestX == (x + 1) && nearestY == (y + 1)){
			return longs[3];
		}
		return 0;

	}

	/////////////////**************INTS*************/////////////////////////////

	public static int[] getIntNeighbours(ByteBufferReader reader, int readerSize,int dataSize, int srcWidth) throws IOException{
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
		return ints;
	}


	public static int interpolateIntsBicubic(int[] ints, Coordinate coord, ByteBufferReader reader, int x, int y, int srcWidth, int srcHeight, int dataSize) throws IOException{

		final double a = 0.0d;
		final double _x = coord.x;
		final double _y = coord.y;

		final int x0 = (int) Math.floor(_x);
		final int y0 = (int) Math.floor(_y);
		float bicubic_int = 0;

		double q = 0;
		for (int _j = 0; _j < 4; _j++) {
			final int v = y0 - 1 + _j;
			double  p = 0;

			for (int _i = 0; _i < 4; _i++) {
				final int u = x0 - 1 + _i;
				final int _index = v * srcWidth + u;

				seekTo(reader, _index, dataSize, u, v, srcWidth, srcHeight);
				bicubic_int = reader.readInt();

				p = p + bicubic_int * cubic(x - u, a);
			}

			q = q + p * cubic(y - v, a);
		}

		return (int) q;

	}

	public static int interpolateIntsBilinear(int[] ints, float x_diff, float y_diff){

		return 	 (int)  Math.rint((ints[0] * (1 - x_diff) * (1 - y_diff) +
				ints[1] * (x_diff) * (1 - y_diff) +
				ints[2] * (y_diff) * (1 - x_diff) +
				ints[3] * (x_diff * y_diff)));

	}

	public static int interpolateIntsNN(int[] ints, int nearestX, int nearestY, int x, int y){

		if(nearestX == x && nearestY == y){
			return ints[0];
		}else if(nearestX == x && nearestY == (y + 1)){
			return ints[2];
		}else if(nearestX == (x + 1) && nearestY ==  y){
			return ints[1];
		}else if(nearestX == (x + 1) && nearestY == (y + 1)){
			return ints[3];
		}
		return 0;

	}

	/////////////////**************FLOATS*************/////////////////////////////

	public static float[] getFloatNeighbours(ByteBufferReader reader, int readerSize,	int dataSize, int srcWidth) throws IOException{
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
		return floats;
	}

	public static float interpolateFloatsBicubic(float[] floats, Coordinate coord, ByteBufferReader reader, int x, int y, int srcWidth, int srcHeight, int dataSize) throws IOException{

		final double a = 0.0d;
		final double _x = coord.x;
		final double _y = coord.y;

		final int x0 = (int) Math.floor(_x);
		final int y0 = (int) Math.floor(_y);
		float bicubic_float = 0;

		double q = 0;
		for (int _j = 0; _j < 4; _j++) {
			final int v = y0 - 1 + _j;
			double  p = 0;

			for (int _i = 0; _i < 4; _i++) {
				final int u = x0 - 1 + _i;
				final int _index = v * srcWidth + u;

				seekTo(reader, _index, dataSize, u, v, srcWidth, srcHeight);
				bicubic_float = reader.readFloat();

				p = p + bicubic_float * cubic(x - u, a);
			}

			q = q + p * cubic(y - v, a);
		}

		return (float) q;
	}

	public static float interpolateFloatsBilinear(float[] floats, float x_diff, float y_diff){

		return 	floats[0] * (1 - x_diff) * (1 - y_diff) +
				floats[1] * (x_diff) * (1 - y_diff) +
				floats[2] * (y_diff) * (1 - x_diff) +
				floats[3] * (x_diff * y_diff);
	}

	public static float interpolateFloatsNN(float[] floats, int nearestX, int nearestY, int x, int y){

		if(nearestX == x && nearestY == y){
			return floats[0];
		}else if(nearestX == x && nearestY == (y + 1)){
			return floats[2];
		}else if(nearestX == (x + 1) && nearestY ==  y){
			return floats[1];
		}else if(nearestX == (x + 1) && nearestY == (y + 1)){
			return floats[3];
		}
		return 0;

	}
	/////////////////**************DOUBLES*************/////////////////////////////

	public static double[] getDoubleNeighbours(ByteBufferReader reader,int readerSize, int dataSize, int srcWidth) throws IOException {
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
		return doubles;
	}

	public static double interpolateDoublesBilinear(double[] doubles, float x_diff, float y_diff){

		return 	doubles[0] * (1 - x_diff) * (1 - y_diff) +
				doubles[1] * (x_diff) * (1 - y_diff) +
				doubles[2] * (y_diff) * (1 - x_diff) +
				doubles[3] * (x_diff * y_diff);

	}

	public static double interpolateDoublesNN(double[] doubles, int nearestX, int nearestY, int x, int y){

		if(nearestX == x && nearestY == y){
			return doubles[0];
		}else if(nearestX == x && nearestY == (y + 1)){
			return doubles[2];
		}else if(nearestX == (x + 1) && nearestY ==  y){
			return doubles[1];
		}else if(nearestX == (x + 1) && nearestY == (y + 1)){
			return doubles[3];
		}
		return 0;

	}

	public static double interpolateDoublesBicubic(double[] doubles, Coordinate coord, ByteBufferReader reader, int x, int y, int srcWidth, int srcHeight, int dataSize) throws IOException{
		final double a = 0.0d;
		final double _x = coord.x;
		final double _y = coord.y;

		final int x0 = (int) Math.floor(_x);
		final int y0 = (int) Math.floor(_y);
		double bicubic_double = 0;

		double q = 0;
		for (int _j = 0; _j < 4; _j++) {
			final int v = y0 - 1 + _j;
			double  p = 0;

			for (int _i = 0; _i < 4; _i++) {
				final int u = x0 - 1 + _i;
				final int _index = v * srcWidth + u;

				seekTo(reader, _index, dataSize, u, v, srcWidth, srcHeight);
				bicubic_double = reader.readDouble();

				p = p + bicubic_double * cubic(x - u, a);
			}

			q = q + p * cubic(y - v, a);
		}
		return q;
	}

	/////////////////**************CHARS*************/////////////////////////////

	public static char interpolateCharsBicubic(char[] bytes, Coordinate coord, ByteBufferReader reader, int x, int y, int srcWidth, int srcHeight, int dataSize) throws IOException{
		final double a = 0.0d;
		final double _x = coord.x;
		final double _y = coord.y;

		final int x0 = (int) Math.floor(_x);
		final int y0 = (int) Math.floor(_y);
		char bicubic_char = 0;

		double q = 0;
		for (int _j = 0; _j < 4; _j++) {
			final int v = y0 - 1 + _j;
			double  p = 0;

			for (int _i = 0; _i < 4; _i++) {
				final int u = x0 - 1 + _i;
				final int _index = v * srcWidth + u;

				seekTo(reader, _index, dataSize, u, v, srcWidth, srcHeight);
				bicubic_char = reader.readChar();

				p = p + bicubic_char * cubic(x - u, a);
			}

			q = q + p * cubic(y - v, a);
		}

		return  (char) q;
	}

	public static char[] getCharNeighbours(ByteBufferReader reader, int readerSize,int dataSize, int srcWidth) throws IOException{
		char[] chars =  new char[4];
		chars[0] = reader.readChar();
		if(reader.getPos() < readerSize){					
			chars[1] = reader.readChar();
		}else{
			chars[1] = chars[0];
		}

		if(reader.getPos() + (srcWidth * dataSize  - 2 * dataSize) < readerSize){
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
		return chars;
	}

	public static char interpolateCharsBilinear(char[] chars, float x_diff, float y_diff){

		return (char)  Math.rint((chars[0] * (1 - x_diff) * (1 - y_diff) +
				chars[1] * (x_diff) * (1 - y_diff) +
				chars[2] * (y_diff) * (1 - x_diff) +
				chars[3] * (x_diff * y_diff)));

	}

	public static char interpolateCharsNN(char[] chars, int nearestX, int nearestY, int x, int y){

		if(nearestX == x && nearestY == y){
			return chars[0];
		}else if(nearestX == x && nearestY == (y + 1)){
			return chars[2];
		}else if(nearestX == (x + 1) && nearestY ==  y){
			return chars[1];
		}else if(nearestX == (x + 1) && nearestY == (y + 1)){
			return chars[3];
		}

		return 0;

	}

	/////////////////**************BYTES*************/////////////////////////////

	public static byte[] getByteNeighbours(ByteBufferReader reader, int readerSize, int dataSize, int srcWidth) throws IOException {
		byte[] bytes = new byte[4];
		bytes[0] = reader.readByte();
		if(reader.getPos() < readerSize){					
			bytes[1] = reader.readByte();
		}else{
			bytes[1] = bytes[0];
		}

		if(reader.getPos() + (srcWidth * dataSize - 2 * dataSize) < readerSize){
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
		return bytes;
	}

	public static byte interpolateBytesBicubic(byte[] bytes, Coordinate coord, ByteBufferReader reader, int x, int y, int srcWidth, int srcHeight, int dataSize) throws IOException{
		final double a = 0.0d;

		final double _x = coord.x;
		final double _y = coord.y;

		final int x0 = (int) Math.floor(_x);
		final int y0 = (int) Math.floor(_y);
		byte bicubic_byte = 0;

		double q = 0;
		for (int _j = 0; _j < 4; _j++) {
			final int v = y0 - 1 + _j;
			double  p = 0;

			for (int _i = 0; _i < 4; _i++) {
				final int u = x0 - 1 + _i;
				final int _index = v * srcWidth + u;

				seekTo(reader, _index, dataSize, u, v, srcWidth, srcHeight);
				bicubic_byte = reader.readByte();

				p = p + bicubic_byte * cubic(x - u, a);
			}

			q = q + p * cubic(y - v, a);
		}

		return (byte) q;
	}

	public static byte interpolateBytesBilinear(byte[] bytes, float x_diff, float y_diff){

		return 	(byte) Math.rint((bytes[0] * (1 - x_diff) * (1 - y_diff) +
				bytes[1] * (x_diff) * (1 - y_diff) +
				bytes[2] * (y_diff) * (1 - x_diff) +
				bytes[3] * (x_diff * y_diff)));
	}

	public static byte interpolateBytesNN(byte[] bytes, int nearestX, int nearestY, int x, int y){

		if(nearestX == x && nearestY == y){
			return bytes[0];
		}else if(nearestX == x && nearestY == (y + 1)){
			return bytes[2];
		}else if(nearestX == (x + 1) && nearestY ==  y){
			return bytes[1];
		}else if(nearestX == (x + 1) && nearestY == (y + 1)){
			return bytes[3];
		}else{
			throw new IllegalArgumentException(String.format("wrong nx %d ny %d x %d y %d", nearestX,nearestY,x,y));
		}

	}

	/////////////////**************UTIL*************/////////////////////////////

	private static void seekTo(ByteBufferReader reader, int _index, int dataSize, int u, int v, int srcWidth, int srcHeight){
		if( v < 0){
			if(u >= 0){
				reader.seekToOffset(u * dataSize);
			}else{
				reader.seekToOffset(0 * dataSize);
			}
		}else if(u < 0){
			if(v >= srcWidth){
				reader.seekToOffset((v - 1) * srcWidth * dataSize);
			}else{												
				reader.seekToOffset(v * srcWidth * dataSize);
			}
		}else if(v >= srcWidth){
			if(u < srcWidth){
				reader.seekToOffset((srcWidth - 1 * srcWidth + u) * dataSize);						
			}else{
				reader.seekToOffset((srcWidth - 1 * srcWidth + (srcWidth - 1)) * dataSize);
			}												
		}else if(u >= srcWidth){
			reader.seekToOffset((v * srcWidth + (srcWidth - 1)) * dataSize);
		}else{
			reader.seekToOffset(_index * dataSize);
		}
	}

	private static  double cubic(double r, double a) {
		if (r < 0) r = -r;
		double w = 0;
		if (r < 1) 
			w = (a+2)*r*r*r - (a+3)*r*r + 1;
		else if (r < 2) 
			w = a*r*r*r - 5*a*r*r + 8*a*r - 4*a;
		return w;
	}



	@Override
	public Priority getPriority() {

		return Priority.NORMAL;
	}

}

