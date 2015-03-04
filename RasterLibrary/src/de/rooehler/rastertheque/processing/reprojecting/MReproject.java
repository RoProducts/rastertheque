package de.rooehler.rastertheque.processing.reprojecting;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

import org.osgeo.proj4j.CoordinateReferenceSystem;
import org.osgeo.proj4j.CoordinateTransform;
import org.osgeo.proj4j.ProjCoordinate;

import android.util.Log;

import com.vividsolutions.jts.geom.Coordinate;

import de.rooehler.rastertheque.core.DataType;
import de.rooehler.rastertheque.core.NoData;
import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.core.util.ByteBufferReader;
import de.rooehler.rastertheque.core.util.ReferencedEnvelope;
import de.rooehler.rastertheque.processing.Interpolation.ResampleMethod;
import de.rooehler.rastertheque.processing.RasterOp;
import de.rooehler.rastertheque.processing.resampling.MResampler;
import de.rooehler.rastertheque.proj.Proj;
import de.rooehler.rastertheque.util.Hints;
import de.rooehler.rastertheque.util.Hints.Key;
import de.rooehler.rastertheque.util.ProgressListener;
/**
 * Implementation of the Reproject operation 
 * in a "manual" way, step by step
 * 
 * @author Robert Oehler
 *
 */
public class MReproject extends Reproject implements RasterOp {

