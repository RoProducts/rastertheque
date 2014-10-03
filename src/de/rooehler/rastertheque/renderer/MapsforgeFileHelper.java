package de.rooehler.rastertheque.renderer;

import java.io.File;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.map.reader.MapDatabase;
import org.mapsforge.map.reader.header.FileOpenResult;
import org.mapsforge.map.reader.header.MapFileInfo;

import android.os.Environment;
import android.util.Log;

public class MapsforgeFileHelper {
	
	private static final String TAG = MapsforgeFileHelper.class.getSimpleName();
	
	public static final int RENDER_MIN_ZOOM = 8;
	public static final int RENDER_MAX_ZOOM = 20;

	public static MapPosition getInitialPosition() {
		MapDatabase mapDatabase = new MapDatabase();
		final FileOpenResult result = mapDatabase.openFile(getMapsforgeFile());
		if (result.isSuccess()) {
			final MapFileInfo mapFileInfo = mapDatabase.getMapFileInfo();
			if (mapFileInfo != null && mapFileInfo.startPosition != null) {
				return new MapPosition(mapFileInfo.startPosition, (byte) mapFileInfo.startZoomLevel);
			} else {
				return new MapPosition(new LatLong(43.93330383300781, 10.300003051757812), (byte) 12);
			}
		}
		throw new IllegalArgumentException("Invalid Map File " + getMapFileName());
	}
	

	/**
	 * @return a map file
	 */
	public static File getMapsforgeFile() {
		File file = new File(Environment.getExternalStorageDirectory()+"/de.rooehler.bikecomputer.pro/", getMapFileName());
		Log.i(TAG, "Map file is " + file.getAbsolutePath()+ " file exists "+Boolean.toString(file.exists()));
		return file;
	}
	
	
	/**
	 * @return the map file name to be used
	 */
	public static String getMapFileName() {
		return "italy.map";
	}

	public static int checkZoomBounds(final int zoom){
		int newZoom = zoom;

		if(zoom < RENDER_MIN_ZOOM)newZoom = RENDER_MIN_ZOOM;
		if(zoom > RENDER_MAX_ZOOM)newZoom = RENDER_MAX_ZOOM;
		return newZoom;
	}
	

}
