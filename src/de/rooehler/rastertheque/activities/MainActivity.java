package de.rooehler.rastertheque.activities;

import java.io.File;

import org.mapsforge.core.model.MapPosition;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.rendertheme.InternalRenderTheme;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import de.rooehler.rastertheque.R;
import de.rooehler.rastertheque.R.drawable;
import de.rooehler.rastertheque.R.id;
import de.rooehler.rastertheque.R.layout;
import de.rooehler.rastertheque.R.string;
import de.rooehler.rastertheque.dialog.FilePickerDialog;
import de.rooehler.rastertheque.dialog.FilePickerDialog.FilePathPickCallback;
import de.rooehler.rastertheque.gdal.GDALDecoder;
import de.rooehler.rastertheque.renderer.RendererType;
import de.rooehler.rastertheque.util.mapsforge.mbtiles.MBTilesLayer;

public class MainActivity extends Activity {
	
	final static String TAG = MainActivity.class.getSimpleName();
	
	final static String PREFS_FILEPATH = "de.rooehler.rastertheque.filepath";
	final static String PREFS_RENDERER_TYPE = "de.rooehler.rastertheque.renderer_type";
	
	private MapView mapView;
	
	private TileCache tileCache;
	
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;
	private CharSequence mDrawerTitle;
    private CharSequence mTitle;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main_layout);
		
		getActionBar().setBackgroundDrawable(new ColorDrawable(0xffffffff));
		
		mTitle = mDrawerTitle = getTitle();
		
		mapView = (MapView) findViewById(R.id.mapView);
		
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        
        mDrawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, RendererType.getTypes()));
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,int pos, long id) {
				Log.d(TAG, "selected pos "+ pos);
				
				mDrawerLayout.closeDrawers();
				
				final RendererType newType = RendererType.values()[pos];
				final String[] extensions = RendererType.getExtensionForType(newType);
				
				new FilePickerDialog(MainActivity.this, "Select a file", extensions, new FilePathPickCallback() {
					
					@Override
					public void filePathPicked(String filePath) {
						
						Log.d(TAG, "path selected "+filePath);			
						//TODO also raster should be able to handle the default implementation
						if(newType == RendererType.RASTER){

							long now = System.currentTimeMillis();
							GDALDecoder.open(filePath);
							
							Log.d(TAG, "RASTER open "+(System.currentTimeMillis()-now+" ms"));
							
							MapPosition mp = RendererType.getCenterForFilePath(newType, getBaseContext(), filePath);
							
							Log.d(TAG, "RASTER center "+(mp.latLong.toString()));
							
						}else{
							
							MapPosition mp = RendererType.getCenterForFilePath(newType, getBaseContext(), filePath);						
							
							setMapStyle(newType, filePath, mp);
							
							Editor ed = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit();
							ed.putString(PREFS_FILEPATH, filePath);
							ed.putInt(PREFS_RENDERER_TYPE, newType.ordinal());
							ed.commit();

							
						}
					}
				});
			
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
		
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		final RendererType type = RendererType.values()[prefs.getInt(PREFS_RENDERER_TYPE, 0)];
		
		String savedFilePath = prefs.getString(PREFS_FILEPATH, null);
		if(savedFilePath == null){
			//hardcoded default file
			savedFilePath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/de.rooehler.bikecomputer.pro/italy.map";
		}		
		
		final MapPosition mapPosition = RendererType.getCenterForFilePath(type,getBaseContext(), savedFilePath);
		
		setMapStyle(type,savedFilePath,mapPosition);
		
				
	}




	@Override
	public void onStart() {
		super.onStart();
	
        this.mapView.getLayerManager().redrawLayers();

	}

	
	@Override
	protected void onStop() {
		super.onStop();
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
	
	
	
	private void setMapStyle(final RendererType type, final String filePath, final MapPosition pMapPosition) {
			
		try{
			final File file = new File(filePath);
			if(!file.exists()){
				Log.e(TAG, "filepath for map invalid");
				return;
			}
			
			//check existing layers
			if(mapView.getLayerManager().getLayers().size() > 0){
				Log.d(TAG, "removing map layer");
				mapView.getLayerManager().getLayers().remove(0);
			}

			MapViewPosition mvp = mapView.getModel().mapViewPosition;		
			mvp.setMapPosition(pMapPosition);
			
			switch (type) {
			case MAPSFORGE:
				
				Layer mapsforgeLayer = new TileRendererLayer(tileCache, mvp, false, true, AndroidGraphicFactory.INSTANCE);
				((TileRendererLayer) mapsforgeLayer).setMapFile(file);
				((TileRendererLayer) mapsforgeLayer).setXmlRenderTheme(InternalRenderTheme.OSMARENDER);
				mapView.getLayerManager().getLayers().add(0, mapsforgeLayer);

				mapView.getModel().mapViewPosition.setZoomLevelMax((byte) RendererType.MAPSFORGE_MAX_ZOOM);
				mapView.getModel().mapViewPosition.setZoomLevelMin((byte) RendererType.MAPSFORGE_MIN_ZOOM);
				
				break;
			case MBTILES:
				
				
				Layer mbTilesLayer = new MBTilesLayer(getBaseContext(), tileCache, mvp, false, AndroidGraphicFactory.INSTANCE, filePath);
				mapView.getLayerManager().getLayers().add(0, mbTilesLayer);
				
//				mapView.getModel().displayModel.setFixedTileSize(256);
				
				mapView.getModel().mapViewPosition.setZoomLevelMax((byte) RendererType.MAPSFORGE_MAX_ZOOM);
				mapView.getModel().mapViewPosition.setZoomLevelMin((byte) RendererType.MAPSFORGE_MIN_ZOOM);
				
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
}
