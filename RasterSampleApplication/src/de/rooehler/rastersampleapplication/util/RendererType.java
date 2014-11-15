package de.rooehler.rastersampleapplication.util;

import java.io.File;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.reader.MapDatabase;
import org.mapsforge.map.reader.header.FileOpenResult;
import org.mapsforge.map.reader.header.MapFileInfo;

import android.util.Log;
import de.rooehler.rastersampleapplication.rasterrenderer.mbtiles.MbTilesDatabase;
import de.rooehler.rastertheque.io.mbtiles.gdal.GDALDecoder;

public enum RendererType {
	

	MAPSFORGE,
	MBTILES,
	RASTER;
	
	public static final int MAPSFORGE_MIN_ZOOM = 8;
	public static final int MAPSFORGE_MAX_ZOOM = 20;
	
	public static final int MBTILES_MIN_ZOOM = 10;
	public static final int MBTILES_MAX_ZOOM = 16;
	
	public static final int RASTER_MIN_ZOOM = 1;
	public static final int RASTER_MAX_ZOOM = 8;
	
	public static final String TAG = RendererType.class.getSimpleName();
	
	
	public static String[] getExtensionForType(RendererType type){
		
		switch (type) {
		case MAPSFORGE:		
			return new String[]{"map"};
		case MBTILES:
			return new String[]{"mbtiles"};
		case RASTER:
			return new String[]{"tif","tiff","dem"};

		default:
			throw new IllegalArgumentException("invalid type requested");
		}
	}
	public static String[] getTypes(){
		
		String[] titles = new String[RendererType.values().length];
		
		for(int i = 0; i < RendererType.values().length;i++){
			titles[i] = RendererType.values()[i].name();
		}
		
		return titles;
	}
	

	public static MapPosition getCenterForFilePath(RendererType type,final MapView mapView,String filePath){
		
		
		switch (type) {
		case MAPSFORGE:
			
			final File file = new File(filePath);
			Log.i(RendererType.class.getSimpleName(), "Map file is " + file.getAbsolutePath()+ " file exists "+Boolean.toString(file.exists()));
			final String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
			MapDatabase mapDatabase = new MapDatabase();
			final FileOpenResult result = mapDatabase.openFile(file);
			if (result.isSuccess()) {
				final MapFileInfo mapFileInfo = mapDatabase.getMapFileInfo();
				if (mapFileInfo != null && mapFileInfo.startPosition != null) {
					return new MapPosition(mapFileInfo.startPosition, mapFileInfo.startZoomLevel != null ? mapFileInfo.startZoomLevel : 8);
				} else if(mapFileInfo != null){
					final LatLong center  = mapFileInfo.boundingBox.getCenterPoint();
					if(center != null){
						return new MapPosition(center, mapFileInfo.startZoomLevel != null ? mapFileInfo.startZoomLevel : 8);
					}else{
						Log.e(RendererType.class.getSimpleName(), "could not retrieve bounding box center position for "+fileName);
					}
				}else{
					Log.e(RendererType.class.getSimpleName(), "could not retrieve map start position for "+fileName);
					return new MapPosition(new LatLong(0,0),(byte) 8);
				}
			}
			throw new IllegalArgumentException("Invalid Map File " + fileName); 
			
		case MBTILES:
			
			MbTilesDatabase db = new MbTilesDatabase(mapView.getContext(), filePath);
			db.openDataBase();
			LatLong loc = db.getBoundingBox().getCenterPoint();
			int[] zoomMinMax = db.getMinMaxZoom(); 
			db.close();
			db = null;
			byte zoom = zoomMinMax == null ? (byte) 8 : (byte) zoomMinMax[0];
			return new MapPosition(loc, zoom);
			
		case RASTER:
			
			if(GDALDecoder.getCurrentDataSet() == null){
				GDALDecoder.open(filePath);
				if(GDALDecoder.getCurrentDataSet() == null){
					throw new IllegalArgumentException("invalid mapFile provided");
				}
			}

			LatLong center = GDALDecoder.getCenterPoint();
			
			final float widthHeightRatio = GDALDecoder.getWidthGeightRatio();
			
			if(widthHeightRatio != 1){
				if(widthHeightRatio > 1){ //width is larger than height, move center up
					center = new LatLong(center.latitude + MercatorProjection.LATITUDE_MAX / widthHeightRatio,center.longitude);
				}else{ //height is larger than width, move center to left		
					center = new LatLong(center.latitude, center.longitude - 360 * widthHeightRatio);
				}
			}
			
			final int tileSize = mapView.getModel().displayModel.getTileSize();
			
			final byte zoomLevel = GDALDecoder.getStartZoomLevel(center,tileSize);
			Log.d(TAG, "calculated zoom "+zoomLevel);
			return new MapPosition(center, zoomLevel);
			
		default:
			
			throw new IllegalArgumentException("invalid type requested");
		}
	}	
}
