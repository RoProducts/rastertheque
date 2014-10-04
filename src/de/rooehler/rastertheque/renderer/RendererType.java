package de.rooehler.rastertheque.renderer;

import java.io.File;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.mbtiles.MbTilesDatabase;
import org.mapsforge.map.reader.MapDatabase;
import org.mapsforge.map.reader.header.FileOpenResult;
import org.mapsforge.map.reader.header.MapFileInfo;

import android.content.Context;
import android.util.Log;

public enum RendererType {
	
	

	MAPSFORGE,
	MBTILES;
	
	public static final int MAPSFORGE_MIN_ZOOM = 8;
	public static final int MAPSFORGE_MAX_ZOOM = 20;
	
	public static final int MBTILES_MIN_ZOOM = 10;
	public static final int MBTILES_MAX_ZOOM = 16;
	
	
	
	public static String getExtensionForType(RendererType type){
		
		switch (type) {
		case MAPSFORGE:
			
			return "map";
		case MBTILES:
			return "mbtiles";

		default:
			throw new IllegalArgumentException("invalid type requested");
		}
	}
	
	public static LatLong getCenterForFilePath(RendererType type,final Context context,String filePath){
		
		
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
					return mapFileInfo.startPosition;
				} else if(mapFileInfo != null){
					final LatLong center  = mapFileInfo.boundingBox.getCenterPoint();
					if(center != null){
						return center;
					}else{
						Log.e(RendererType.class.getSimpleName(), "could not retrieve bounding box center position for "+fileName);
					}
				}else{
					Log.e(RendererType.class.getSimpleName(), "could not retrieve map start position for "+fileName);
					return new LatLong(0,0);
				}
			}
			throw new IllegalArgumentException("Invalid Map File " + fileName);
			
		case MBTILES:
			
			MbTilesDatabase db = new MbTilesDatabase(context, filePath);
			db.openDataBase();
			LatLong loc = db.getBoundingBox().getCenterPoint();
			db.close();
			db = null;
			return loc;

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


