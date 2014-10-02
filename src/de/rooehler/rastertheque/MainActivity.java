package de.rooehler.rastertheque;

import java.io.File;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.reader.MapDatabase;
import org.mapsforge.map.reader.header.FileOpenResult;
import org.mapsforge.map.reader.header.MapFileInfo;
import org.mapsforge.map.rendertheme.InternalRenderTheme;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import de.rooehler.rastertheque.util.LocateMe;

public class MainActivity extends Activity {
	
	final static String TAG = MainActivity.class.getSimpleName();
	
	private static final int RENDER_MIN_ZOOM = 8;
	private static final int RENDER_MAX_ZOOM = 20;
	
	private MapView mapView;
	
	private TileCache tileCache;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		
		setContentView(R.layout.main_layout);
		
		mapView = (MapView) findViewById(R.id.mapView);
		
		tileCache = AndroidUtil.createTileCache(this,
				"de.rooehler.bikecomputer.pro/cache/",
				mapView.getModel().displayModel.getTileSize(),
				getResources().getDisplayMetrics().density, //screenratio
				mapView.getModel().frameBufferModel.getOverdrawFactor());
		
		setMapStyle();
			
		
	}

	@Override
	public void onStart() {
		super.onStart();
	
        this.mapView.getLayerManager().redrawLayers();

	}
	
	private void setMapStyle() {
	
		
		try{
			
			Layer resultingLayer = null;
			LatLong actualLoc = LocateMe.locateMeForLatLong(mapView.getContext());
			MapViewPosition mvp = mapView.getModel().mapViewPosition;

			int zoom =  PreferenceManager.getDefaultSharedPreferences(this).getInt("last_zoom", 12);
			
			int zoomInBounds = checkZoomBounds( zoom);
			
			if(actualLoc != null){ //use located pos
				Log.d(TAG, "centering in mapStyle on "+actualLoc.toString());
				mvp.setMapPosition(new MapPosition(actualLoc,(byte) zoomInBounds));
			}else{ //use mapviews position or static if not available
				mvp.setMapPosition(getInitialPosition());
			}

			MapDatabase mapDatabase = new MapDatabase();
		
			final FileOpenResult result = mapDatabase.openFile(getMapFile());
			if (result.isSuccess()) {

				resultingLayer = new TileRendererLayer(tileCache, mvp, false, AndroidGraphicFactory.INSTANCE);
				((TileRendererLayer) resultingLayer).setMapFile(getMapFile());
				InternalRenderTheme theme = InternalRenderTheme.OSMARENDER;
				//((TileRendererLayer) resultingLayer).setTextScale(1.5f);	
				((TileRendererLayer) resultingLayer).setXmlRenderTheme(theme);

				mapView.getModel().mapViewPosition.setZoomLevelMax((byte) RENDER_MAX_ZOOM);
				mapView.getModel().mapViewPosition.setZoomLevelMin((byte) RENDER_MIN_ZOOM);
				mapView.getLayerManager().getLayers().add(0, resultingLayer);
				
				//now that we have that layer, check if it covers
				if(!LocateMe.mapFileContainsPoint(mapView, actualLoc)){
					try{
						Toast.makeText(getBaseContext(), "position outside of map file, centering on mapfile",Toast.LENGTH_SHORT).show();
						TileRendererLayer trl = (TileRendererLayer) mapView.getLayerManager().getLayers().get(0);

						if(trl.getMapDatabase().getMapFileInfo().startPosition != null){

							mapView.getModel().mapViewPosition.setCenter(trl.getMapDatabase().getMapFileInfo().startPosition);
							Log.d(TAG, "centering on startpos : " + trl.getMapDatabase().getMapFileInfo().startPosition.toString());

						}else{ //use bounding box center

							mapView.getModel().mapViewPosition.setCenter(trl.getMapDatabase().getMapFileInfo().boundingBox.getCenterPoint());
							Log.d(TAG, "centering on bbs center : " + trl.getMapDatabase().getMapFileInfo().boundingBox.getCenterPoint().toString());
						}

						mapView.getModel().mapViewPosition.setZoomLevel((byte) 8); //zoom out to see the map
						mapView.getLayerManager().redrawLayers();

					}catch(Exception e){
						Log.e(TAG, "Error using maps center as center position in setMapStyle",e);
					}
				}

			}else{
				Log.d(TAG, "no success opening the file");

			}

			
			if(mapView.getLayerManager().getLayers().size() > 0 ){
				mapView.setClickable(true);
				mapView.setBuiltInZoomControls(true);
				mapView.getMapScaleBar().setVisible(true);
			}else{
				Log.d(TAG, "no layers created");
			}
		}catch(Exception e){
			Log.e(TAG, "error setting map style",e);
		}
		
	}

	protected MapPosition getInitialPosition() {
		MapDatabase mapDatabase = new MapDatabase();
		final FileOpenResult result = mapDatabase.openFile(getMapFile());
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
	protected File getMapFile() {
		File file = new File(Environment.getExternalStorageDirectory()+"/de.rooehler.bikecomputer.pro/", this.getMapFileName());
		Log.i(TAG, "Map file is " + file.getAbsolutePath()+ " file exists "+Boolean.toString(file.exists()));
		return file;
	}
	
	
	/**
	 * @return the map file name to be used
	 */
	protected String getMapFileName() {
		return "italy.map";
	}

	public static int checkZoomBounds(final int zoom){
		int newZoom = zoom;

		if(zoom < RENDER_MIN_ZOOM)newZoom = RENDER_MIN_ZOOM;
		if(zoom > RENDER_MAX_ZOOM)newZoom = RENDER_MAX_ZOOM;
		return newZoom;
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		try{

			this.tileCache.destroy();
	  	    this.mapView.destroy();
	  	    org.mapsforge.map.android.graphics.AndroidResourceBitmap.clearResourceBitmaps();
			mapView = null;

		}catch (Exception e){
			Log.e(TAG, "error in onDestroy", e);
		}
	}

}
