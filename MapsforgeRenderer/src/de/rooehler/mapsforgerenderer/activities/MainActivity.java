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

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.Rect;
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

import com.vividsolutions.jts.geom.Coordinate;

import de.rooehler.mapsforgerenderer.R;
import de.rooehler.mapsforgerenderer.dialog.AlertFactory;
import de.rooehler.mapsforgerenderer.dialog.FilePickerDialog;
import de.rooehler.mapsforgerenderer.dialog.FilePickerDialog.FilePathPickCallback;
import de.rooehler.mapsforgerenderer.interfaces.IWorkStatus;
import de.rooehler.mapsforgerenderer.rasterrenderer.RasterLayer;
import de.rooehler.mapsforgerenderer.rasterrenderer.gdal.GDALMapsforgeRenderer;
import de.rooehler.mapsforgerenderer.rasterrenderer.mbtiles.MBTilesMapsforgeRenderer;
import de.rooehler.mapsforgerenderer.util.SupportedType;
import de.rooehler.rastertheque.core.Dataset;
import de.rooehler.rastertheque.core.Drivers;
import de.rooehler.rastertheque.io.gdal.GDALDataset;
import de.rooehler.rastertheque.io.gdal.GDALDriver;
import de.rooehler.rastertheque.io.mbtiles.MBTilesDataset;
import de.rooehler.rastertheque.proj.Proj;

/**
 * Main and sole activity of the Mapsforge Sample Application
 * 
 * contains a listdrawer which enables the user to select a "filetype"
 * file are divided into the categories "Mapsforge, MBTiles and Raster"
 * 
 * Mapsforge files ".map" are handled with the native TileLayer
 * MBTiles and Raster with custom implementations
 * 
 * @author Robert Oehler
 *
 */
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

        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.ic_drawer,  /* nav drawer image to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description for accessibility */
                R.string.drawer_close  /* "close drawer" description for accessibility */
                ) {
            public void onDrawerClosed(View view) {
                getActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); 
            }

            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu();
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

			Dataset mbtilesDataset = null;
			try{
				mbtilesDataset = Drivers.open(filePath, null);
			}catch(IOException e){
				Log.e(TAG, "error opening file "+filePath);
				AlertFactory.showAlert(this, "Error", "There was an error opening the file : \n"+filePath.substring(filePath.lastIndexOf("/") + 1));
				return;
			}

			final Coordinate coord = mbtilesDataset.getBoundingBox().centre();
			LatLong loc = new LatLong(coord.y, coord.x);
			int[] zoomMinMax = ((MBTilesDataset)mbtilesDataset).getMinMaxZoom(); 
			byte zoom = zoomMinMax == null ? (byte) 8 : (byte) zoomMinMax[0];
			final MapPosition mbtmp = new MapPosition(loc, zoom);
			final MapViewPosition mbtmvp = mapView.getModel().mapViewPosition;		
			mbtmvp.setMapPosition(mbtmp);

			MBTilesMapsforgeRenderer mbTilesRenderer = new MBTilesMapsforgeRenderer( AndroidGraphicFactory.INSTANCE, ((MBTilesDataset)mbtilesDataset));
			Layer mbTilesLayer = new RasterLayer(getBaseContext(), tileCache, mbtmvp, false, AndroidGraphicFactory.INSTANCE, mbTilesRenderer, this);
			Log.d(TAG, "setting max to "+zoomMinMax[1]+ " min to "+ zoomMinMax[0]);
			mapView.getLayerManager().getLayers().add(0, mbTilesLayer);
			mapView.getModel().mapViewPosition.setZoomLevelMax((byte) zoomMinMax[1]);
			mapView.getModel().mapViewPosition.setZoomLevelMin((byte) zoomMinMax[0]);
			mapView.getMapScaleBar().setVisible(false);

			break;

		case RASTER:
			
			Dataset gdalDataset = null;
			try{
				gdalDataset = Drivers.open(filePath, null);
			}catch(IOException e){
				Log.e(TAG, "error opening file "+filePath);
				AlertFactory.showAlert(this, "Error", "There was an error opening the file : \n"+filePath.substring(filePath.lastIndexOf("/") + 1));
				return;
			}

			if(gdalDataset != null){

				if(gdalDataset.getCRS() == null){      	
					AlertFactory.showAlert(this, "No CRS ", "No CRS available for the file : \n"+filePath.substring(filePath.lastIndexOf("/") + 1) +"\n\nCannot show it");
					gdalDataset.close();
					return;
				}else{
					//if it is not 900913 transform to 900913
					if(!gdalDataset.getCRS().equals(Proj.EPSG_900913)){
						Log.i(TAG, "reprojecting to EPSG 900913");
						org.gdal.gdal.Dataset reproj = ((GDALDataset)gdalDataset).transform(Proj.proj2wkt(Proj.EPSG_900913.getParameterString()));
						gdalDataset = new GDALDataset(gdalDataset.getSource(), reproj, (GDALDriver)gdalDataset.getDriver());
					}
				}
				if(gdalDataset.getBoundingBox() == null){
					AlertFactory.showAlert(this, "No BoundingBox", "No BoundingBox available for the file : \n"+filePath.substring(filePath.lastIndexOf("/") + 1) +"\n\nCannot show it");
					gdalDataset.close();
					return;
				}
				//this dataset is okay, close any earlier opened
				if(ds != null){
					ds.close();
				}

				ds = gdalDataset;
			}else{
				Log.w(TAG, "cannot open file "+filePath);
				AlertFactory.showAlert(this, "No Driver", "No Driver could open the file : \n"+filePath.substring(filePath.lastIndexOf("/") + 1));
				return;
			}

			GDALMapsforgeRenderer gdalFileRenderer = new GDALMapsforgeRenderer(AndroidGraphicFactory.INSTANCE,((GDALDataset) ds));
			
			
			final int tileSize = mapView.getModel().displayModel.getTileSize();
			DisplayMetrics displaymetrics = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
			int width = displaymetrics.widthPixels;
			byte startZoomLevel = gdalFileRenderer.calculateStartZoomLevel(tileSize,width);
			
			final Rect dim = ((GDALDataset) ds).getDimension();
			final int w  = dim.width();
			final int h = dim.height();
			Log.v(TAG, "width : "+w + " height : "+ h);
			
			//this sample implementation follows the policy to show raster files in their complete extent
			// i.e. --> the file is internally zoomed 
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

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
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
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        
        return super.onOptionsItemSelected(item);
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
