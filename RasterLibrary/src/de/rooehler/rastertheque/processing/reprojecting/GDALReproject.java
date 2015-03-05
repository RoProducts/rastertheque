package de.rooehler.rastertheque.processing.reprojecting;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.osgeo.proj4j.CoordinateReferenceSystem;

import android.graphics.Rect;
import android.util.Log;
import de.rooehler.rastertheque.core.Band;
import de.rooehler.rastertheque.core.DataType;
import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.core.RasterQuery;
import de.rooehler.rastertheque.core.util.ReferencedEnvelope;
import de.rooehler.rastertheque.io.gdal.GDALBand;
import de.rooehler.rastertheque.io.gdal.GDALDataset;
import de.rooehler.rastertheque.io.gdal.GDALRasterQuery;
import de.rooehler.rastertheque.processing.Interpolation.ResampleMethod;
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

	/**
	 * executes the operation on the @param raster according to the @params
	 * using the optional @param hints
	 * the  @listener is currently not used
	 */
	@Override
	public void execute(Raster raster, Map<Key, Serializable> params, Hints hints, ProgressListener listener) {

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
				try{
					dst_crs = Proj.crs(wkt);
				}catch(RuntimeException e){
					Log.e(Reproject.class.getSimpleName(), "error parsing target projection String "+wkt);
					return;
				}
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
		//check for an interpolation hint
		int interpolation = gdalconst.GRA_Bilinear;
		if(hints != null && hints.containsKey(Hints.KEY_INTERPOLATION)){
			ResampleMethod method = (ResampleMethod) hints.get(Hints.KEY_INTERPOLATION);
			interpolation = resampleMethod2GDALInterpolation(method);
		}
		
		//prepare some constants
		final String src_wkt = Proj.proj2wkt(src_crs.getParameterString());
		final String dst_wkt = Proj.proj2wkt(dst_crs.getParameterString());

		final DataType dataType = raster.getBands().get(0).datatype();
		final int gdalDataType = DataType.toGDAL(dataType);

		final int srcWidth  = raster.getDimension().width();
		final int srcHeight = raster.getDimension().height();

		//the size of the buffer for one band
		final int bandSize = srcWidth * srcHeight * dataType.size();
		final int bandCount = raster.getBands().size();

		//1a.create an in-memory gdal dataset
		Driver driver = gdal.GetDriverByName("MEM");
		Dataset ds = driver.Create(
				"",
				srcWidth,
				srcHeight,
				bandCount,
				gdalDataType);
		//1b.set projection
		ds.SetProjection(src_wkt);

		//1c.set transform
		double[] geotransform = raster.getGeoTransform();
		ds.SetGeoTransform(geotransform);

		//1d.fill the dataset with the data of the buffer
		for(int i = 0;i < bandCount;i++){
			
			int success = Integer.MIN_VALUE;
			if(bandCount > 1){
				//needs to subdivide the raster buffer data into a buffer for each band
				byte[] bandBytes = new byte[bandSize];
				raster.getData().get(bandBytes, i * bandSize, bandSize);
				ByteBuffer bandBuffer = ByteBuffer.allocateDirect(bandSize);
				bandBuffer.put(bandBytes);

				success = ds.GetRasterBand(i + 1).WriteRaster_Direct(0, 0, srcWidth, srcHeight, bandBuffer);
			}else{
				//its possible to use the buffer as is
				success = ds.GetRasterBand(i + 1).WriteRaster_Direct(0, 0, srcWidth, srcHeight, raster.getData());
			}
			
			if(success != gdalconst.CE_None){
				Log.e(MReproject.class.getSimpleName(), "error filling in memory dataset bands with data from raster");
				return;
			}
		}
		//data available, can warp
		
		//2. transform the source bounds to the target bounds
		//2a.src envelope
		ReferencedEnvelope	src_refEnv = new ReferencedEnvelope(raster.getBoundingBox(), src_crs);
		//2b.transform to the target crs
		ReferencedEnvelope reprojected = src_refEnv.transform(dst_crs, 10);
		
		//3a.create an in-memory target dataset
		Dataset warped = driver.Create("", srcWidth, srcHeight, bandCount, gdalDataType);
		double[] warped_geotransform = new double[]{
				reprojected.getEnvelope().getMinX(),
				(reprojected.getEnvelope().getMaxX() - reprojected.getEnvelope().getMinX()) / srcWidth, /* w-e pixel resolution */
				geotransform[2],  /* 0 */
				reprojected.getEnvelope().getMaxY() , /* top left y */
				geotransform[4], /* 0 */
				- ((reprojected.getEnvelope().getMaxY() - reprojected.getEnvelope().getMinY()) / srcHeight) /* n-s pixel resolution (negative value) */
		};
		//3b.set transform
		warped.SetGeoTransform(warped_geotransform);
		//3c.set projection
		warped.SetProjection(dst_wkt);
		
		//4. reproject using GDAL
		int success = gdal.ReprojectImage(ds, warped, src_wkt, dst_wkt, interpolation);
		
		if(success != gdalconst.CE_None){
			Log.e(MReproject.class.getSimpleName(), "error reprojecting with gdal");
			return;
		}		

		final GDALDataset warpedDS = new GDALDataset(warped);
		
		int nbands = warped.GetRasterCount();
		
		List<Band> bands = new ArrayList<Band>(nbands);
		for (int i = 1; i <= nbands; i++) {
			bands.add(new GDALBand(warped.GetRasterBand(i)));
		}
		final Rect readDim = new Rect(0, 0, srcWidth, srcHeight);
		RasterQuery query = new GDALRasterQuery(
				reprojected.getEnvelope(),
				dst_crs,
				bands,
				readDim,
				dataType,
				readDim);
		
		//5.read from reprojected
		Raster reprojectedRaster = warpedDS.read(query);
		//6. set data
		raster.setData(reprojectedRaster.getData());
		
		// clean up
		warpedDS.close();
		ds.delete();
		warped.delete();
	}

	@Override
	public Priority getPriority() {
		return Priority.HIGH;
	}
	
	private int resampleMethod2GDALInterpolation(ResampleMethod m){
		
		switch (m) {
		case NEARESTNEIGHBOUR: 
			return gdalconst.GRA_NearestNeighbour;
		case BILINEAR:
			return gdalconst.GRA_Bilinear;
		case BICUBIC:
			return gdalconst.GRA_Cubic;

		default:
			return gdalconst.GRA_Bilinear;
		}
	}

}
