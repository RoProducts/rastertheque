package de.rooehler.rastertheque.renderer;

import java.io.File;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.reader.MapDatabase;
import org.mapsforge.map.reader.header.FileOpenResult;
import org.mapsforge.map.reader.header.MapFileInfo;

import android.content.Context;
import android.util.Log;
import de.rooehler.rastertheque.gdal.GDALDecoder;
import de.rooehler.rastertheque.util.mapsforge.mbtiles.MbTilesDatabase;

public enum RendererType {

	MAPSFORGE,
	MBTILES,
	RASTER;
	
	public static final int MAPSFORGE_MIN_ZOOM = 8;
	public static final int MAPSFORGE_MAX_ZOOM = 20;
	
	public static final int MBTILES_MIN_ZOOM = 10;
	public static final int MBTILES_MAX_ZOOM = 16;
	
	
	
	public static String[] getExtensionForType(RendererType type){
		
		switch (type) {
		case MAPSFORGE:		
			return new String[]{"map"};
		case MBTILES:
			return new String[]{"mbtiles"};
		case RASTER:
			return new String[]{"tif","dem"};

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
					return new MapPosition(mapFileInfo.startPosition,mapFileInfo.startZoomLevel);
				} else if(mapFileInfo != null){
					final LatLong center  = mapFileInfo.boundingBox.getCenterPoint();
					if(center != null){
						return new MapPosition(center,mapFileInfo.startZoomLevel);
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
			
			long now = System.currentTimeMillis();
			if(GDALDecoder.getCurrentDataSet() == null){
				GDALDecoder.open(filePath);
				if(GDALDecoder.getCurrentDataSet() == null){
					throw new IllegalArgumentException("invalid mapFile provided");
				}
			}
			final BoundingBox bb = GDALDecoder.getBoundingBox();
			final LatLong center = bb.getCenterPoint();
			Log.d(RendererType.class.getSimpleName(), "RASTER getCenter took "+(System.currentTimeMillis()-now+" ms"));
	
			byte rasterZoom = LatLongUtils.zoomForBounds( mapView.getDimension(), bb, mapView.getModel().displayModel.getTileSize());
			
			return new MapPosition(center, rasterZoom);
			
		default:
			
			throw new IllegalArgumentException("invalid type requested");
		}
	}
	
	public static int checkZoomBounds(RendererType type,final int zoom){
		
		int newZoom = zoom;
		
		switch (type) {
		case MAPSFORGE:

			if(zoom < MAPSFORGE_MIN_ZOOM)newZoom = MAPSFORGE_MIN_ZOOM;
			if(zoom > MAPSFORGE_MAX_ZOOM)newZoom = MAPSFORGE_MAX_ZOOM;

			return newZoom;

		case MBTILES:

			if(zoom < MBTILES_MIN_ZOOM)newZoom = MBTILES_MIN_ZOOM;
			if(zoom > MBTILES_MAX_ZOOM)newZoom = MBTILES_MAX_ZOOM;

			return newZoom;

		default:
			throw new IllegalArgumentException("invalid type requested");
		}
	}
	
}


