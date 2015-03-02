package de.rooehler.rastertheque.processing.reprojecting;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.osr.SpatialReference;

import android.graphics.Rect;
import android.util.Log;

import com.vividsolutions.jts.geom.Envelope;

import de.rooehler.rastertheque.core.Band;
import de.rooehler.rastertheque.core.DataType;
import de.rooehler.rastertheque.core.NoData;
import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.core.RasterQuery;
import de.rooehler.rastertheque.core.util.ByteBufferReader;
import de.rooehler.rastertheque.io.gdal.GDALBand;
import de.rooehler.rastertheque.io.gdal.GDALDataset;
import de.rooehler.rastertheque.io.gdal.GDALRasterQuery;
import de.rooehler.rastertheque.processing.RasterOp;
import de.rooehler.rastertheque.proj.Proj;
import de.rooehler.rastertheque.util.Hints;
import de.rooehler.rastertheque.util.Hints.Key;
import de.rooehler.rastertheque.util.ProgressListener;

public class GDALReproject  extends Reproject implements RasterOp {

	@Override
	public void execute(Raster raster, Map<Key, Serializable> params, Hints hints, ProgressListener listener) {

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

		final int bandCount = raster.getBands().size();
		//the size of the buffer for one band
		final int bandSize = src_raster_width * src_raster_height * dataType.size();
		final int gdalDataType = DataType.toGDAL(raster.getBands().get(0).datatype());

		//create an in-memory gdal dataset
		Driver driver = gdal.GetDriverByName("MEM");
		Dataset ds = driver.Create(
				"",
				src_raster_width,
				src_raster_height,
				bandCount,
				gdalDataType);

		ds.SetProjection(src_proj.ExportToWkt());

		//set transform
		double[] geotransform = new double[]{
				raster.getBoundingBox().getMinX(), /* top left x */
				raster.getBoundingBox().getWidth() / src_raster_width, /* w-e pixel resolution */
				0,  /* 0 */
				raster.getBoundingBox().getMaxY() , /* top left y */
				0, /* 0 */
				- (raster.getBoundingBox().getHeight() / src_raster_height) /* n-s pixel resolution (negative value) */
		};
		ds.SetGeoTransform(geotransform);

		for(int i = 0;i < bandCount;i++){
			/*
			byte[] bandBytes = new byte[bandSize];
			raster.getData().get(bandBytes, i * bandSize, bandSize);
			ByteBuffer bandBuffer = ByteBuffer.allocateDirect(bandSize);
			bandBuffer.put(bandBytes);
			
			//TODO test multiple bands with this setup
			*/
			int success = ds.GetRasterBand(i + 1).WriteRaster_Direct(0, 0, src_raster_width, src_raster_height, raster.getData());
			
			if(success != gdalconst.CE_None){
				Log.e(MReproject.class.getSimpleName(), "error writing raster");
			}
		}
		//data available, can warp
		
		Dataset warped = gdal.AutoCreateWarpedVRT(ds, src_proj.ExportToWkt(), dst_proj.ExportToWkt());
		
		if(warped == null){
			Log.e(GDALReproject.class.getSimpleName(), "error reprojecting from "+src_proj.ExportToPrettyWkt()+ " to "+dst_proj.ExportToPrettyWkt());
			return;
		}
		
		final int warped_width  = warped.getRasterXSize();
		final int warped_height = warped.getRasterYSize();

		double[] gt = warped.GetGeoTransform();
		double	minx = gt[0];
		double	miny = gt[3] + warped_width*gt[4] + warped_height*gt[5];
		double	maxx = gt[0] + warped_width*gt[1] + warped_height*gt[2]; 
		double	maxy = gt[3];

		final GDALDataset warpedDS = new GDALDataset(warped);
		
		Envelope reprojected = new Envelope(minx, maxx, miny, maxy);
		
		int nbands = warped.GetRasterCount();
		
		List<Band> bands = new ArrayList<Band>(nbands);
		for (int i = 1; i <= nbands; i++) {
			bands.add(new GDALBand(warped.GetRasterBand(i)));
		}
		final Rect readDim = new Rect(0, 0, warped_width, warped_height);
		
		RasterQuery query = new GDALRasterQuery(
				reprojected,
				dst_proj,
				bands,
				readDim,
				dataType,
				readDim);
		
		Raster reprojectedRaster = warpedDS.read(query);
		
		ByteBufferReader reader = new ByteBufferReader(reprojectedRaster.getData().array(), ByteOrder.nativeOrder());
		ByteBuffer newBuffer = ByteBuffer.allocateDirect(bandSize * bandCount); 

		NoData noData = null;
		for(int i = 0; i < bandCount; i++){
			
			noData = raster.getBands().get(i).nodata();
			
			if(noData == NoData.NONE){
				noData = NoData.noDataForDataType(dataType);
			}
			
			for(int y = 0; y < src_raster_height; y++){
				for(int x = 0; x < src_raster_width; x++){
					
					
					if(x < warped_width && y < warped_height){
						//inside
						int bufferPos = ((i * bandSize) + (y* warped_width + x)) * dataType.size();						

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
							Log.e(MReproject.class.getSimpleName(), "error reading from bytebuffered reader , x : "+x+" y : "+y);
						}
					}else{
						//outside
						
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

		warpedDS.close();
		
		raster.setData(newBuffer);

	}

	@Override
	public Priority getPriority() {
		// TODO correct
		return Priority.LOW;
	}

}
