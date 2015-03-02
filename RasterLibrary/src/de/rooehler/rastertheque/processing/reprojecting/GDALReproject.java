package de.rooehler.rastertheque.processing.reprojecting;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.osr.SpatialReference;
import org.osgeo.proj4j.CoordinateReferenceSystem;
import org.osgeo.proj4j.CoordinateTransform;
import org.osgeo.proj4j.CoordinateTransformFactory;
import org.osgeo.proj4j.ProjCoordinate;

import android.graphics.Rect;
import android.util.Log;

import com.vividsolutions.jts.geom.Envelope;

import de.rooehler.rastertheque.core.Band;
import de.rooehler.rastertheque.core.DataType;
import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.core.RasterQuery;
import de.rooehler.rastertheque.io.gdal.GDALBand;
import de.rooehler.rastertheque.io.gdal.GDALDataset;
import de.rooehler.rastertheque.io.gdal.GDALRasterQuery;
import de.rooehler.rastertheque.processing.RasterOp;
import de.rooehler.rastertheque.proj.Proj;
import de.rooehler.rastertheque.util.Hints;
import de.rooehler.rastertheque.util.Hints.Key;
import de.rooehler.rastertheque.util.ProgressListener;
/**
 * Reproject Operation which makes use of the GDAL library
 * 
 * @author Robert Oehler
 *
 */
public class GDALReproject extends Reproject implements RasterOp {

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
		
		CoordinateReferenceSystem src_crs = Proj.crs(src_proj.ExportToProj4());
		CoordinateReferenceSystem dst_crs = Proj.crs(dst_proj.ExportToProj4());

		final DataType dataType = raster.getBands().get(0).datatype();

		final int src_raster_width  = raster.getDimension().width();
		final int src_raster_height = raster.getDimension().height();

		final int bandCount = raster.getBands().size();
		//the size of the buffer for one band
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
		double[] geotransform = raster.getGeoTransform();
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
		
		CoordinateTransformFactory txFactory = new CoordinateTransformFactory();
		CoordinateTransform tx = txFactory.createTransform(src_crs, dst_crs);
		
	    // Work out the boundaries of the new dataset in the target projection
		ProjCoordinate ul = new ProjCoordinate(geotransform[0], geotransform[3]);
		ProjCoordinate lr = new ProjCoordinate(geotransform[0] + geotransform[1]*src_raster_width,geotransform[3] + geotransform[5]*src_raster_height);
		ProjCoordinate ul_warped = tx.transform(ul,ul);
		ProjCoordinate lr_warped = tx.transform(lr, lr);
		
		Dataset warped = driver.Create(
				"",
				src_raster_width,
				src_raster_height,
				bandCount,
				gdalDataType);
		
		double[] warped_geotransform = new double[]{
				ul_warped.x,
				(lr_warped.x - ul_warped.x) / src_raster_width, /* w-e pixel resolution */
				geotransform[2],  /* 0 */
				ul_warped.y , /* top left y */
				geotransform[4], /* 0 */
				- ((ul_warped.y - lr_warped.y) / src_raster_height) /* n-s pixel resolution (negative value) */
		};
		warped.SetGeoTransform(warped_geotransform);
		warped.SetProjection(dst_proj.ExportToWkt());
		
		int success = gdal.ReprojectImage(ds, warped);
		
		if(success != gdalconst.CE_None){
			Log.e(MReproject.class.getSimpleName(), "error reprojecting with gdal");
		}		

		final GDALDataset warpedDS = new GDALDataset(warped);
		
		Envelope reprojected = new Envelope(ul.x, lr.x, lr.y, ul.y);
		
		int nbands = warped.GetRasterCount();
		
		List<Band> bands = new ArrayList<Band>(nbands);
		for (int i = 1; i <= nbands; i++) {
			bands.add(new GDALBand(warped.GetRasterBand(i)));
		}
		final Rect readDim = new Rect(0, 0, src_raster_width, src_raster_height);
		
		RasterQuery query = new GDALRasterQuery(
				reprojected,
				dst_proj,
				bands,
				readDim,
				dataType,
				readDim);
		
		Raster reprojectedRaster = warpedDS.read(query);
		
		raster.setData(reprojectedRaster.getData());

		warpedDS.close();
		ds.delete();
		warped.delete();
	}

	@Override
	public Priority getPriority() {
		// TODO correct
		return Priority.LOW;
	}

}
