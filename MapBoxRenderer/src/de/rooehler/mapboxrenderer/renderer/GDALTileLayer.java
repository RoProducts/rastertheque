package de.rooehler.mapboxrenderer.renderer;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import uk.co.senab.bitmapcache.CacheableBitmapDrawable;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.tileprovider.MapTile;
import com.mapbox.mapboxsdk.tileprovider.modules.MapTileDownloader;
import com.mapbox.mapboxsdk.tileprovider.tilesource.TileLayer;
import com.mapbox.mapboxsdk.views.util.Projection;
import com.vividsolutions.jts.geom.Envelope;

import de.rooehler.rastertheque.core.Band.Color;
import de.rooehler.rastertheque.core.DataType;
import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.core.RasterDataset;
import de.rooehler.rastertheque.core.RasterQuery;
import de.rooehler.rastertheque.io.gdal.GDALDataset;
import de.rooehler.rastertheque.processing.Renderer;
import de.rooehler.rastertheque.processing.RenderingHints;
import de.rooehler.rastertheque.processing.RenderingHints.Key;
import de.rooehler.rastertheque.processing.Resampler;
import de.rooehler.rastertheque.processing.Resampler.ResampleMethod;
import de.rooehler.rastertheque.util.Constants;
import de.rooehler.rastertheque.util.Formulae;
/**
 * A GDALTileLayer extends the Mapbox TileLayer to extract Tiles out of the
 * GDAL raster data
 * 
 * @author Robert Oehler
 *
 */
public class GDALTileLayer extends TileLayer {
	
	private final static String TAG = GDALTileLayer.class.getSimpleName();
	
	private byte mInternalZoom = 1;
	
	private GDALDataset mRasterDataset;
	
	private Renderer mRenderer;
	
	private Resampler mResampler;
	
	private int mRasterBandCount = 1;
	
	private final String mSource;
	
	private int mStartZoomLevel;
	
	private LatLng mStartPos;
	
	private int mTileSize;
	
	private static final int NO_DATA_COLOR = 0xff000000;
	/**
	 * A TileLayer which provides Tiles of a corresponding Raster file
	 * 
	 * To change the number of executing threads look for NUMBER_OF_TILE_DOWNLOAD_THREADS in com.mapbox.mapboxsdk.tileprovider.constants
	 * 
	 * @param file the raster file (explicitely needed to conform to the super class TileLayer)
	 * @param dataset the dataset of this file
	 * @param pResampler the resampler used by this TileLayer
	 * @param pRenderer the renderer used by this TileLayer
	 */
	public GDALTileLayer(final File file, final GDALDataset dataset,final Resampler pResampler,final Renderer pRenderer) {
		super(file.getName(), file.getAbsolutePath());

		mSource = dataset.getSource();

		mRasterDataset = dataset;

		mRenderer = pRenderer;
		
		mResampler = pResampler;

		initialize();
	}	

	/**
     * initializes this layer by setting up the initial zoom, internal scale and the boundingbox
     */
    private void initialize() {
    	
		this.mTileSize = getTileSizePixels();

		this.mRasterBandCount = this.mRasterDataset.getBands().size();

		if(mRasterBandCount == 3){
			mRenderer.useRGBBands( checkIfHasRGBBands());
		}
    	
		final Envelope bb = mRasterDataset.getBoundingBox();

		final LatLng sw = new LatLng(bb.getMinY(),bb.getMinX()); 
		final LatLng ne = new LatLng(bb.getMaxY(),bb.getMaxX()); 

		//meters per pixel of this raster --> distance min-max / length min-max
		double res_in_Meters = Formulae.distanceBetweenInMeters(bb.getMinY(),bb.getMinX(), bb.getMaxY(),bb.getMaxX()) /
				Math.hypot(mRasterDataset.getDimension().getHeight(),mRasterDataset.getDimension().getWidth());
		
		mInternalZoom = 1;
		int startZoomLevel = 0;
		while(Constants.getResolutionInMetersPerPixelForZoomLevel(startZoomLevel) > res_in_Meters){
			startZoomLevel++;
		}
		Log.d(TAG, "calculated start zoom level "+ startZoomLevel);
	
		mMinimumZoomLevel = Math.max(0, startZoomLevel - 5);
		
		mStartZoomLevel = startZoomLevel;
		
		mMaximumZoomLevel = Math.min(18, startZoomLevel + 8);

		mName = mSource;
		mDescription = "GDALLayer";

		mBoundingBox = new BoundingBox(ne, sw);
		mCenter = mBoundingBox.getCenter();

    }

