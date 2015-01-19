package de.rooehler.mapboxrenderer;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.tileprovider.tilesource.ITileLayer;
import com.mapbox.mapboxsdk.tileprovider.tilesource.MBTilesLayer;
import com.mapbox.mapboxsdk.tileprovider.tilesource.MapboxTileLayer;
import com.mapbox.mapboxsdk.views.MapView;

import de.rooehler.mapboxrenderer.fileselection.FilePickerDialog;
import de.rooehler.mapboxrenderer.fileselection.FilePickerDialog.FilePathPickCallback;
import de.rooehler.mapboxrenderer.fileselection.SupportedType;
import de.rooehler.mapboxrenderer.renderer.GDALTileLayer;
import de.rooehler.rastertheque.io.gdal.GDALDataset;
import de.rooehler.rastertheque.io.gdal.GDALDriver;
import de.rooehler.rastertheque.processing.Renderer;
import de.rooehler.rastertheque.processing.Resampler;
import de.rooehler.rastertheque.processing.rendering.MRenderer;
import de.rooehler.rastertheque.processing.resampling.OpenCVResampler;



public class MapBoxSampleActivity extends Activity {

	private final static String TAG = MapBoxSampleActivity.class.getSimpleName(); 
	
	final static String PREFS_FILEPATH = "de.rooehler.mapboxrenderer.filepath";
	final static String PREFS_RENDERER_TYPE = "de.rooehler.mapboxrenderer.renderer_type";
	
