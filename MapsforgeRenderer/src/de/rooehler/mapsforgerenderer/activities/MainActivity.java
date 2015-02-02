package de.rooehler.mapsforgerenderer.activities;

import java.io.File;
import java.io.IOException;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.util.MercatorProjection;
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
import org.osgeo.proj4j.CoordinateReferenceSystem;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.DisplayMetrics;
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

import de.rooehler.mapsforgerenderer.R;
import de.rooehler.mapsforgerenderer.dialog.AlertFactory;
import de.rooehler.mapsforgerenderer.dialog.FilePickerDialog;
import de.rooehler.mapsforgerenderer.dialog.FilePickerDialog.FilePathPickCallback;
import de.rooehler.mapsforgerenderer.dialog.SelectProjectionDialog;
import de.rooehler.mapsforgerenderer.dialog.SelectProjectionDialog.IProjectionSelected;
import de.rooehler.mapsforgerenderer.interfaces.IWorkStatus;
import de.rooehler.mapsforgerenderer.rasterrenderer.RasterLayer;
import de.rooehler.mapsforgerenderer.rasterrenderer.gdal.GDALMapsforgeRenderer;
import de.rooehler.mapsforgerenderer.rasterrenderer.mbtiles.MBTilesMapsforgeRenderer;
import de.rooehler.mapsforgerenderer.util.SupportedType;
import de.rooehler.rastertheque.core.Dataset;
import de.rooehler.rastertheque.io.gdal.GDALDataset;
import de.rooehler.rastertheque.io.gdal.GDALDriver;
import de.rooehler.rastertheque.io.mbtiles.MBTilesDataset;
import de.rooehler.rastertheque.io.mbtiles.MBTilesDriver;
import de.rooehler.rastertheque.processing.Renderer;
import de.rooehler.rastertheque.processing.Resampler;
import de.rooehler.rastertheque.processing.rendering.MRenderer;
import de.rooehler.rastertheque.processing.resampling.raw.OpenCVRawResampler;
import de.rooehler.rastertheque.processing.resampling.rendered.MResampler;
import de.rooehler.rastertheque.processing.resampling.rendered.OpenCVResampler;
import de.rooehler.rastertheque.proj.Proj;


public class MainActivity extends Activity implements IWorkStatus{
	
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
    
    private boolean isRendering = false;
    
    private Dataset ds;
    