    /**
     * calculates a Drawable
     */
    @Override
    public Drawable getDrawableFromTile(final MapTileDownloader downloader, final MapTile aTile, boolean hdpi) {

    	int ts = mTileSize;
    	if(aTile .getTileRect() != null){
    		ts = aTile.getTileRect().right - aTile.getTileRect().left;  	
    	}
    	final int zoom = aTile.getZ();

		final Envelope dim = mRasterDataset.getDimension();
		final int h =  (int) dim.getHeight();
		final int w =  (int) dim.getWidth();
		
		final DataType datatype = mRasterDataset.getBands().get(0).datatype();
		
    	long now = System.currentTimeMillis();
    	
    	//1. where is tile which should be displayed
    	final Point t  = Projection.tileXYToPixelXY(aTile.getX(), aTile.getY(), null);
    	final Point t2 = Projection.tileXYToPixelXY(aTile.getX() + 1, aTile.getY() + 1 , null);
    	
    	final PointF northWest = Projection.latLongToPixelXY(mBoundingBox.getLatNorth(), mBoundingBox.getLonWest(), zoom, null);
    	final PointF southEast = Projection.latLongToPixelXY(mBoundingBox.getLatSouth(), mBoundingBox.getLonEast(), zoom, null);
    	
    	//2. calculate the relative position of this point inside the bounds of this raster
    	final double xRatio = (t.x - northWest.x) / (southEast.x - northWest.x);
    	final double yRatio = (t.y - northWest.y) / (southEast.y - northWest.y);
    	final double xRatio2 = (t2.x - northWest.x) / (southEast.x - northWest.x);
    	final double yRatio2 = (t2.y - northWest.y) / (southEast.y - northWest.y);
    	
    	//3. interpolate x and y to read from
    	double readFromX =   w * xRatio;
    	double readFromY =   h * yRatio;
    	double readFromX2 =  w * xRatio2;
    	double readFromY2 =  h * yRatio2;

    	//4. TODO improve calculate the amount to read
    	int zoomedTSX = (int) (readFromX2 - readFromX);  
    	int zoomedTSY = (int) (readFromY2 - readFromY);  
    	final float scaleFactor = (float) ((zoomedTSX + zoomedTSY) / 2) / ts;

    	int readAmountX = zoomedTSX;
    	int readAmountY = zoomedTSY;
    	
    	if(zoomedTSX < 0 || zoomedTSY < 0){
    		return returnNoDataTile(downloader, aTile, ts, now);
    	}else{
    		Log.e(TAG, "wanted "+ts +" reading x : "+zoomedTSX +" y : "+ zoomedTSY);
//    		Log.i(TAG, "ts : " + ts + " zoomedTS is : " + zoomedTS +" zoom "+ zoom+ " scaleFactor : "+scaleFactor);
    		
    	}

//        Log.d(TAG, String.format("tile %d %d zoom %d read from %d %d amount %d %d",aTile.getX(), aTile.getY(),aTile.getZ(), aTile.getZ(),readFromX,readFromY,readAmountX,readAmountY));   
        
        if(readFromX < 0 || readFromX + readAmountX > w ||  readFromY < 0 || readFromY + readAmountY > h){

        	//if entirely out of bounds -> return white tile
        	if(readFromX + readAmountX <= 0 || readFromX  > w ||
        	   readFromY + readAmountY <= 0 || readFromY  > h){
        		//cannot read, create white tile
        		return returnNoDataTile(downloader, aTile, ts, now);
        	}
        	
        	//this tile is partially out of bounds, get available rectangle
        	int availableX = readAmountX, availableY = readAmountY;
        	int targetXSize = ts, targetYSize = ts;
            int coveredXOrigin = 0, coveredYOrigin = 0;

        	if(readFromX + readAmountX > w || 	readFromY + readAmountY > h){
        		//max x or y bounds hit
        		if(readFromX + readAmountX > w){        			
        			availableX = (int) (w -  readFromX);   			
        			targetXSize = (int) (availableX * (1 / scaleFactor));
        		}
        		if(readFromY + readAmountY > h){        			
        			availableY = (int) (h - readFromY);  			
        			targetYSize = (int) (availableY * (1 / scaleFactor));
        		}
        	}

        	if(readFromX < 0 || readFromY < 0){
        		//min x or y bounds hit
        		if(readFromX < 0){        			
        			availableX = (int) (readAmountX - Math.abs(readFromX));
        			coveredXOrigin = Math.round((readAmountX - availableX) /  scaleFactor);
        			targetXSize = (int) (availableX * (1 /  scaleFactor));
        			readFromX = 0;
        		}
        		if(readFromY < 0){        			
        			availableY = (int) (readAmountY - Math.abs(readFromY));
        			coveredYOrigin = Math.round((readAmountY - availableY) / scaleFactor);
        			targetYSize = (int) (availableY * (1 / scaleFactor));
        			readFromY = 0;
        		}
        	}
//        	Log.e(TAG, "reading of ("+availableX+","+availableY +") from "+readFromX+","+readFromY+" target {"+gdalTargetXSize+","+gdalTargetYSize+"}, covered: X"+coveredXOrigin+", Y "+coveredYOrigin);

        	final Envelope targetDim = useGDALAsResampler(targetXSize ,availableX) ?
        			new Envelope(0, targetXSize, 0, targetYSize) : new Envelope(0, availableX, 0, availableY);
        	
        	final int pixels[] = executeQuery(
           			new Envelope(readFromX, readFromX + availableX, readFromY, readFromY + availableY),
             		targetDim,
             		datatype,
             		!useGDALAsResampler(targetXSize ,availableX),
             		targetXSize , targetYSize);
           	
           	return createBoundsTile(downloader, aTile, pixels, coveredXOrigin,coveredYOrigin, targetXSize, targetYSize, ts, now);


        }else{ //this rectangle is fully covered by the file
        	Log.i(TAG, "reading of ("+readAmountX+","+readAmountY +") from "+readFromX+","+readFromY+" of file {"+w+","+h+"}");    	
        }  
        final Envelope targetDim = useGDALAsResampler(ts , readAmountX) ?
        		new Envelope(0, ts, 0, ts) : new Envelope(0, readAmountX, 0, readAmountY);
      
        final int pixels[] = executeQuery(
        		new Envelope(readFromX, readFromX + readAmountX, readFromY, readFromY + readAmountY),
        		targetDim,
        		datatype,
        		!useGDALAsResampler( ts , readAmountX),
        		ts , ts);

        Bitmap bitmap = Bitmap.createBitmap(ts, ts, Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, ts, 0, 0, ts, ts);
        

//        Log.d(TAG, "tile  took "+((System.currentTimeMillis() - now) / 1000.0f)+ " s");

        CacheableBitmapDrawable result = downloader.getCache().putTileBitmap(aTile, bitmap);
        if (result == null) {
        	Log.d(TAG, "error reading stream from mbtiles");
        }
        
        return result;

    }
    /**
     * executes a query against the dataset, resamples the result if necessary and returns the rendered pixels
     * @param bounds the bounds of the query
     * @param readDim the target dimension of the query
     * @param datatype the datatype of the query
     * @param resample if resampling is necessary ( or done by implicitely providing a different target dimension in the query)
     * @param targetWidth width of the target tile
     * @param targetHeight height of the target tile
     * @return array of pixels of size targetWidth * targetHeight
     */
	public int[] executeQuery(final Envelope bounds, final Envelope readDim, final DataType datatype, boolean resample, final int targetWidth, final int targetHeight){
		

		
		final RasterQuery query = new RasterQuery(
				bounds,
				mRasterDataset.getCRS(),
				mRasterDataset.getBands(), 
				readDim,
				datatype);

		Raster raster = null;
		
		synchronized(this){
			raster = mRasterDataset.read(query);
		}
		
		
		//new do rasterOp
		
//		HashMap<Key,Object> hm = new HashMap<>();
//		hm.put(RenderingHints.KEY_INTERPOLATION,
//				RenderingHints.VALUE_INTERPOLATION_BICUBIC);
//		hm.put(RenderingHints.KEY_SYMBOLIZATION,
//				RenderingHints.VALUE_AMPLITUDE_RESCALING);
//		
//		final RenderingHints hints = new RenderingHints(hm);
		
		
		//old

		if(resample){

			// first resampling, second rendering
			mResampler.resample(raster, new Envelope(0, targetWidth, 0, targetHeight), ResampleMethod.BILINEAR );
			return mRenderer.render(raster);

		}else{
			return mRenderer.render(raster);
		}
	}

	
	/**
	 * returns a tile which partially contains raster data, the rest is filled with pixels according to the nodata value
	 * @param downloader the downloader of this layer,
	 * @param aTile the tile to render
	 * @param gdalPixels the rendered pixels of the raster data
	 * @param coveredOriginX the x coord of the covered area's origin
	 * @param coveredOriginY the y coord of the covered area's origin
	 * @param coveredAreaX the covered area's width
	 * @param coveredAreaY the covered area's height
	 * @param destinationTileSize the destination tileSize
	 * @param timestamp when the creation of this tile started
	 * @return the pixels of destTileSize*destTileSize,
	 *  containing the rendered pixels filled up with nodata pixels
	 */
	public Drawable createBoundsTile(
			final MapTileDownloader downloader,
			final MapTile aTile,
			final int[] gdalPixels,
			final int coveredOriginX,
			final int coveredOriginY,
			final int coveredAreaX,
			final int coveredAreaY,
			final int destinationTileSize,
			final long timestamp){

		int[] pixels = new int[destinationTileSize * destinationTileSize];
		int gdalPixelCounter = 0;

		for (int y = 0; y < destinationTileSize; y++) {
			for (int x = 0; x < destinationTileSize; x++) {

				int pos = y * destinationTileSize + x;

				if( x  >= coveredOriginX && y >= coveredOriginY && x < coveredOriginX + coveredAreaX && y < coveredOriginY + coveredAreaY){
					//gdalpixel
					pixels[pos] = gdalPixels[gdalPixelCounter++];

				}else {
					//no data
					pixels[pos] =  NO_DATA_COLOR;
				}
			}
		}
		Bitmap bitmap = Bitmap.createBitmap(destinationTileSize, destinationTileSize, Config.ARGB_8888);
		bitmap.setPixels(pixels, 0, destinationTileSize, 0, 0, destinationTileSize, destinationTileSize);

//		Log.d(TAG, "tile  took "+((System.currentTimeMillis() - timestamp) / 1000.0f)+ " s");


		CacheableBitmapDrawable result = downloader.getCache().putTileBitmap(aTile, bitmap);
		if (result == null) {
			Log.d(TAG, "error reading stream from mbtiles");
		}
		return result;

	}

    
	/**
	 * returns a Tile filled with pixels according to the NO_DATA_COLOR
	 * as the desired coordinates were not covered by the dataset
	 * @param bitmap to modify the pixels 
	 * @param ts destination tilesize
	 * @param timestamp when the creation of this tile started
	 * @return the modified bitmap
	 */
	protected Drawable returnNoDataTile(final MapTileDownloader downloader, final MapTile aTile,final int ts, final long timestamp){
		
		Bitmap bitmap = Bitmap.createBitmap(ts, ts, Config.ARGB_8888);
		//cannot read, create white tile
		int[] pixels = new int[ts * ts];
		for (int i = 0; i < pixels.length; i++) {
			pixels[i] = NO_DATA_COLOR;
		}
		bitmap.setPixels(pixels, 0, ts, 0, 0, ts, ts);

		Log.d(TAG, "tile  took "+((System.currentTimeMillis() - timestamp) / 1000.0f)+ " s");
		CacheableBitmapDrawable result = downloader.getCache().putTileBitmap(aTile, bitmap);
		if (result == null) {
			Log.d(TAG, "error reading stream from mbtiles");
		}
		return result;
		
	}
	