	private MapView mv;
	private String currentMap = null;
	private ITileLayer mCurrentLayer;
	
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;
	private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.mapbox);	

		mv = (MapView) findViewById(R.id.mapview);
		mv.setMinZoomLevel(mv.getTileProvider().getMinimumZoomLevel());
		mv.setMaxZoomLevel(mv.getTileProvider().getMaximumZoomLevel());
		mv.setCenter(mv.getTileProvider().getCenterCoordinate());
		mv.setZoom(0);
		currentMap = getString(R.string.streetMapId);
		
		mTitle = mDrawerTitle = getTitle();
		
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        
        mDrawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, SupportedType.getTypes()));
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,int pos, long id) {
				Log.d(TAG, "selected pos "+ pos);
				
				mDrawerLayout.closeDrawers();
				
				applyMapStyle(pos);
				
				
			}
		});
        
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
        
    	final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		final SupportedType type = SupportedType.values()[prefs.getInt(PREFS_RENDERER_TYPE, 0)];
		
		String savedFilePath = prefs.getString(PREFS_FILEPATH, null);
		if(type == SupportedType.RASTER && savedFilePath != null){
			
			replaceWithGDAL(savedFilePath);
			
		}else if(type == SupportedType.MBTILES && savedFilePath !=null){
					
			replaceWithMBTiles(savedFilePath);
		}	

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
    public boolean onOptionsItemSelected(MenuItem item) {
         // The action bar home/up action should open or close the drawer.
         // ActionBarDrawerToggle will take care of this.
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return true;
    }

	
	public void applyMapStyle(int pos) {
		
		final SupportedType type = SupportedType.values()[pos];
		
		switch(type){
		
		case MBTILES:
		case RASTER:
			{
				final FilePathPickCallback callback = new FilePathPickCallback() {
					
					@Override
					public void filePathPicked(String filePath) {
						
						Log.d(TAG, "path selected "+filePath);
						
						if(type == SupportedType.RASTER){
							replaceWithGDAL(filePath);
						}else if(type == SupportedType.MBTILES){							
							replaceWithMBTiles(filePath);
						}						
						
						Editor ed = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit();
						ed.putString(PREFS_FILEPATH, filePath);
						ed.putInt(PREFS_RENDERER_TYPE, type.ordinal());
						ed.commit();
						
					}
				};

				final FilePickerDialog fpd  = new FilePickerDialog(this, "Select a file", SupportedType.getExtensions(type), callback);
				
				fpd.show();
				
				break;
			}
		case Outdoors:
			replaceMapView(getString(R.string.outdoorsMapId));
			break;
		case Pencil:
			replaceMapView(getString(R.string.pencilMapId));
			break;
		case Satellite:
			replaceMapView(getString(R.string.satelliteMapId));
			break;
		case SpaceShip:
			replaceMapView(getString(R.string.spaceShipMapId));
			break;
		case Street:
			replaceMapView(getString(R.string.streetMapId));
			break;
		case Terrain:
			replaceMapView(getString(R.string.terrainMapId));
			break;
		case Woodcut:
			replaceMapView(getString(R.string.woodcutMapId));
			break;
		default:
			break;
		
		}
	}

	private void replaceWithGDAL(final String filePath) {
    	

		final boolean useColorMap = true;

		final GDALDriver driver = new GDALDriver();
		
		GDALDataset dataset = null;
		try{ //try to open the file and check if it is valid (has a projection and a bounding box)
			if(driver.canOpen(filePath)){
				
				dataset = driver.open(filePath);
		        
		        if(dataset.getCRS() == null){      	
		        	AlertFactory.showErrorAlert(this, "No CRS ", "No CRS available for the file : \n"+filePath.substring(filePath.lastIndexOf("/") + 1) +"\n\nCannot show it");
		        	dataset.close();
		        	return;
		        }
		        if(dataset.getBoundingBox() == null){
		        	AlertFactory.showErrorAlert(this, "No BoundingBox", "No BoundingBox available for the file : \n"+filePath.substring(filePath.lastIndexOf("/") + 1) +"\n\nCannot show it");
		        	dataset.close();
		        	return;
		        }
		        //this dataset is okay, close any earlier opened
				if(mCurrentLayer != null && mCurrentLayer instanceof GDALTileLayer){
					((GDALTileLayer) mCurrentLayer).close();
				}
			}else{
				Log.w(TAG, "cannot open file "+filePath);
				AlertFactory.showErrorAlert(this, "No Driver", "No Driver could open the file : \n"+filePath.substring(filePath.lastIndexOf("/") + 1));
				return;
			}
		}catch(IOException e){
			Log.e(TAG, "error opening file "+filePath);
			AlertFactory.showErrorAlert(this, "Error", "There was an error opening the file : \n"+filePath.substring(filePath.lastIndexOf("/") + 1));
			return;
		}
		Renderer renderer = new MRenderer(filePath, true);
		Resampler resampler = new OpenCVResampler();
		
		mCurrentLayer = new GDALTileLayer(new File(filePath), dataset, resampler, renderer, useColorMap);

		Log.e(TAG, "setting zoom for new file to "+ (((GDALTileLayer) mCurrentLayer).getStartZoomLevel()));
		mv.setZoom(((GDALTileLayer) mCurrentLayer).getStartZoomLevel());
		mv.setCenter(mCurrentLayer.getCenterCoordinate());
		
		BoundingBox box = mCurrentLayer.getBoundingBox();
		mv.setScrollableAreaLimit(box);
		mv.setMinZoomLevel(mv.getTileProvider().getMinimumZoomLevel());
		mv.setMaxZoomLevel(mv.getTileProvider().getMaximumZoomLevel());
		currentMap = filePath;
		
		mv.setTileSource(mCurrentLayer);
		
		//DEBUG
		MapView.setDebugMode(true);
	}
	
	/**
	 * MapBox provides is "own" MBTiles driver, no need to use rastertheque
	 * @param filePath
	 */
	private void replaceWithMBTiles(final String filePath) {
		
		mCurrentLayer = new MBTilesLayer(new File(filePath));
    	
        mv.setTileSource(mCurrentLayer);
        BoundingBox box = mCurrentLayer.getBoundingBox();
        mv.setScrollableAreaLimit(box);
        mv.setMinZoomLevel(mv.getTileProvider().getMinimumZoomLevel());
        mv.setMaxZoomLevel(mv.getTileProvider().getMaximumZoomLevel());
		currentMap = filePath;
		
	}

	protected void replaceMapView(String layer) {

		if (TextUtils.isEmpty(layer) || TextUtils.isEmpty(currentMap) || currentMap.equalsIgnoreCase(layer)) {
			return;
		}


        mCurrentLayer = new MapboxTileLayer(layer);

        mv.setTileSource(mCurrentLayer);
        mv.setScrollableAreaLimit(mCurrentLayer.getBoundingBox());
        mv.setMinZoomLevel(mv.getTileProvider().getMinimumZoomLevel());
        mv.setMaxZoomLevel(mv.getTileProvider().getMaximumZoomLevel());
		currentMap = layer;
		

    }

    public LatLng getMapCenter() {
        return mv.getCenter();
    }

    public void setMapCenter(ILatLng center) {
        mv.setCenter(center);
    }
}