    private long now;
    

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main_layout);
		
		getActionBar().setBackgroundDrawable(new ColorDrawable(0xffffffff));
		
		
		mapView = (MapView) findViewById(R.id.mapView);
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
		
		FilePickerDialog fpd = null;
		
		final FilePathPickCallback callback = new FilePathPickCallback() {
			
			@Override
			public void filePathPicked(String filePath) {
				Log.d(TAG, "path selected "+filePath);
				
				setMapStyle(type, filePath);
				
				Editor ed = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit();
				ed.putString(PREFS_FILEPATH, filePath);
				ed.putInt(PREFS_RENDERER_TYPE, type.ordinal());
				ed.commit();
			}
		};
		

		fpd = new FilePickerDialog(this, "Select a file", SupportedType.getExtensions(type), callback);
		
		
		fpd.show();

	}

	private void setMapStyle(final SupportedType type, final String filePath) {

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
			mapView.getMapScaleBar().setVisible(true);
			break;
		case MBTILES:

			final MBTilesDriver mbtdriver = new MBTilesDriver();
			if(!mbtdriver.canOpen(filePath)){
				Log.e(TAG, "cannot open "+ filePath+" with MBTiles driver");
				return;
			}
			
			try {
				final MBTilesDataset mbTilesDataset = mbtdriver.open(filePath);
				
				final Coordinate coord = mbTilesDataset.getBoundingBox().centre();
				LatLong loc = new LatLong(coord.y, coord.x);
				int[] zoomMinMax = mbTilesDataset.getMinMaxZoom(); 
				byte zoom = zoomMinMax == null ? (byte) 8 : (byte) zoomMinMax[0];
				final MapPosition mbtmp = new MapPosition(loc, zoom);
				final MapViewPosition mbtmvp = mapView.getModel().mapViewPosition;		
				mbtmvp.setMapPosition(mbtmp);

				final Resampler resampler = new MResampler();
				MBTilesMapsforgeRenderer mbTilesRenderer = new MBTilesMapsforgeRenderer( AndroidGraphicFactory.INSTANCE, mbTilesDataset, resampler);
				Layer mbTilesLayer = new RasterLayer(getBaseContext(), tileCache, mbtmvp, false, AndroidGraphicFactory.INSTANCE, mbTilesRenderer, this);
				Log.d(TAG, "setting max to "+zoomMinMax[1]+ " min to "+ zoomMinMax[0]);
				mapView.getLayerManager().getLayers().add(0, mbTilesLayer);
				mapView.getModel().mapViewPosition.setZoomLevelMax((byte) zoomMinMax[1]);
				mapView.getModel().mapViewPosition.setZoomLevelMin((byte) zoomMinMax[0]);
				mapView.getMapScaleBar().setVisible(false);
				
			} catch (IOException e) {
				Log.e(TAG, "error opening "+ filePath+" with MBTiles driver");
			}
			break;

		case RASTER:
						
			GDALDriver driver = new GDALDriver();

			try{
				if(driver.canOpen(filePath)){
					
					final GDALDataset newDs = driver.open(filePath);
			        
			        if(newDs.getCRS() == null){      	
			        	AlertFactory.showErrorAlert(this, "No CRS ", "No CRS available for the file : \n"+filePath.substring(filePath.lastIndexOf("/") + 1) +"\n\nCannot show it");
			        	newDs.close();
			        	return;
			        }
			        if(newDs.getBoundingBox() == null){
			        	AlertFactory.showErrorAlert(this, "No BoundingBox", "No BoundingBox available for the file : \n"+filePath.substring(filePath.lastIndexOf("/") + 1) +"\n\nCannot show it");
			        	newDs.close();
			        	return;
			        }
			       //this dataset is okay, close any earlier opened
					if(ds != null){
						ds.close();
					}
					
			        ds = newDs;
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
			
			boolean raw = false;
 			Resampler resampler = raw == true ? new OpenCVRawResampler() : new OpenCVResampler();
			
			GDALMapsforgeRenderer gdalFileRenderer = new GDALMapsforgeRenderer(AndroidGraphicFactory.INSTANCE,((GDALDataset) ds), renderer,resampler, true);
			//TODO refactor the initial zoom calculation
			final int tileSize = mapView.getModel().displayModel.getTileSize();
			DisplayMetrics displaymetrics = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
			int width = displaymetrics.widthPixels;
			byte startZoomLevel = gdalFileRenderer.calculateStartZoomLevel(tileSize,width);
			
			final Envelope dim = ((GDALDataset) ds).getDimension();
			final int h = (int) dim.getHeight();
			final int w = (int) dim.getWidth();
			Log.v(TAG, "width : "+w + " height : "+ h);
			
			final MapPosition gdalmp =  new MapPosition(calculateStartPositionForRaster(w, h),startZoomLevel);
			final MapViewPosition gdalmvp = mapView.getModel().mapViewPosition;		
			gdalmvp.setMapPosition(gdalmp);
			mapView.getModel().mapViewPosition.setMapPosition(gdalmp);

			byte zoomLevelMax = gdalFileRenderer.calculateZoomLevelsAndStartScale(tileSize, width, w, h);
			Layer rasterLayer = new RasterLayer(getBaseContext(),tileCache, gdalmvp, false, AndroidGraphicFactory.INSTANCE, gdalFileRenderer, this);
			mapView.getLayerManager().getLayers().add(0, rasterLayer);
			Log.d(TAG, "setting max to "+zoomLevelMax+ " min to "+ startZoomLevel);
			mapView.getModel().mapViewPosition.setZoomLevelMin(startZoomLevel);
			mapView.getModel().mapViewPosition.setZoomLevelMax(zoomLevelMax);
			mapView.getMapScaleBar().setVisible(false);
			break;

		default:
			break;
		}


		if(mapView.getLayerManager().getLayers().size() > 0 ){
			mapView.setClickable(true);
			mapView.setBuiltInZoomControls(true);
			
		}else{
			Log.e(TAG, "no layers created");
		}

		invalidateOptionsMenu();

	}
	
	private LatLong calculateStartPositionForRaster(int rasterWidth, int rasterHeight) {
		
		if(rasterWidth == rasterHeight){
			return new LatLong(0, 0);
		}else if(rasterWidth > rasterHeight){
			//wider --> increase lat
			float ratio = 1- ((float) rasterHeight / rasterWidth);
			Log.d(TAG, "calculated start pos : { "+(MercatorProjection.LATITUDE_MAX * ratio)+",0}");
			return new LatLong(MercatorProjection.LATITUDE_MAX * ratio, 0);			
		}else{
			//taller
			float ratio = 1- ((float)rasterWidth / rasterHeight);
			return new LatLong(0, 180 * ratio);
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
        if(isRendering){
        	menu.findItem(R.id.menu_refresh).setActionView(  R.layout.actionbar_indeterminate_progress);
        }else{
        	menu.findItem(R.id.menu_refresh).setActionView(null);
        }
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
//        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
    	boolean isEditableMap = isEditableMap();
    	
    	menu.findItem(R.id.menu_transform).setVisible(isEditableMap);
    	menu.findItem(R.id.menu_colormap).setVisible(isEditableMap);
//        menu.findItem(R.id.menu_save).setVisible(isEditableMap);
        
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        
        switch(item.getItemId()) {
        
        case R.id.menu_transform:
        		if(isEditableMap()){
        			final GDALMapsforgeRenderer renderer = ((GDALMapsforgeRenderer) ((RasterLayer) mapView.getLayerManager().getLayers().get(0)).getRasterRenderer());
        			
        			final CoordinateReferenceSystem currentCrs = renderer.getCurrentCRS();
        			
        			if(currentCrs != null){
        				
        				final String crs = Proj.toWKT(currentCrs, true);

        				SelectProjectionDialog.showProjectionSelectionDialog(MainActivity.this,crs, new IProjectionSelected() {

        					@Override
        					public void selected(String proj) {

        						Log.d(TAG, "Selected : "+proj);
        						tileCache.destroy();
        						renderer.setDesiredCRS(proj);
        						mapView.getLayerManager().redrawLayers();
        					}
        				});
        			}else{
        				Toast.makeText(getBaseContext(), "Error retrieving the current crs, cannot transform!", Toast.LENGTH_SHORT).show();
        			}
        		}
        	break;
        case R.id.menu_colormap:
        		if(isEditableMap()){
        			GDALMapsforgeRenderer renderer = ((GDALMapsforgeRenderer) ((RasterLayer) mapView.getLayerManager().getLayers().get(0)).getRasterRenderer());
        			if(renderer.canSwitchColorMap()){
        				
        				renderer.toggleUseColorMap();
        				tileCache.destroy();
        				Toast.makeText(getBaseContext(), "Color Mode toggled",Toast.LENGTH_SHORT).show();
        			}else{
        				Toast.makeText(getBaseContext(), "Cannot toggle color mode for this raster",Toast.LENGTH_SHORT).show();        				
        			}
        			
        			mapView.getLayerManager().redrawLayers();
        		}
        	break;
//        case R.id.menu_save:
//        	if(isEditableMap()){
//        		
//        		GDALMapsforgeRenderer renderer = ((GDALMapsforgeRenderer) ((RasterLayer) mapView.getLayerManager().getLayers().get(0)).getRasterRenderer());
//    			
//        		final Callable<Dataset> c = renderer.getRaster().saveCurrentProjectionToFile("reproject.tif");
//    			
//    			pd = new ProgressDialog(this);
//        		pd.setCancelable(false);
//        		pd.setTitle(getString(R.string.app_name));
//        		pd.setMessage("Saving");
//        		pd.setIcon(R.drawable.ic_launcher);
//        		pd.setCanceledOnTouchOutside(false);
//        		pd.show();
//    			
//        		FutureTask<Dataset> future = new FutureTask<Dataset>(c);
//        	 
//        	    ExecutorService executor = Executors.newFixedThreadPool(1);
//        	    executor.execute(future);
//        	    
//        	    while (true) {
//                    try {
//                        if(future.isDone() ){
//                            Log.d(TAG, "done saving");
//                            executor.shutdown();
//                            break;
//                        }
//                         
//                        if(!future.isDone()){
//                        //wait indefinitely for future task to complete
//                        	Log.d(TAG, "FutureTask "+future.get());
//                        }
//  
//                    } catch (InterruptedException | ExecutionException e) {
//                    	Log.e(TAG, "error");
//                    }
//                }
//    			pd.dismiss();		
//    					
//        	}

        }
        return super.onOptionsItemSelected(item);
    }
    
    boolean isEditableMap(){
    	return mapView.getLayerManager().getLayers().size() > 0 &&
 	   mapView.getLayerManager().getLayers().get(0) instanceof RasterLayer && 
 	   ((RasterLayer) mapView.getLayerManager().getLayers().get(0)).getRasterRenderer() instanceof GDALMapsforgeRenderer;
    }

    @Override
    public void isRendering() {

    	if(!isRendering){	
    		runOnUiThread(new Runnable() {		
    			@Override
    			public void run() {
    				isRendering = true;
    				invalidateOptionsMenu();
    				now = System.currentTimeMillis();

    			}
    		});
    	}
    }

    @Override
    public void renderingFinished() {

    	if(isRendering){	
    		runOnUiThread(new Runnable() {
    			@Override
    			public void run() {
    				isRendering = false;
    				invalidateOptionsMenu();
    				Log.d(TAG, "This operation took "+ (System.currentTimeMillis() - now) / 1000f +" s");
    			}
    		});
    	}
    }
}