	/**
	 * checks if this dataset consists of three bands red, green and blue
	 * which can be used for rendering
	 * @return if this dataset has three bands and their colors are RED GREEN and BLUE
	 */
	protected boolean checkIfHasRGBBands() {
		
		List<de.rooehler.rastertheque.core.Band> bands = mRasterDataset.getBands();
		
		return bands.size() == 3 &&
			   bands.get(0).color() == Color.RED &&
			   bands.get(1).color() == Color.GREEN &&
			   bands.get(2).color() == Color.BLUE;
	}


    @Override
    public void detach() {
    	
    }
    /**
     * checks if the resampling should be done inherently by GDAL or by the provided Resampler
     * 
     * Currently for queries where the amount to read from the dataset
     * is larger than the desired tile size GDAL is always used
     * as otherwise enourmouse amounts of memory need to be allocated which will lead
     * to OOM fastly
     * 
     * hence only if sample up operation is necessary the provided Resampler will be used
     * 
     * Sampling down is always done by GDAL
     * 
     * @param desiredTileSize the size of the tile to create
     * @param readFromDataSetSize the amount to read from the dataset
     * @return
     */
	public boolean useGDALAsResampler(int desiredTileSize, int readFromDataSetSize){
		
		return desiredTileSize <= readFromDataSetSize || mResampler instanceof GDALDataset;
	}
    
	public void close(){
		mRasterDataset.close();
				
	}
	
	public int getStartZoomLevel(){
		return mStartZoomLevel;
	}
	
	public LatLng getStartPos(){
		return mStartPos;
	}
	
	public RasterDataset getRasterDataset(){
		return mRasterDataset;
	}
}
