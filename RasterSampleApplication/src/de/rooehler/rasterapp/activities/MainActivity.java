package de.rooehler.rasterapp.activities;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import org.gdal.gdal.Dataset;
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
import org.osgeo.proj4j.CoordinateTransform;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

import de.rooehler.rasterapp.R;
import de.rooehler.rasterapp.dialog.FilePickerDialog;
import de.rooehler.rasterapp.dialog.FilePickerDialog.FilePathPickCallback;
import de.rooehler.rasterapp.dialog.SelectProjectionDialog;
import de.rooehler.rasterapp.dialog.SelectProjectionDialog.IProjectionSelected;
import de.rooehler.rasterapp.rasterrenderer.RasterLayer;
import de.rooehler.rasterapp.rasterrenderer.gdal.GDALMapsforgeRenderer;
import de.rooehler.rasterapp.rasterrenderer.mbtiles.MBTilesMapsforgeRenderer;
import de.rooehler.rasterapp.util.SupportedType;
import de.rooehler.rastertheque.io.gdal.GDALRasterIO;
import de.rooehler.rastertheque.io.mbtiles.MBTilesRasterIO;
import de.rooehler.rastertheque.io.mbtiles.MbTilesDatabase;
import de.rooehler.rastertheque.processing.colormap.MColorMapProcessing;
import de.rooehler.rastertheque.proj.Proj;
import de.rooehler.rastertheque.util.Constants;


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
	
    private ProgressDialog pd;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main_layout);
		
		getActionBar().setBackgroundDrawable(new ColorDrawable(0xffffffff));
		
		mTitle = mDrawerTitle = getTitle();
		
		mapView = (MapView) findViewById(R.id.mapView);
		
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        
        mDrawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, SupportedType.getTypes()));
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,int pos, long id) {
				Log.d(TAG, "selected pos "+ pos);
				
				mDrawerLayout.closeDrawers();
				
				showFileSelectionDialog(SupportedType.values()[pos]);
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
		
		tileCache = AndroidUtil.createTileCache(this,
				"rastertheque/cache/",
				mapView.getModel().displayModel.getTileSize(),
				getResources().getDisplayMetrics().density, //screenratio
				mapView.getModel().frameBufferModel.getOverdrawFactor(),
				true);//delete external cache before start
		
		
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		final SupportedType type = SupportedType.values()[prefs.getInt(PREFS_RENDERER_TYPE, 0)];
		
		String savedFilePath = prefs.getString(PREFS_FILEPATH, null);
		if(savedFilePath == null){
			
			showFileSelectionDialog(type);
			
		}	else{
					
			setMapStyle(type, savedFilePath);
		}			
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
	
	public void showFileSelectionDialog(final SupportedType type){
		
		new FilePickerDialog(MainActivity.this, "Select a file", SupportedType.getExtensionForType(type), new FilePathPickCallback() {
			
			@Override
			public void filePathPicked(String filePath) {
				
				Log.d(TAG, "path selected "+filePath);
																										
				setMapStyle(type, filePath);
				
				Editor ed = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit();
				ed.putString(PREFS_FILEPATH, filePath);
				ed.putInt(PREFS_RENDERER_TYPE, type.ordinal());
				ed.commit();
			}
		});
	}
	
	private void setMapStyle(final SupportedType type, final String filePath) {
		
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
			
			switch (type) {
			case MAPSFORGE:
				//to create a mapsforge mapview layer in version > 0.4.0 a mapviewposition must be known priorly
				//hence it is necessary to analyze the desired position before actually creating the layer
				final MapPosition msfmp = getCenterForMapsforgeFile(filePath);
				final MapViewPosition msfmvp = mapView.getModel().mapViewPosition;		
				msfmvp.setMapPosition(msfmp);
				
				Layer mapsforgeLayer = new TileRendererLayer(tileCache, msfmvp, false, true, AndroidGraphicFactory.INSTANCE);
				((TileRendererLayer) mapsforgeLayer).setMapFile(file);
				((TileRendererLayer) mapsforgeLayer).setXmlRenderTheme(InternalRenderTheme.OSMARENDER);
				mapView.getLayerManager().getLayers().add(0, mapsforgeLayer);

				mapView.getModel().mapViewPosition.setZoomLevelMax((byte) 20);
				mapView.getModel().mapViewPosition.setZoomLevelMin((byte) 8);
				
				break;
			case MBTILES:
				
				final MapPosition mbtmp = getCenterForMBTilesFile(filePath);
				final MapViewPosition mbtmvp = mapView.getModel().mapViewPosition;		
				mbtmvp.setMapPosition(mbtmp);
				
				MBTilesRasterIO mbTilesRaster = new MBTilesRasterIO(getBaseContext(),filePath);
				MBTilesMapsforgeRenderer mbTilesRenderer = new MBTilesMapsforgeRenderer( AndroidGraphicFactory.INSTANCE, mbTilesRaster);
				Layer mbTilesLayer = new RasterLayer(getBaseContext(), tileCache, mbtmvp, false, AndroidGraphicFactory.INSTANCE, mbTilesRenderer);
				mapView.getLayerManager().getLayers().add(0, mbTilesLayer);
								
//				mapView.getModel().mapViewPosition.setZoomLevelMax((byte) mbTilesRaster.getMaxZoom());
//				mapView.getModel().mapViewPosition.setZoomLevelMin((byte) mbTilesRaster.getMinZoom());
				
				break;
				
			case RASTER:
				
				GDALRasterIO gdalRaster = new GDALRasterIO(filePath);
				MColorMapProcessing mColorMapProcessing = new MColorMapProcessing(filePath);
				final MapPosition gdalmp = getCenterForGDALFile(gdalRaster);
				final MapViewPosition gdalmvp = mapView.getModel().mapViewPosition;		
				gdalmvp.setMapPosition(gdalmp);
				
				GDALMapsforgeRenderer gdalFileRenderer = new GDALMapsforgeRenderer(AndroidGraphicFactory.INSTANCE, gdalRaster, mColorMapProcessing, true);
				Layer rasterLayer = new RasterLayer(getBaseContext(),tileCache, gdalmvp, false, AndroidGraphicFactory.INSTANCE, gdalFileRenderer);
				mapView.getLayerManager().getLayers().add(0, rasterLayer);
				
//				mapView.getModel().mapViewPosition.setZoomLevelMax((byte) gdalRaster.getMaxZoom());
//				mapView.getModel().mapViewPosition.setZoomLevelMin((byte) gdalRaster.getMinZoom());
				
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
			
			invalidateOptionsMenu();
			
		}catch(Exception e){
			Log.e(TAG, "error setting map style",e);
		}
		
	}
	
	public MapPosition getCenterForMapsforgeFile(final String filePath){
		
		final File file = new File(filePath);
		Log.i(SupportedType.class.getSimpleName(), "Map file is " + file.getAbsolutePath()+ " file exists "+Boolean.toString(file.exists()));
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
					Log.e(SupportedType.class.getSimpleName(), "could not retrieve bounding box center position for "+fileName);
				}
			}else{
				Log.e(SupportedType.class.getSimpleName(), "could not retrieve map start position for "+fileName);
				return new MapPosition(new LatLong(0,0),(byte) 8);
			}
		}
		throw new IllegalArgumentException("Invalid Map File " + fileName); 
	}
	
	public MapPosition getCenterForMBTilesFile(final String filePath){
		
		MbTilesDatabase db = new MbTilesDatabase(getBaseContext(), filePath);
		db.openDataBase();
		Coordinate coord = db.getEnvelope().centre();
		LatLong loc = new LatLong(coord.y, coord.x);
		int[] zoomMinMax = db.getMinMaxZoom(); 
		db.close();
		db = null;
		byte zoom = zoomMinMax == null ? (byte) 8 : (byte) zoomMinMax[0];
		return new MapPosition(loc, zoom);
	}
	
	public MapPosition getCenterForGDALFile(GDALRasterIO raster){

		Coordinate center = raster.getCenterPoint();
		Log.d(TAG, "normal center +" + center.x + " "+center.y);
		LatLong latLong = new LatLong(center.y, center.x);
		
//		Envelope rasterEnvelope = raster.getEnvelope();
//		Log.d(TAG, "raster envelope " + rasterEnvelope.toString());
//		Envelope reprojectedEnvelope = Proj.reproject(rasterEnvelope, raster.getCRS(),Proj.EPSG_4326);
//		 
//		Coordinate reprojectedCoord =  reprojectedEnvelope.centre();
//		Log.d(TAG, "reprojected envelope " + reprojectedEnvelope.toString());
//		Log.d(TAG, "reprojected center " + reprojectedCoord.x + " "+reprojectedCoord.y);
//		LatLong latLong = new LatLong(reprojectedCoord.y, reprojectedCoord.x);
		
		final int tileSize = mapView.getModel().displayModel.getTileSize();
		
		final byte zoomLevel = raster.getStartZoomLevel(center,tileSize);
		Log.d(TAG, "calculated zoom "+zoomLevel);
		
		return new MapPosition(latLong, zoomLevel);
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
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
//        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
    	boolean isEditableMap = isEditableMap();
    	
    	menu.findItem(R.id.menu_transform).setVisible(isEditableMap);
    	menu.findItem(R.id.menu_colormap).setVisible(isEditableMap);
        menu.findItem(R.id.menu_save).setVisible(isEditableMap);
        
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
        
        case R.id.menu_transform:
        		if(isEditableMap()){
        			SelectProjectionDialog.showProjectionSelectionDialog(MainActivity.this, new IProjectionSelected() {
						
						@Override
						public void selected(String proj) {
							
							GDALMapsforgeRenderer renderer = ((GDALMapsforgeRenderer) ((RasterLayer) mapView.getLayerManager().getLayers().get(0)).getRasterRenderer());
							Log.d(TAG, "Selected : "+proj);
							tileCache.destroy();
							renderer.getRaster().applyProjection(proj);
							mapView.getLayerManager().redrawLayers();
						}
					});
        		}
        	break;
        case R.id.menu_colormap:
        		if(isEditableMap()){
        			GDALMapsforgeRenderer renderer = ((GDALMapsforgeRenderer) ((RasterLayer) mapView.getLayerManager().getLayers().get(0)).getRasterRenderer());
        			renderer.toggleUseColorMap();
        			tileCache.destroy();
        			Toast.makeText(getBaseContext(), "Color Mode toggled",Toast.LENGTH_SHORT).show();
        			
        			mapView.getLayerManager().redrawLayers();
        		}
        	break;
        case R.id.menu_save:
        	if(isEditableMap()){
        		
        		GDALMapsforgeRenderer renderer = ((GDALMapsforgeRenderer) ((RasterLayer) mapView.getLayerManager().getLayers().get(0)).getRasterRenderer());
    			
        		final Callable<Dataset> c = renderer.getRaster().saveCurrentProjectionToFile("reproject.tif");
    			
    			pd = new ProgressDialog(this);
        		pd.setCancelable(false);
        		pd.setTitle(getString(R.string.app_name));
        		pd.setMessage("Saving");
        		pd.setIcon(R.drawable.ic_launcher);
        		pd.setCanceledOnTouchOutside(false);
        		pd.show();
    			
        		FutureTask<Dataset> future = new FutureTask<Dataset>(c);
        	 
        	    ExecutorService executor = Executors.newFixedThreadPool(1);
        	    executor.execute(future);
        	    
        	    while (true) {
                    try {
                        if(future.isDone() ){
                            Log.d(TAG, "done saving");
                            executor.shutdown();
                            break;
                        }
                         
                        if(!future.isDone()){
                        //wait indefinitely for future task to complete
                        	Log.d(TAG, "FutureTask "+future.get());
                        }
  
                    } catch (InterruptedException | ExecutionException e) {
                    	Log.e(TAG, "error");
                    }
                }
    			pd.dismiss();		
    					
        	}

        }
        return super.onOptionsItemSelected(item);
    }
    
    boolean isEditableMap(){
    	return mapView.getLayerManager().getLayers().size() > 0 &&
 	   mapView.getLayerManager().getLayers().get(0) instanceof RasterLayer && 
 	   ((RasterLayer) mapView.getLayerManager().getLayers().get(0)).getRasterRenderer() instanceof GDALMapsforgeRenderer;
    }
}
