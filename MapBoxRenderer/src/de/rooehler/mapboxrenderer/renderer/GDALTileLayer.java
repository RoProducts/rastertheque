package de.rooehler.mapboxrenderer.renderer;

import java.io.File;
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
import com.vividsolutions.jts.geom.Dimension;
import com.vividsolutions.jts.geom.Envelope;

import de.rooehler.rastertheque.core.Band.Color;
import de.rooehler.rastertheque.core.DataType;
import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.core.RasterDataset;
import de.rooehler.rastertheque.core.RasterQuery;
import de.rooehler.rastertheque.io.gdal.GDALDataset;
import de.rooehler.rastertheque.processing.Renderer;
import de.rooehler.rastertheque.processing.Resampler;
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
	
	private boolean mUseColorMap;
	
	private int mRasterBandCount = 1;
	
	private boolean hasRGBBands;
	
//	private final int mScreenWidth;
	
	private final String mSource;
	
	private int mStartZoomLevel;
	
	private LatLng mStartPos;
	
	private int mTileSize;
	
	private static final int NO_DATA_COLOR = 0xff000000;
	
	public GDALTileLayer(final File file, final GDALDataset gdalRaster,final Resampler pResampler,final Renderer pRenderer, final boolean pUseColormap) {
		super(file.getName(), file.getAbsolutePath());

		mSource = file.getAbsolutePath();

		mRasterDataset = gdalRaster;

		mRenderer = pRenderer;
		
		mResampler = pResampler;

//		this.mScreenWidth = pScreenWidth;

		mUseColorMap = pUseColormap;

		initialize();
	}	

	/**
     * initializes this reader by setting up, intial zoom, internal scale and the boundingbox
     */
    private void initialize() {
    	
		this.mTileSize = getTileSizePixels();

		this.mRasterBandCount = this.mRasterDataset.getBands().size();

		if(mRasterBandCount == 3){
			hasRGBBands = checkIfHasRGBBands();
		}
    	
		final Envelope bb = mRasterDataset.getBoundingBox();

		final LatLng sw = new LatLng(bb.getMinY(),bb.getMinX()); 
		final LatLng ne = new LatLng(bb.getMaxY(),bb.getMaxX()); 

		//meters per pixel of this raster
		double res_in_Meters = Formulae.distanceBetweenInMeters(
				bb.getMinY(),bb.getMinX(), bb.getMaxY(),bb.getMaxX()) /
				Math.hypot(mRasterDataset.getDimension().getHeight(),mRasterDataset.getDimension().getWidth()); //dist in m / pixel of hyp
		
		mInternalZoom = 1;
		int startZoomLevel = 2;
		while(res_in_Meters < 1000){
			res_in_Meters *= 2;
			startZoomLevel++;
		}
	
		
		mMinimumZoomLevel = mStartZoomLevel = startZoomLevel;
		
		mMaximumZoomLevel = startZoomLevel + 8;

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
    	final Point t = Projection.tileXYToPixelXY(aTile.getX(), aTile.getY(), null);
    	final Point t2  =Projection.tileXYToPixelXY(aTile.getX() + 1, aTile.getY() , null);
    	
    	final PointF northWest = Projection.latLongToPixelXY(mBoundingBox.getLatNorth(), mBoundingBox.getLonWest(), zoom, null);
    	final PointF southEast = Projection.latLongToPixelXY(mBoundingBox.getLatSouth(), mBoundingBox.getLonEast(), zoom, null);
    	
    	//2. calculate the relative position of this point inside the bounds of this raster
    	final double xRatio = (t.x - northWest.x) / (southEast.x - northWest.x);
    	final double yRatio = (t.y - northWest.y) / (southEast.y - northWest.y);
    	final double xRatio2 = (t2.x - northWest.x) / (southEast.x - northWest.x);
    	
    	//3. interpolate x and y to read from
    	double readFromX =   w * xRatio;
    	double readFromY =   h * yRatio;
    	double readFromX2 =  w * xRatio2;

    	//4. TODO improve calculate the amount to read
    	int zoomedTS = (int) (readFromX2 - readFromX);// (int) (ts * scaleFactor);  
    	final float scaleFactor = (float) zoomedTS / ts;

    	int readAmountX = zoomedTS;
    	int readAmountY = zoomedTS;
    	
    	if(zoomedTS < 0){
    		return returnNoDataTile(downloader, aTile, ts, now);
    	}else{
    		Log.e(TAG, "wanted "+ts +" reading "+zoomedTS);
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
        	int gdalTargetXSize = ts, gdalTargetYSize = ts;
            int coveredXOrigin = 0, coveredYOrigin = 0;

        	if(readFromX + readAmountX > w || 	readFromY + readAmountY > h){
        		//max x or y bounds hit
        		if(readFromX + readAmountX > w){        			
        			availableX = (int) (w -  readFromX);   			
        			gdalTargetXSize = (int) (availableX * (1 / scaleFactor));
        		}
        		if(readFromY + readAmountY > h){        			
        			availableY = (int) (h - readFromY);  			
        			gdalTargetYSize = (int) (availableY * (1 / scaleFactor));
        		}
        	}

        	if(readFromX < 0 || readFromY < 0){
        		//min x or y bounds hit
        		if(readFromX < 0){        			
        			availableX = (int) (readAmountX - Math.abs(readFromX));
        			coveredXOrigin = Math.round((readAmountX - availableX) /  scaleFactor);
        			gdalTargetXSize = (int) (availableX * (1 /  scaleFactor));
        			readFromX = 0;
        		}
        		if(readFromY < 0){        			
        			availableY = (int) (readAmountY - Math.abs(readFromY));
        			coveredYOrigin = Math.round((readAmountY - availableY) / scaleFactor);
        			gdalTargetYSize = (int) (availableY * (1 / scaleFactor));
        			readFromY = 0;
        		}
        	}
//        	Log.e(TAG, "reading of ("+availableX+","+availableY +") from "+readFromX+","+readFromY+" target {"+gdalTargetXSize+","+gdalTargetYSize+"}, covered: X"+coveredXOrigin+", Y "+coveredYOrigin);

        	final Envelope targetDim = useGDALAsResampler() ? new Envelope(0, gdalTargetXSize, 0, gdalTargetYSize) : new Envelope(0, availableX, 0, availableY);
        	
           	int pixels[] = executeQuery(
           			new Envelope(readFromX, readFromX + availableX, readFromY, readFromY + availableY),
             		targetDim,
             		datatype,
             		!(useGDALAsResampler() || gdalTargetXSize < availableX),
             		gdalTargetXSize , gdalTargetYSize);
           	
           	return createBoundsTile(downloader, aTile, pixels, coveredXOrigin,coveredYOrigin, gdalTargetXSize, gdalTargetYSize, ts, now);


        }else{ //this rectangle is fully covered by the file
        	Log.i(TAG, "reading of ("+readAmountX+","+readAmountY +") from "+readFromX+","+readFromY+" of file {"+w+","+h+"}");    	
        }  
        final Envelope targetDim = useGDALAsResampler() ? new Envelope(0, ts, 0, ts) : new Envelope(0, readAmountX, 0, readAmountY);
      
        int pixels[] = executeQuery(
        		new Envelope(readFromX, readFromX + readAmountX, readFromY, readFromY + readAmountY),
        		targetDim,
        		datatype,
        		!(useGDALAsResampler() || ts < readAmountX),
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
    
	public int[] executeQuery(final Envelope envelope, final Envelope readDim, final DataType datatype, boolean resample, final int targetWidth, final int targetHeight){
		
		final RasterQuery query = new RasterQuery(
				envelope,
				mRasterDataset.getCRS(),
				mRasterDataset.getBands(), 
				readDim,
				datatype);

		Raster raster = null;
		synchronized(this){
			raster = mRasterDataset.read(query);
		}

		if(resample){
            int pixels[] = render(raster);
        	int[] resampledPixels = new int[targetWidth * targetHeight];
        	mResampler.resample(pixels, (int) readDim.getWidth(), (int) readDim.getHeight(), resampledPixels, targetWidth, targetHeight );
        	return resampledPixels;
        }else{
        	return render(raster);
        }
	}
	
	/**
	 * render the data contained in @param buffer
	 * Currently this will, depending on the data
	 * <ol>
	 *   <li>if the raster contains 3 bands R, G and B use these bands to render</li>
	 *   <li>if there is an according colormap to this raster file use this colormap to render</li>
	 *   <li>if none of the before will interpolate a gray scale image</li>
	 * </ol>  
	 *   
	 * @param buffer the data to render
	 * @param tilePixelAmount the size of the resulting pixel array
	 * @param dataType the type of the data
	 * @return an array containing the rendered pixels in top left first order
	 */
	public int[] render(final Raster raster){

		
		int[] pixels = null;
		
		if(hasRGBBands){
			pixels = mRenderer.rgbBands(raster);
		}else if(mRenderer.hasColorMap() && mUseColorMap){
			pixels = mRenderer.colormap(raster);
		}else{        	
			pixels = mRenderer.grayscale(raster);
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
	 * returns a Tile filled with white pixels as the desired coordinates are not covered by the file
	 * @param bitmap to modify the pixels 
	 * @param ts destination tilesize
	 * @param timestamp when the creation of this tile started
	 * @return the modified bitmap
	 */
	public Drawable returnNoDataTile(final MapTileDownloader downloader,	final MapTile aTile,final int ts, final long timestamp){
		
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
	
	public double scaleFactorAccordingToZoom(int zoom){
		
		int diff = zoom - mStartZoomLevel + mInternalZoom;;
		
		return Math.pow(2,  -diff);
		
	}	
	
	private boolean checkIfHasRGBBands() {
		
		List<de.rooehler.rastertheque.core.Band> bands = mRasterDataset.getBands();
		
		return bands.size() == 3 &&
			   bands.get(0).color() == Color.RED &&
			   bands.get(1).color() == Color.GREEN &&
			   bands.get(2).color() == Color.BLUE;
	}


    @Override
    public void detach() {
    	
    }
    
	public boolean useGDALAsResampler(){
		
		return mResampler instanceof GDALDataset;
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
