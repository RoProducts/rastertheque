package de.rooehler.rasterapp.test;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Rect;
import android.util.Log;

import com.vividsolutions.jts.geom.Envelope;

import de.rooehler.rastertheque.core.Drivers;
import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.core.RasterQuery;
import de.rooehler.rastertheque.io.gdal.GDALDataset;
import de.rooehler.rastertheque.io.gdal.GDALRasterQuery;
import de.rooehler.rastertheque.processing.RasterOps;
import de.rooehler.rastertheque.processing.reprojecting.MReproject;
import de.rooehler.rastertheque.util.Constants;
import de.rooehler.rastertheque.util.Hints.Key;

public class ReprojectionTest extends android.test.ActivityTestCase {
	
	/**
	 * tests the reprojection of a raster
	 * 
	 * the result cannnot be checked thoroughly, it needs to be visualized and checked by "eye"
	 * 
	 * @throws IOException
	 */
	
	public void testReprojection() throws IOException {
		
		GDALDataset dataset = (GDALDataset) Drivers.open(TestIO.C4107a1_INT_CM, null);
		
		Envelope bounds = dataset.getBoundingBox();
		Rect dim = dataset.getDimension();
		
		final int rs = 10;
		
		final int ts =  (int) (dim.right / rs);

		final Rect readSize = new Rect(dim.left,dim.top,dim.left + ts,dim.top + ts);
		
		final Envelope  readBounds = new Envelope(
				bounds.getMinX(),
				bounds.getMinX() + bounds.getWidth() / rs,
				bounds.getMinY(),
				bounds.getMinY() + bounds.getHeight() / rs);

		final RasterQuery query = new GDALRasterQuery(
				readBounds,
				dataset.getCRS(),
				dataset.getBands(),
				readSize,
				dataset.getBands().get(0).datatype(),
				readSize);

		Raster raster = dataset.read(query);
		
		
//		String wkt = Constants.EPSG_4326;
		String wkt = Constants.EPSG_3857;
		
		HashMap<Key,Serializable> params = new HashMap<>();
		
		params.put(MReproject.KEY_REPROJECT_TARGET_CRS, wkt);
		
		long now = System.currentTimeMillis();
		RasterOps.execute(raster, RasterOps.REPROJECT, params, null, null);
		Log.d(ReprojectionTest.class.getSimpleName(), "reprojecting took "+ (System.currentTimeMillis() - now));
		
		now = System.currentTimeMillis();
		RasterOps.execute(raster, RasterOps.COLORMAP, null, null, null);
		Log.d(ReprojectionTest.class.getSimpleName(), "colormap took "+ (System.currentTimeMillis() - now));
		
		final int[] pixels  = new int[ts * ts];

    	raster.getData().asIntBuffer().get(pixels);
		
		Bitmap bitmap = Bitmap.createBitmap(ts, ts, Config.ARGB_8888);
		bitmap.setPixels(pixels, 0, ts, 0, 0, ts, ts);	
		
		assertNotNull(bitmap);

		TestUtil.saveImage(bitmap,"ReprojectOp_gdal_3857");
	}
	
	
}
