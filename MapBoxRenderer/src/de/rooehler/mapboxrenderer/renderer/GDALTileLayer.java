package de.rooehler.mapboxrenderer.renderer;

import java.io.File;
import java.io.Serializable;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.List;

import uk.co.senab.bitmapcache.CacheableBitmapDrawable;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
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
import de.rooehler.rastertheque.core.util.ByteBufferReader;
import de.rooehler.rastertheque.core.util.ByteBufferReaderUtil;
import de.rooehler.rastertheque.io.gdal.GDALDataset;
import de.rooehler.rastertheque.io.gdal.GDALRasterQuery;
import de.rooehler.rastertheque.processing.RasterOps;
import de.rooehler.rastertheque.processing.rendering.MColorMap;
import de.rooehler.rastertheque.processing.resampling.Resampler;
import de.rooehler.rastertheque.proj.Proj;
import de.rooehler.rastertheque.util.Formulae;
import de.rooehler.rastertheque.util.Hints;
import de.rooehler.rastertheque.util.Hints.Key;
/**
 * A GDALTileLayer extends the Mapbox TileLayer to extract Tiles out of the
 * GDAL raster data
 * 
 * @author Robert Oehler
 *
 */
public class GDALTileLayer extends TileLayer {
	
	private final static String TAG = GDALTileLayer.class.getSimpleName();

	private GDALDataset mRasterDataset;
	
	private int mStartZoomLevel;
	
	private LatLng mStartPos;
	
	private int mTileSize;
	
	private static final int NO_DATA_COLOR = 0xff000000;
	
	private static long mStart = -1;
	private static long mTileCount = 0;
	
	/**
	 * A TileLayer which provides Tiles of a corresponding Raster file
	 * 
	 * To change the number of executing threads look for NUMBER_OF_TILE_DOWNLOAD_THREADS in com.mapbox.mapboxsdk.tileprovider.constants
	 * 
	 * @param file the raster file (explicitly needed to conform to the super class TileLayer)
	 * @param dataset the dataset of this file
	 * @param pResampler the resampler used by this TileLayer
	 * @param pRenderer the renderer used by this TileLayer
	 */
	public GDALTileLayer(final File file, final GDALDataset dataset) {
		super(file.getName(), file.getAbsolutePath());

		mRasterDataset = dataset;

		initialize();
	}	

	/**
     * initializes this layer by setting up the initial zoom, internal scale and the boundingbox
     */
    private void initialize() {
    	
		this.mTileSize = getTileSizePixels();
    	
		final Envelope bb = GDALDataset.convertToLatLon(mRasterDataset.getBoundingBox(),Proj.proj2wkt(mRasterDataset.getCRS().getParameterString()));

		final LatLng sw = new LatLng(bb.getMinY(),bb.getMinX()); 
		final LatLng ne = new LatLng(bb.getMaxY(),bb.getMaxX()); 
		
		final Rect dim = mRasterDataset.getDimension();
		final int width  = dim.width();
		final int height  = dim.height();

		//meters per pixel of this raster --> distance min-max / length min-max
		double res_in_Meters = Formulae.distanceBetweenInMeters(bb.getMinY(),bb.getMinX(), bb.getMaxY(),bb.getMaxX()) /
				Math.hypot(height,width);
		
		int startZoomLevel = 0;
		while(Formulae.getResolutionInMetersPerPixelForZoomLevel(startZoomLevel) > res_in_Meters){
			startZoomLevel++;
		}
		Log.d(TAG, "calculated start zoom level "+ startZoomLevel);
	
		mMinimumZoomLevel = Math.max(0, startZoomLevel - 5);
		
		mStartZoomLevel = startZoomLevel;
		
		mMaximumZoomLevel = Math.min(18, startZoomLevel + 8);

		mName = mRasterDataset.getSource();
		mDescription = "GDALLayer";

		mBoundingBox = new BoundingBox(ne, sw);
		mCenter = mBoundingBox.getCenter();

		mStart = System.currentTimeMillis();
    }

