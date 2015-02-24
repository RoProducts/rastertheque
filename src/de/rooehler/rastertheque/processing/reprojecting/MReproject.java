package de.rooehler.rastertheque.processing.reprojecting;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

import org.gdal.osr.SpatialReference;
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
import de.rooehler.rastertheque.processing.RasterOp;
import de.rooehler.rastertheque.proj.Proj;
import de.rooehler.rastertheque.util.Hints;
import de.rooehler.rastertheque.util.Hints.Key;
import de.rooehler.rastertheque.util.ProgressListener;

public class MReproject extends Reproject implements RasterOp {


	@Override
	public void execute(Raster raster, Map<Key, Serializable> params,Hints hints, ProgressListener listener) {

		// src projection
		SpatialReference src_proj = raster.getCRS();

		// target projection
		SpatialReference dst_proj = null;

		if(params != null && params.containsKey(Reproject.KEY_REPROJECT_TARGET_CRS)){
			String wkt = (String) params.get(Reproject.KEY_REPROJECT_TARGET_CRS);
			//if this is a proj parameter string convert to wkt
			if(wkt != null && wkt.startsWith("+proj")){
				wkt = Proj.proj2wkt(wkt);
			}
			//try to create a SpatialReference from it
			if(wkt != null){				
				dst_proj = new SpatialReference(wkt);
			}else{
				Log.e(MReproject.class.getSimpleName(), "no well-known text provided as dst crs parameter");
				return;
			}
			
		}else if(params == null){	
			Log.e(MReproject.class.getSimpleName(), "no params provided");
			return;
		}else if(!params.containsKey(Reproject.KEY_REPROJECT_TARGET_CRS)){
			Log.e(MReproject.class.getSimpleName(), "no parameter for the target crs provided");
			return;
		}
		if(src_proj == null){
			Log.e(MReproject.class.getSimpleName(), "src raster does not have a crs, cannot reproject");
			return;	
		}
		if(dst_proj == null){		
			Log.e(MReproject.class.getSimpleName(), "invalid well-known text provided as dst crs parameter");
			return;
		}

		final DataType dataType = raster.getBands().get(0).datatype();
		final int src_raster_width  = raster.getDimension().right - raster.getDimension().left;
		final int src_raster_height = raster.getDimension().bottom - raster.getDimension().top;

		CoordinateReferenceSystem src_crs = Proj.crs(src_proj.ExportToProj4());
		CoordinateReferenceSystem dst_crs = Proj.crs(dst_proj.ExportToProj4());

		ReferencedEnvelope	src_refEnv = new ReferencedEnvelope(raster.getBoundingBox(), src_crs);

		//target model space --> bounds
//		Envelope reprojected = Proj.reproject(raster.getBoundingBox(), src_crs, dst_crs);

		//densify 
		ReferencedEnvelope reprojected = src_refEnv.transform(dst_crs, 10);

		Log.d(MReproject.class.getSimpleName(), "reprojected "+reprojected.toString());

		//target raster resolution "how much model units are between two raster points"
		double dst_x_res = reprojected.getEnvelope().getWidth() / src_raster_width;
		double dst_y_res = reprojected.getEnvelope().getHeight() / src_raster_height;

		//src raster resolution -> "how much model units are between two raster points"
		double src_x_res = raster.getBoundingBox().getWidth() / src_raster_width;
		double src_y_res = raster.getBoundingBox().getHeight() / src_raster_height;

		//target reference coordinate
		final Coordinate dst_upperLeft = new Coordinate(reprojected.getEnvelope().getMinX(), reprojected.getEnvelope().getMaxY());
		//src reference coordinate
		final Coordinate src_upperLeft = new Coordinate(raster.getBoundingBox().getMinX(), raster.getBoundingBox().getMaxY());
		//inverse transform
		final CoordinateTransform inverseTransform = Proj.transform(dst_crs, src_crs);
		
		//the size of the buffer for one band
		final int bandSize = src_raster_width * src_raster_height * dataType.size();
		ByteBufferReader reader = new ByteBufferReader(raster.getData().array(), ByteOrder.nativeOrder());
		ByteBuffer newBuffer = ByteBuffer.allocate(bandSize * raster.getBands().size()); 

		NoData noData = null;
		
		ProjCoordinate dst_model_pos = new ProjCoordinate(dst_upperLeft.x , dst_upperLeft.y);
		Coordinate src_model_coord = new Coordinate();
		
		for(int i = 0; i < raster.getBands().size(); i++){
			//determine the nodata for each band
			noData = raster.getBands().get(i).nodata();
			//if not available, use the datatypes min value
			if(noData == NoData.NONE){
				noData = NoData.noDataForDataType(dataType);
			}

			for(int y = 0; y < src_raster_height; y++){
				for(int x = 0; x < src_raster_width; x++){

					//calculate the destination model coordinate within the destination bounds
					dst_model_pos.x = dst_upperLeft.x + x * dst_x_res;
					dst_model_pos.y = dst_upperLeft.y - y * dst_y_res;
					
					//transform it to the source model
					inverseTransform.transform(dst_model_pos, dst_model_pos);

					src_model_coord.x = dst_model_pos.x;
					src_model_coord.y = dst_model_pos.y;
					
					// if the source raster contains this position
					if(raster.getBoundingBox().contains(src_model_coord)){

						//calculate src raster position
						double src_raster_x = (src_model_coord.x - src_upperLeft.x) / src_x_res;
						double src_raster_y = (src_upperLeft.y - src_model_coord.y) / src_y_res;

						//TODO interpolate the position, for now nearest
						final int nearestX = (int) Math.rint(src_raster_x);
						final int nearestY = (int) Math.rint(src_raster_y);
						
						if(nearestX >= 0 && nearestX < src_raster_width && nearestY >= 0 && nearestY < src_raster_height){

							//determine the pos of this raster value within the src raster
							int bufferPos = (i * bandSize) + (nearestY * src_raster_width + nearestX) * dataType.size();

							//move buffer to this pos			
							reader.seekToOffset(bufferPos);

							try{
								//read from src into the new buffer
								switch(dataType){
								case BYTE:
									newBuffer.put(reader.readByte());
									break;
								case CHAR:
									newBuffer.putChar(reader.readChar());
									break;
								case DOUBLE:
									newBuffer.putDouble(reader.readDouble());
									break;
								case FLOAT:
									newBuffer.putFloat(reader.readFloat());
									break;
								case INT:
									newBuffer.putInt(reader.readInt());
									break;
								case LONG:
									newBuffer.putLong(reader.readLong());
									break;
								case SHORT:
									newBuffer.putShort(reader.readShort());
									break;
								default:
									break;

								}
							}catch(IOException e){
								Log.e(MReproject.class.getSimpleName(), "error reading from bytebuffered reader @ x : "+x+" y : "+ y);
							}
						}else{ //the interpolated src raster position is outside the raster bounds - > cannot use src raster
							
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
