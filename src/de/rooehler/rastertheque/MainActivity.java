package de.rooehler.rastertheque;

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
import org.mapsforge.map.rendertheme.InternalRenderTheme;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import de.rooehler.rastertheque.renderer.MapsforgeFileHelper;
import de.rooehler.rastertheque.renderer.RendererType;
import de.rooehler.rastertheque.util.Constants;
import de.rooehler.rastertheque.util.LocateMe;

public class MainActivity extends Activity {
	
	final static String TAG = MainActivity.class.getSimpleName();
	
	private MapView mapView;
	
	private TileCache tileCache;
	
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;
	private CharSequence mDrawerTitle;
    private CharSequence mTitle;
	
	private final static String[] titles = new String[]{"Mapsforge","MBTiles"};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main_layout);
		
		mTitle = mDrawerTitle = getTitle();
		
		mapView = (MapView) findViewById(R.id.mapView);
		
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        
        mDrawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, titles));
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,int pos, long id) {
				Log.d(TAG, "selected pos "+ pos);
			}
		});
        
        // enable ActionBar app icon to behave as action to toggle nav drawer
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.ic_drawer,  /* nav drawer image to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description for accessibility */
                R.string.drawer_close  /* "close drawer" description for accessibility */
                ) {
            public void onDrawerClosed(View view) {
                getActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
		
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

	
	@Override
	protected void onStop() {
		super.onStop();
		
		Editor ed = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit();
		ed.putInt(Constants.PREFS_ZOOM, mapView.getModel().mapViewPosition.getZoomLevel());
		ed.commit();
	}

	@Override
	public void onDestroy(){
		super.onDestroy();

		this.tileCache.destroy();
		this.mapView.destroy();
		org.mapsforge.map.android.graphics.AndroidResourceBitmap.clearResourceBitmaps();
		mapView = null;

	}
	
	@Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getActionBar().setTitle(mTitle);
    }
	
	/**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        mDrawerToggle.onConfigurationChanged(newConfig);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
//        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
//        menu.findItem(R.id.action_websearch).setVisible(!drawerOpen);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
         // The action bar home/up action should open or close the drawer.
         // ActionBarDrawerToggle will take care of this.
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle action buttons
        switch(item.getItemId()) {

        default:
            return super.onOptionsItemSelected(item);
        }
    }
	
	
	
	private void setMapStyle() {
			
		try{

			LatLong actualLoc = LocateMe.locateMeForLatLong(mapView.getContext());
			MapViewPosition mvp = mapView.getModel().mapViewPosition;

			final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			final int zoom =  prefs.getInt(Constants.PREFS_ZOOM, 12);
			
			final RendererType type = RendererType.values()[prefs.getInt(Constants.PREFS_RENDERER_TYPE, 0)];
			
			int zoomInBounds = checkZoomBounds(type, zoom);
			
			if(actualLoc != null){ //use located pos
				Log.d(TAG, "centering in mapStyle on "+actualLoc.toString());
				mvp.setMapPosition(new MapPosition(actualLoc,(byte) zoomInBounds));
			}else{ //use mapviews position or static if not available
				mvp.setMapPosition(MapsforgeFileHelper.getInitialPosition());
			}
			
			switch (type) {
			case MAPSFORGE:
				
				MapDatabase mapDatabase = new MapDatabase();
				
				final FileOpenResult result = mapDatabase.openFile(MapsforgeFileHelper.getMapsforgeFile());
				if (result.isSuccess()) {
					
					Layer resultingLayer = new TileRendererLayer(tileCache, mvp, false, AndroidGraphicFactory.INSTANCE);
					((TileRendererLayer) resultingLayer).setMapFile(MapsforgeFileHelper.getMapsforgeFile());
					((TileRendererLayer) resultingLayer).setXmlRenderTheme(InternalRenderTheme.OSMARENDER);
					mapView.getLayerManager().getLayers().add(0, resultingLayer);
					
					mapView.getModel().mapViewPosition.setZoomLevelMax((byte) MapsforgeFileHelper.RENDER_MAX_ZOOM);
					mapView.getModel().mapViewPosition.setZoomLevelMin((byte) MapsforgeFileHelper.RENDER_MIN_ZOOM);
					
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
				
				break;
			case MBTILES:
				
				break;

			default:
				break;
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



	public static int checkZoomBounds(final RendererType type,final int zoom){
		int newZoom = zoom;

		switch (type) {
		case MAPSFORGE:
			
			if(zoom < MapsforgeFileHelper.RENDER_MIN_ZOOM)newZoom = MapsforgeFileHelper.RENDER_MIN_ZOOM;
			if(zoom > MapsforgeFileHelper.RENDER_MAX_ZOOM)newZoom = MapsforgeFileHelper.RENDER_MAX_ZOOM;
			break;
		case MBTILES:
		
		break;

		default:
			break;
		}
		return newZoom;
	}

}
