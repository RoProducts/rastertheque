package de.rooehler.mapboxrenderer;

import java.io.File;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.util.DisplayMetrics;
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

import de.rooehler.mapboxrenderer.R;
import de.rooehler.mapboxrenderer.fileselection.FilePickerDialog;
import de.rooehler.mapboxrenderer.fileselection.SupportedType;
import de.rooehler.mapboxrenderer.fileselection.FilePickerDialog.FilePathPickCallback;
import de.rooehler.mapboxrenderer.renderer.GDALTileLayer;
import de.rooehler.rastertheque.io.gdal.GDALRasterIO;
import de.rooehler.rastertheque.processing.colormap.MColorMapProcessing;



public class MapBoxSampleActivity extends Activity {

	private final static String TAG = MapBoxSampleActivity.class.getSimpleName(); 
	
	final static String PREFS_FILEPATH = "de.rooehler.mapboxrenderer.filepath";
	final static String PREFS_RENDERER_TYPE = "de.rooehler.mapboxrenderer.renderer_type";
	
	private MapView mv;
	private String currentMap = null;
	
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
			{
				new FilePickerDialog(MapBoxSampleActivity.this, "Select a file", SupportedType.getExtensionForType(type), new FilePathPickCallback() {
					
					@Override
					public void filePathPicked(String filePath) {
						
						Log.d(TAG, "path selected "+filePath);
						
						replaceWithMBTiles(filePath);
						
						Editor ed = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit();
						ed.putString(PREFS_FILEPATH, filePath);
						ed.putInt(PREFS_RENDERER_TYPE, type.ordinal());
						ed.commit();
						
					}
				});
				break;
			}
		case RASTER:
		{
			new FilePickerDialog(MapBoxSampleActivity.this, "Select a file", SupportedType.getExtensionForType(type), new FilePathPickCallback() {
				
				@Override
				public void filePathPicked(String filePath) {
					
					Log.d(TAG, "path selected "+filePath);
								
					replaceWithGDAL(filePath);
					
					Editor ed = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit();
					ed.putString(PREFS_FILEPATH, filePath);
					ed.putInt(PREFS_RENDERER_TYPE, type.ordinal());
					ed.commit();
				}
			});
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
		
//    	final String filePath = Environment.getExternalStorageDirectory()+"/rastertheque/HN+24_900913.tif";
//		final String filePath = Environment.getExternalStorageDirectory()+"/rastertheque/GRAY_50M_SR_OB.tif";
//		final String filePath = Environment.getExternalStorageDirectory()+"/rastertheque/soilmoisture.tif";
//    	final String filePath = Environment.getExternalStorageDirectory()+"/rastertheque/dem.tif";
    	
    	DisplayMetrics displaymetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
		int screenWidth = displaymetrics.widthPixels;
		
		final boolean useColorMap = true;
    	
    	GDALRasterIO gdalRaster = new GDALRasterIO(filePath);
		MColorMapProcessing mColorMapProcessing = new MColorMapProcessing(filePath);
		GDALTileLayer source = new GDALTileLayer(getBaseContext(), new File(filePath), gdalRaster, mColorMapProcessing, screenWidth, useColorMap,mv.getProjection());

		mv.setZoom(source.getStartZoomLevel());
		mv.setCenter(source.getCenterCoordinate());

		
	    mv.setTileSource(source);
	    BoundingBox box = source.getBoundingBox();
        mv.setScrollableAreaLimit(box);
        mv.setMinZoomLevel(mv.getTileProvider().getMinimumZoomLevel());
        mv.setMaxZoomLevel(mv.getTileProvider().getMaximumZoomLevel());
		currentMap = filePath;
	}
	
	private void replaceWithMBTiles(final String filePath) {
		
    	MBTilesLayer source = new MBTilesLayer(new File(filePath));
    	
        mv.setTileSource(source);
        BoundingBox box = source.getBoundingBox();
        mv.setScrollableAreaLimit(box);
        mv.setMinZoomLevel(mv.getTileProvider().getMinimumZoomLevel());
        mv.setMaxZoomLevel(mv.getTileProvider().getMaximumZoomLevel());
		currentMap = filePath;
		
	}

	protected void replaceMapView(String layer) {

		if (TextUtils.isEmpty(layer) || TextUtils.isEmpty(currentMap) || currentMap.equalsIgnoreCase(layer)) {
			return;
		}

        ITileLayer source;
        BoundingBox box;

        source = new MapboxTileLayer(layer);

        mv.setTileSource(source);
        box = source.getBoundingBox();
        mv.setScrollableAreaLimit(box);
        mv.setMinZoomLevel(mv.getTileProvider().getMinimumZoomLevel());
        mv.setMaxZoomLevel(mv.getTileProvider().getMaximumZoomLevel());
		currentMap = layer;
		
		
/*
        mv.setCenter(mv.getTileProvider().getCenterCoordinate());
        mv.setZoom(0);
*/
    }

//    private Button changeButtonTypeface(Button button) {
//        return button;
//    }

    public LatLng getMapCenter() {
        return mv.getCenter();
    }

    public void setMapCenter(ILatLng center) {
        mv.setCenter(center);
    }

//    /**
//     * Method to show settings  in alert dialog
//     * On pressing Settings button will lauch Settings Options - GPS
//     */
//    public void showSettingsAlert() {
//        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getBaseContext());
//
//        // Setting Dialog Title
//        alertDialog.setTitle("GPS settings");
//
//        // Setting Dialog Message
//        alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?");
//
//        // On pressing Settings button
//        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int which) {
//                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//                getBaseContext().startActivity(intent);
//            }
//        });
//
//        // on pressing cancel button
//        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int which) {
//                dialog.cancel();
//            }
//        });
//
//        // Showing Alert Message
//        alertDialog.show();
//    }
}