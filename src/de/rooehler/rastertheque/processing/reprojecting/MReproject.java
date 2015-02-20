package de.rooehler.rastertheque.processing.reprojecting;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.osr.SpatialReference;
import org.osgeo.proj4j.CoordinateReferenceSystem;
import org.osgeo.proj4j.CoordinateTransform;
import org.osgeo.proj4j.ProjCoordinate;

import android.util.Log;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

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

		// needs src projection
		SpatialReference src_proj = raster.getCRS();

		// needs target projection
		SpatialReference dst_proj = null;

		if(params != null && params.containsKey(Reproject.KEY_REPROJECT_TARGET_CRS)){
			String wkt = (String) params.get(Reproject.KEY_REPROJECT_TARGET_CRS);
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

		final DataType dataType = raster.getBands().get(0).datatype();
		

		CoordinateReferenceSystem src_crs = Proj.crs(src_proj.ExportToProj4());

		CoordinateReferenceSystem dst_crs = Proj.crs(dst_proj.ExportToProj4());
		
		final int src_raster_width  = raster.getDimension().right - raster.getDimension().left;
		final int src_raster_height = raster.getDimension().bottom - raster.getDimension().top;
		
		ReferencedEnvelope	src_refEnv = new ReferencedEnvelope(raster.getBoundingBox(), src_crs);

		//the size of the buffer for one band
		final int bandSize = src_raster_width * src_raster_height * dataType.size();

		ByteBufferReader reader = new ByteBufferReader(raster.getData().array(), ByteOrder.nativeOrder());
		ByteBuffer newBuffer = ByteBuffer.allocateDirect(bandSize * raster.getBands().size()); 

		//target model space --> bounds
		Envelope reprojected = Proj.reproject(raster.getBoundingBox(), src_crs, dst_crs);
	
		//densify //TODO use
		ReferencedEnvelope densified = src_refEnv.transform(dst_crs, 10);

		Log.d(MReproject.class.getSimpleName(), "reprojected "+reprojected.toString());

		//target raster resolution "how much model units are between two raster points"
		double dst_x_res = reprojected.getWidth() / src_raster_width;
		double dst_y_res = reprojected.getHeight() / src_raster_height;

		//src raster resolution -> "how much model units are between two raster points"
		double src_x_res = raster.getBoundingBox().getWidth() / src_raster_width;
		double src_y_res = raster.getBoundingBox().getHeight() / src_raster_height;

		//target reference coordinate
		final Coordinate dst_upperLeft = new Coordinate(reprojected.getMinX(), reprojected.getMinY());
        //TODO comment
		final Coordinate src_upperLeft = new Coordinate(raster.getBoundingBox().getMinX(), raster.getBoundingBox().getMinY());

		final CoordinateTransform inverseTransform = Proj.transform(dst_crs, src_crs);

		for(int i = 0; i < raster.getBands().size(); i++){
			for(int y = 0; y < src_raster_height; y++){
				for(int x = 0; x < src_raster_width; x++){

				
					ProjCoordinate dst_model_pos = new ProjCoordinate(dst_upperLeft.x +  x * dst_x_res, dst_upperLeft.y + y * dst_y_res);

					inverseTransform.transform(dst_model_pos, dst_model_pos);

					Coordinate src_model_coord = new Coordinate(dst_model_pos.x, dst_model_pos.y);

					if(raster.getBoundingBox().contains(src_model_coord)){

						//calculate src raster
						double src_raster_x = (src_model_coord.x - src_upperLeft.x) / src_x_res;
						double src_raster_y = (src_model_coord.y - src_upperLeft.y) / src_y_res;

						final int nearestX = (int) Math.rint(src_raster_x);
						final int nearestY = (int) Math.rint(src_raster_y);

						//TODO interpolate the position

						int bufferPos = (i * bandSize) + (nearestY * src_raster_width + nearestX) * dataType.size();

						reader.seekToOffset(bufferPos);

						try{

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
							Log.e(MReproject.class.getSimpleName(), "error reading from bytebuffered reader");
						}

					}else{

						// use NoData
						NoData noData = raster.nodata().equals(NoData.NONE) ? NoData.noDataForDataType(dataType) : raster.nodata();

						switch(dataType){
						case BYTE:

							newBuffer.put((byte)noData.getValue());
							break;
						case CHAR:
							newBuffer.putChar((char)noData.getValue());
							break;
						case DOUBLE:
							newBuffer.putDouble(noData.getValue());
							break;
						case FLOAT:
							newBuffer.putFloat((float)noData.getValue());
							break;
						case INT:
							newBuffer.putInt((int)noData.getValue());
							break;
						case LONG:
							newBuffer.putLong((long)noData.getValue());
							break;
						case SHORT:
							newBuffer.putShort((short)noData.getValue());
							break;
						default:
							break;

						}
					}

				}				
			}
		}
		newBuffer.rewind();
		raster.setData(newBuffer);
	}


	@Override
	public Priority getPriority() {

		return Priority.NORMAL;
	}

}