    /**
     * calculates a Raster Drawable using the GDAL dataset
     */
    @Override
    public Drawable getDrawableFromTile(final MapTileDownloader downloader, final MapTile aTile, boolean hdpi) {

    	if(mRasterDataset == null || mRasterDataset.getBands().size() == 0){
    		return null;
    	}
    	
    	int ts = mTileSize;
    	if(aTile.getTileRect() != null){
    		ts = aTile.getTileRect().width();  	
    	}
    	final int zoom = aTile.getZ();
    	
    	final Rect dim = mRasterDataset.getDimension();
		final int w  = dim.width();
		final int h = dim.height();
				
		final DataType datatype = mRasterDataset.getBands().get(0).datatype();
				
    	long now = System.currentTimeMillis();
    	
    	//1. where is tile which should be displayed
    	final Point t  = Projection.tileXYToPixelXY(aTile.getX(), aTile.getY(), null);
    	final Point t2 = Projection.tileXYToPixelXY(aTile.getX() + 1, aTile.getY() + 1 , null);
    	
    	final LatLng uL = Projection.pixelXYToLatLong(t.x, t.y, aTile.getZ());
    	final LatLng lR = Projection.pixelXYToLatLong(t2.x, t2.y, aTile.getZ());
    	
    	final Envelope bounds = new Envelope(uL.getLongitude(),lR.getLongitude(),lR.getLatitude(),uL.getLatitude());
    			
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

    	//4. calculate the amount to read
    	int readAmountX = (int) Math.round(readFromX2 - readFromX);  
    	int readAmountY = (int) Math.round(readFromY2 - readFromY);  
    	
    	if(readAmountX < 0 || readAmountY < 0){
    		return returnNoDataTile(downloader, aTile, ts, now);
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
            final float scaleFactorX = (float) readAmountX  / ts;
            final float scaleFactorY = (float) readAmountY  / ts;

        	if(readFromX + readAmountX > w || 	readFromY + readAmountY > h){
        		//max x or y bounds hit
        		if(readFromX + readAmountX > w){        			
        			availableX = (int) (w -  readFromX);   			
        			targetXSize = (int) (availableX * (1 / scaleFactorX));
        		}
        		if(readFromY + readAmountY > h){        			
        			availableY = (int) (h - readFromY);  			
        			targetYSize = (int) (availableY * (1 / scaleFactorY));
        		}
        	}

        	if(readFromX < 0 || readFromY < 0){
        		//min x or y bounds hit
        		if(readFromX < 0){        			
        			availableX = (int) (readAmountX - Math.abs(readFromX));
        			coveredXOrigin = Math.round((readAmountX - availableX) /  scaleFactorX);
        			targetXSize = (int) (availableX * (1 /  scaleFactorX));
        			readFromX = 0;
        		}
        		if(readFromY < 0){        			
        			availableY = (int) (readAmountY - Math.abs(readFromY));
        			coveredYOrigin = Math.round((readAmountY - availableY) / scaleFactorY);
        			targetYSize = (int) (availableY * (1 / scaleFactorY));
        			readFromY = 0;
        		}
        	}
//        	Log.e(TAG, "reading of ("+availableX+","+availableY +") from "+readFromX+","+readFromY+" target {"+gdalTargetXSize+","+gdalTargetYSize+"}, covered: X"+coveredXOrigin+", Y "+coveredYOrigin);

        	final Rect targetDim = useGDALAsResampler(targetXSize ,availableX) ?
        			new Rect(0, 0, targetXSize, targetYSize) : new Rect(0, 0, availableX, availableY);
        	
        	final int pixels[] = executeQuery(
        			bounds,
           			new Rect((int)readFromX,(int) readFromY,(int) readFromX + availableX,(int) readFromY + availableY),
             		targetDim,
             		datatype,
             		!useGDALAsResampler(targetXSize ,availableX),
             		targetXSize / (double) availableX, targetYSize / (double) availableY);
           	
           	return createBoundsTile(downloader, aTile, pixels, coveredXOrigin,coveredYOrigin, targetXSize, targetYSize, ts, now);


        }else{ //this rectangle is fully covered by the file
//        	Log.i(TAG, "reading of ("+readAmountX+","+readAmountY +") from "+readFromX+","+readFromY+" of file {"+w+","+h+"}");    	
        }  
        final Rect targetDim = useGDALAsResampler(ts , readAmountX) ?
        		new Rect(0,0, ts, ts) : new Rect(0, 0, readAmountX, readAmountY);
      
        final int pixels[] = executeQuery(
        		bounds,
        		new Rect((int)readFromX,(int) readFromY,(int) readFromX + readAmountX,(int) readFromY + readAmountY),
        		targetDim,
        		datatype,
        		!useGDALAsResampler( ts , readAmountX),
        		ts / (double) readAmountX , ts / (double) readAmountY);

        Bitmap bitmap = Bitmap.createBitmap(ts, ts, Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, ts, 0, 0, ts, ts);
        
        mTileCount++;
        Log.d(TAG, "tile done "+ (System.currentTimeMillis() - mStart ) / 1000f + " since start for "+mTileCount+ " tiles");
        
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
	public int[] executeQuery(final Envelope bounds, final Rect readDim, final Rect targetDim, final DataType datatype, boolean resample, final double scaleX, final double scaleY){
		
		final RasterQuery query = new GDALRasterQuery(
				bounds,
				mRasterDataset.getCRS(),
				mRasterDataset.getBands(), 
				readDim,
				datatype,
				targetDim);

		Raster raster = null;
		
		synchronized(this){
			raster = mRasterDataset.read(query);
		}

		if(resample){
				
			HashMap<Key,Serializable> resampleParams = new HashMap<>();

			resampleParams.put(Resampler.KEY_SIZE, new Double[]{scaleX, scaleY});

			RasterOps.execute(raster, RasterOps.RESIZE, resampleParams, null, null);

		}
		
		if(checkIfHasRGBBands()){
			//no rendering necessary
			return renderRGB(raster);
		}else{

			HashMap<Key,Serializable> renderParams = new HashMap<>();

			renderParams.put(Hints.KEY_COLORMAP, new MColorMap());	
//			renderParams.put(Hints.KEY_AMPLITUDE_RESCALING, new OpenCVAmplitudeRescaler());	

			RasterOps.execute(raster, RasterOps.COLORMAP, renderParams, null, null);
//			RasterOps.execute(raster, RasterOps.AMPLITUDE_RESCALING, renderParams, null, null);

			final int width  = raster.getDimension().width();
    		final int height = raster.getDimension().height();

        	final int[] pixels  = new int[width * height];

			raster.getData().asIntBuffer().get(pixels);

			return pixels;

		}
	}

	
	private int[] renderRGB(final Raster raster) {
		
		final ByteBufferReader reader = new ByteBufferReader(raster.getData().array(), ByteOrder.nativeOrder());
		
		final int width  = raster.getDimension().width();
		final int height = raster.getDimension().height();
		final int pixelAmount = width * height;
		
		int [] pixels = new int[pixelAmount];
		
		double[] pixelsR = new double[pixelAmount];
		double[] pixelsG = new double[pixelAmount];
		double[] pixelsB = new double[pixelAmount];
           
		for (int i = 0; i < pixelAmount; i++) {	
			pixelsR[i] =  ByteBufferReaderUtil.getValue(reader, raster.getBands().get(0).datatype());
		}
		for (int j = 0; j < pixelAmount; j++) {	
			pixelsG[j] =  ByteBufferReaderUtil.getValue(reader, raster.getBands().get(1).datatype());
		}
		for (int k = 0; k < pixelAmount; k++) {	
			pixelsB[k] =  ByteBufferReaderUtil.getValue(reader, raster.getBands().get(2).datatype());
		}
		
        for (int l = 0; l < pixelAmount; l++) {	
        	
        	double r = pixelsR[l];
        	double g = pixelsG[l];
        	double b = pixelsB[l];
        	
        	pixels[l] = 0xff000000 | ((((int) r) << 16) & 0xff0000) | ((((int) g) << 8) & 0xff00) | ((int) b);
        }
        
		return pixels;
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
    	
    	if(mRasterDataset != null){    		
    		mRasterDataset.close();
    		mRasterDataset = null;
    	}
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
		
		return desiredTileSize <= readFromDataSetSize;
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