	/**
	 * executes the operation on the @param raster according to the @params
	 * using the optional @param hints
	 * the  @listener reports the progress in terms of percent (1-99)
	 */
	@Override
	public void execute(Raster raster, Map<Key, Serializable> params,Hints hints, ProgressListener listener) {

		// src projection		
		CoordinateReferenceSystem src_crs = raster.getCRS();
		// target projection
		CoordinateReferenceSystem dst_crs = null;

		if(params != null && params.containsKey(Reproject.KEY_REPROJECT_TARGET_CRS)){
			String wkt = (String) params.get(Reproject.KEY_REPROJECT_TARGET_CRS);
			//if this is a proj parameter string convert to wkt
			if(wkt != null && wkt.startsWith("+proj")){
				wkt = Proj.proj2wkt(wkt);
			}
			//try to create a CoordinateReferenceSystem from it
			if(wkt!= null){				
				dst_crs = Proj.crs(wkt);
			}else{
				Log.e(MReproject.class.getSimpleName(), "no proj params String provided as dst crs parameter");
				return;
			}
			
		}else if(params == null){	
			Log.e(MReproject.class.getSimpleName(), "no params provided");
			return;
		}else if(!params.containsKey(Reproject.KEY_REPROJECT_TARGET_CRS)){
			Log.e(MReproject.class.getSimpleName(), "no parameter for the target crs provided");
			return;
		}
		if(src_crs == null){
			Log.e(MReproject.class.getSimpleName(), "src raster does not have a crs, cannot reproject");
			return;	
		}
		if(dst_crs == null){		
			Log.e(MReproject.class.getSimpleName(), "invalid well-known text provided as dst crs parameter");
			return;
		}

		ResampleMethod method = ResampleMethod.BILINEAR;
		if(hints != null && hints.containsKey(Hints.KEY_INTERPOLATION)){
			method = (ResampleMethod) hints.get(Hints.KEY_INTERPOLATION);
		}
		
		final DataType dataType = raster.getBands().get(0).datatype();
		final int srcWidth  = raster.getDimension().width();
		final int srcHeight = raster.getDimension().height();

		//source envelope
		ReferencedEnvelope	src_refEnv = new ReferencedEnvelope(raster.getBoundingBox(), src_crs);

		//transform the src envelope to the target envelope using the target crs
		//densify it with 10 additional points
		ReferencedEnvelope reprojected = src_refEnv.transform(dst_crs, 10);

		//target raster resolution "how much model units are between two raster points"
		double dst_x_res = reprojected.getEnvelope().getWidth() / srcWidth;
		double dst_y_res = reprojected.getEnvelope().getHeight() / srcHeight;

		//src raster resolution -> "how much model units are between two raster points"
		double src_x_res = raster.getBoundingBox().getWidth() / srcWidth;
		double src_y_res = raster.getBoundingBox().getHeight() / srcHeight;

		//target reference coordinate
		final Coordinate dst_upperLeft = new Coordinate(reprojected.getEnvelope().getMinX(), reprojected.getEnvelope().getMaxY());
		//src reference coordinate
		final Coordinate src_upperLeft = new Coordinate(raster.getBoundingBox().getMinX(), raster.getBoundingBox().getMaxY());
		//inverse transform
		final CoordinateTransform inverseTransform = Proj.transform(dst_crs, src_crs);
		
		//the size of the buffer for one band
		final int bandSize = srcWidth * srcHeight * dataType.size();
		final int readerSize = bandSize * raster.getBands().size();
		ByteBufferReader reader = new ByteBufferReader(raster.getData().array(), ByteOrder.nativeOrder());
		ByteBuffer newBuffer = ByteBuffer.allocate(bandSize * raster.getBands().size()); 

		NoData noData = null;
		
		ProjCoordinate dst_model_pos = new ProjCoordinate(dst_upperLeft.x , dst_upperLeft.y);
		Coordinate src_model_coord = new Coordinate();
		
		final int bandAmount = raster.getBands().size();
		final float onePercent = bandAmount * srcHeight * srcWidth / 100f;
		float current = onePercent;
		int percent = 1;
		
		for(int i = 0; i < bandAmount; i++){
			//determine the nodata for each band
			noData = raster.getBands().get(i).nodata();
			//if not available, use the datatypes min value
			if(noData == NoData.NONE){
				noData = NoData.noDataForDataType(dataType);
			}
			final int dataSize = raster.getBands().get(i).datatype().size();

			for(int y = 0; y < srcHeight; y++){
				for(int x = 0; x < srcWidth; x++){

					//calculate the destination model coordinate within the destination bounds
					dst_model_pos.x = dst_upperLeft.x + x * dst_x_res;
					dst_model_pos.y = dst_upperLeft.y - y * dst_y_res;
					
					//transform it to the source model
					inverseTransform.transform(dst_model_pos, dst_model_pos);

					src_model_coord.x = dst_model_pos.x;
					src_model_coord.y = dst_model_pos.y;
					
					//progress
					if(x * y * bandAmount > current){
						if(listener != null){								
							listener.onProgress(percent);
						}
						current += onePercent;
						percent++;
					}
					
					// if the source raster contains this position
					if(raster.getBoundingBox().contains(src_model_coord)){

						//calculate src raster position
						double src_raster_x = (src_model_coord.x - src_upperLeft.x) / src_x_res;
						double src_raster_y = (src_upperLeft.y - src_model_coord.y) / src_y_res;

						//reference src raster position (upper left of neighbourhood)
						final int srcX = (int) src_raster_x;
						final int srcY = (int) src_raster_y;
						
						if(srcX >= 0 && srcX < srcWidth && srcY >= 0 && srcY < srcHeight){

							//determine the pos of this raster value within the src raster
							int bufferPos = (i * bandSize) + (srcY * srcWidth + srcX) * dataType.size();

							//move buffer to this pos			
							reader.seekToOffset(bufferPos);

							try{
								//read from src into the new buffer
								switch(dataType){
								case BYTE:
									
									byte[] bytes = MResampler.getByteNeighbours(reader, readerSize, dataSize, srcWidth);					
									byte interpolatedByte = 0;
									
									switch (method) {
									case NEARESTNEIGHBOUR:
										int nearestX = (int) Math.rint(src_raster_x);
										int nearestY = (int) Math.rint(src_raster_y);
										interpolatedByte = MResampler.interpolateBytesNN(bytes, nearestX, nearestY, srcX, srcY);
										break;
									case BILINEAR:
										float x_diff = (float) (src_raster_x - srcX);
										float y_diff = (float) (src_raster_y - srcY);
										interpolatedByte = MResampler.interpolateBytesBilinear(bytes, x_diff, y_diff);
										break;
									case BICUBIC:
										src_model_coord.x = src_raster_x;
										src_model_coord.y = src_raster_y;
										interpolatedByte = MResampler.interpolateBytesBicubic(bytes, src_model_coord, reader, srcX, srcY, srcWidth, srcHeight, dataSize);
										break;
									}
									
									newBuffer.put(interpolatedByte);
									break;
								case CHAR:
									
									char[] chars = MResampler.getCharNeighbours(reader, readerSize, dataSize, srcWidth);					
									char interpolatedChar = 0;
									
									switch (method) {
									case NEARESTNEIGHBOUR:
										int nearestX = (int) Math.rint(src_raster_x);
										int nearestY = (int) Math.rint(src_raster_y);
										interpolatedChar = MResampler.interpolateCharsNN(chars, nearestX, nearestY, srcX, srcY);
										break;
									case BILINEAR:
										float x_diff = (float) (src_raster_x - srcX);
										float y_diff = (float) (src_raster_y - srcY);
										interpolatedChar = MResampler.interpolateCharsBilinear(chars, x_diff, y_diff);
										break;
									case BICUBIC:
										src_model_coord.x = src_raster_x;
										src_model_coord.y = src_raster_y;
										interpolatedChar = MResampler.interpolateCharsBicubic(chars, src_model_coord, reader, srcX, srcY, srcWidth, srcHeight, dataSize);
										break;
									}
									
									newBuffer.putChar(interpolatedChar);
									break;
								case DOUBLE:
									double[] doubles = MResampler.getDoubleNeighbours(reader, readerSize, dataSize, srcWidth);					
									double interpolatedDouble = 0.0d;
									
									switch (method) {
									case NEARESTNEIGHBOUR:
										int nearestX = (int) Math.rint(src_raster_x);
										int nearestY = (int) Math.rint(src_raster_y);
										interpolatedDouble = MResampler.interpolateDoublesNN(doubles, nearestX, nearestY, srcX, srcY);
										break;
									case BILINEAR:
										float x_diff = (float) (src_raster_x - srcX);
										float y_diff = (float) (src_raster_y - srcY);
										interpolatedDouble = MResampler.interpolateDoublesBilinear(doubles, x_diff, y_diff);
										break;
									case BICUBIC:
										src_model_coord.x = src_raster_x;
										src_model_coord.y = src_raster_y;
										interpolatedDouble = MResampler.interpolateDoublesBicubic(doubles, src_model_coord, reader, srcX, srcY, srcWidth, srcHeight, dataSize);
										break;
									}
									
									newBuffer.putDouble(interpolatedDouble);
									break;
								case FLOAT:
									float[] floats = MResampler.getFloatNeighbours(reader, readerSize, dataSize, srcWidth);					
									float interpolatedFloat = 0.0f;
									
									switch (method) {
									case NEARESTNEIGHBOUR:
										int nearestX = (int) Math.rint(src_raster_x);
										int nearestY = (int) Math.rint(src_raster_y);
										interpolatedFloat = MResampler.interpolateFloatsNN(floats, nearestX, nearestY, srcX, srcY);
										break;
									case BILINEAR:
										float x_diff = (float) (src_raster_x - srcX);
										float y_diff = (float) (src_raster_y - srcY);
										interpolatedFloat = MResampler.interpolateFloatsBilinear(floats, x_diff, y_diff);
										break;
									case BICUBIC:
										src_model_coord.x = src_raster_x;
										src_model_coord.y = src_raster_y;
										interpolatedFloat = MResampler.interpolateFloatsBicubic(floats, src_model_coord, reader, srcX, srcY, srcWidth, srcHeight, dataSize);
										break;
									}
									
									newBuffer.putFloat(interpolatedFloat);
									break;
								case INT:
									int[] ints = MResampler.getIntNeighbours(reader, readerSize, dataSize, srcWidth);					
									int interpolatedInt = 0;
									
									switch (method) {
									case NEARESTNEIGHBOUR:
										int nearestX = (int) Math.rint(src_raster_x);
										int nearestY = (int) Math.rint(src_raster_y);
										interpolatedInt = MResampler.interpolateIntsNN(ints, nearestX, nearestY, srcX, srcY);
										break;
									case BILINEAR:
										float x_diff = (float) (src_raster_x - srcX);
										float y_diff = (float) (src_raster_y - srcY);
										interpolatedInt = MResampler.interpolateIntsBilinear(ints, x_diff, y_diff);
										break;
									case BICUBIC:
										src_model_coord.x = src_raster_x;
										src_model_coord.y = src_raster_y;
										interpolatedInt = MResampler.interpolateIntsBicubic(ints, src_model_coord, reader, srcX, srcY, srcWidth, srcHeight, dataSize);
										break;
									}
									
									newBuffer.putInt(interpolatedInt);
									break;
								case LONG:
									long[] longs= MResampler.getLongNeighbours(reader, readerSize, dataSize, srcWidth);					
									long interpolatedLong = 0l;
									
									switch (method) {
									case NEARESTNEIGHBOUR:
										int nearestX = (int) Math.rint(src_raster_x);
										int nearestY = (int) Math.rint(src_raster_y);
										interpolatedLong = MResampler.interpolateLongsNN(longs, nearestX, nearestY, srcX, srcY);
										break;
									case BILINEAR:
										float x_diff = (float) (src_raster_x - srcX);
										float y_diff = (float) (src_raster_y - srcY);
										interpolatedLong = MResampler.interpolateLongsBilinear(longs, x_diff, y_diff);
										break;
									case BICUBIC:
										src_model_coord.x = src_raster_x;
										src_model_coord.y = src_raster_y;
										interpolatedLong = MResampler.interpolateLongsBicubic(longs, src_model_coord, reader, srcX, srcY, srcWidth, srcHeight, dataSize);
										break;
									}
									
									newBuffer.putLong(interpolatedLong);
									break;
								case SHORT:
									short[] shorts = MResampler.getShortNeighbours(reader, readerSize, dataSize, srcWidth);					
									short interpolatedShort = 0;
									
									switch (method) {
									case NEARESTNEIGHBOUR:
										int nearestX = (int) Math.rint(src_raster_x);
										int nearestY = (int) Math.rint(src_raster_y);
										interpolatedShort = MResampler.interpolateShortsNN(shorts, nearestX, nearestY, srcX, srcY);
										break;
									case BILINEAR:
										float x_diff = (float) (src_raster_x - srcX);
										float y_diff = (float) (src_raster_y - srcY);
										interpolatedShort = MResampler.interpolateShortsBilinear(shorts, x_diff, y_diff);
										break;
									case BICUBIC:
										src_model_coord.x = src_raster_x;
										src_model_coord.y = src_raster_y;
										interpolatedShort = MResampler.interpolateShortsBicubic(shorts, src_model_coord, reader, srcX, srcY, srcWidth, srcHeight, dataSize);
										break;
									}
									
									newBuffer.putShort(interpolatedShort);
									break;
								default:
									break;

								}
							}catch(IOException e){
								Log.e(MReproject.class.getSimpleName(), "error reading from bytebuffered reader @ x : "+x+" y : "+ y);
							}
						}else{ //the src raster position is outside the raster bounds - > cannot use src raster
							
							addNoData(newBuffer, noData, dataType);
							
						}
					}else{ // the calculated position is outside the src bounds -> cannot use src raster

						// use NoData values and write it to the new buffer
						addNoData(newBuffer, noData, dataType);

					}
				}				
			}
		}
		//set the newBuffer as the rasters data
		raster.setData(newBuffer);
	}
	
	/**
	 * adds the value of the NoData object of this raster to the buffer
	 * according to the datatype
	 * @param buffer the buffer to write
	 * @param noData the nodata
	 * @param dataType the datatype
	 */
	public void addNoData(ByteBuffer buffer, NoData noData, DataType dataType){
		switch(dataType){
		case BYTE:

			buffer.put((byte)noData.getValue());
			break;
		case CHAR:
			buffer.putChar((char)noData.getValue());
			break;
		case DOUBLE:
			buffer.putDouble(noData.getValue());
			break;
		case FLOAT:
			buffer.putFloat((float)noData.getValue());
			break;
		case INT:
			buffer.putInt((int)noData.getValue());
			break;
		case LONG:
			buffer.putLong((long)noData.getValue());
			break;
		case SHORT:
			buffer.putShort((short)noData.getValue());
			break;
		default:
			break;

		}
	}


	@Override
	public Priority getPriority() {

		return Priority.NORMAL;
	}

}
