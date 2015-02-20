package de.rooehler.rastertheque.processing.reprojecting;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.osr.SpatialReference;

import android.util.Log;
import de.rooehler.rastertheque.core.DataType;
import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.core.util.ByteBufferReader;
import de.rooehler.rastertheque.processing.RasterOp;
import de.rooehler.rastertheque.util.Hints;
import de.rooehler.rastertheque.util.Hints.Key;
import de.rooehler.rastertheque.util.ProgressListener;

public class GDALReproject  extends Reproject implements RasterOp {

	@Override
	public void execute(Raster raster, Map<Key, Serializable> params, Hints hints, ProgressListener listener) {


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

		final int src_raster_width  = raster.getDimension().right - raster.getDimension().left;
		final int src_raster_height = raster.getDimension().bottom - raster.getDimension().top;

		//the size of the buffer for one band
		final int bandSize = src_raster_width * src_raster_height * dataType.size();
		ByteBufferReader reader = new ByteBufferReader(raster.getData().array(), ByteOrder.nativeOrder());
		ByteBuffer newBuffer = ByteBuffer.allocateDirect(bandSize * raster.getBands().size()); 


		final int gdalDataType = DataType.toGDAL(raster.getBands().get(0).datatype());

		//create an in-memory gdal dataset
		Driver driver = gdal.GetDriverByName("MEM");
		Dataset ds = driver.Create(
				"",
				src_raster_width,
				src_raster_height,
				raster.getBands().size(),
				gdalDataType);

		ds.SetProjection(src_proj.ExportToWkt());

		double[] geotransform = new double[]{
				raster.getBoundingBox().getMinX(), /* top left x */
				raster.getBoundingBox().getWidth() / src_raster_width, /* w-e pixel resolution */
				0,  /* 0 */
				raster.getBoundingBox().getMaxY() , /* top left y */
				0, /* 0 */
				- (raster.getBoundingBox().getHeight() / src_raster_height) /* n-s pixel resolution (negative value) */
		};
		ds.SetGeoTransform(geotransform);

		for(int i = 0;i < raster.getBands().size();i++){

			//TODO adapt for multiple bands
			int success = ds.GetRasterBand(i + 1).WriteBlock_Direct(0, 0, raster.getData());

			if(success != gdalconst.CE_None){
				Log.e(MReproject.class.getSimpleName(), "error writing block");
			}
		}
		//DONE can warp

		Dataset warped = gdal.AutoCreateWarpedVRT(ds, src_proj.ExportToWkt(), dst_proj.ExportToWkt());

		int width  = warped.getRasterXSize();
		int	height = warped.getRasterYSize();

		double[] gt = warped.GetGeoTransform();
		double	minx = gt[0];
		double	miny = gt[3] + width*gt[4] + height*gt[5]; // from	http://gdal.org/gdal_datamodel.html
		double	maxx = gt[0] + width*gt[1] + height*gt[2]; // from	http://gdal.org/gdal_datamodel.html
		double	maxy = gt[3];

		//[180,180] --> [158,200]



	}

	@Override
	public Priority getPriority() {
		// TODO correct
		return Priority.LOW;
	}

}
