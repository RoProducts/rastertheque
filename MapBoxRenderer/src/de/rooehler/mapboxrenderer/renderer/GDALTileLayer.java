package de.rooehler.mapboxrenderer.renderer;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import org.gdal.gdal.Band;
import org.gdal.gdalconst.gdalconstConstants;

import uk.co.senab.bitmapcache.CacheableBitmapDrawable;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.mapbox.mapboxsdk.constants.MapboxConstants;
import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.tileprovider.MapTile;
import com.mapbox.mapboxsdk.tileprovider.modules.MapTileDownloader;
import com.mapbox.mapboxsdk.tileprovider.tilesource.TileLayer;
import com.mapbox.mapboxsdk.views.util.Projection;
import com.mapbox.mapboxsdk.views.util.constants.MapViewConstants;

import de.rooehler.rastertheque.core.Dimension;
import de.rooehler.rastertheque.core.Rectangle;
import de.rooehler.rastertheque.io.gdal.DataType;
import de.rooehler.rastertheque.io.gdal.GDALRasterIO;
import de.rooehler.rastertheque.processing.ColorMapProcessing;
import de.rooehler.rastertheque.processing.colormap.MColorMapProcessing;

public class GDALTileLayer extends TileLayer implements MapViewConstants, MapboxConstants{
	
	private final static String TAG = GDALTileLayer.class.getSimpleName();
	
	private byte mInternalZoom = 1;
	
	private final byte NATIVE_ZOOM_RANGE = 5;
	
	private boolean isWorking = true;
	
	private GDALRasterIO mRasterIO;
	
	private ColorMapProcessing mColorMapProcessing;
	
	private boolean mUseColorMap;
	
	private int mRasterBandCount = 1;
	
	private boolean hasRGBBands;
	
	private final int mScreenWidth;
	
	private final String mSource;
	
	private Context mContext;
	
	private int mStartZoomLevel;
	
	private LatLng mStartPos;
	
	private int mTileSize;

	private Projection mProj;
	
	private static final int NO_DATA_COLOR = 0xff000000;
	
	public GDALTileLayer(final Context context,final File file, final GDALRasterIO gdalRaster,final MColorMapProcessing pColorMapProcessing, final int pScreenWidth, final boolean pUseColormap, final Projection pProj) {
		super(file.getName(), file.getAbsolutePath());

		mContext = context;

		mSource = file.getAbsolutePath();

		mRasterIO = gdalRaster;

		mColorMapProcessing = pColorMapProcessing;
		
		this.mProj = pProj;

		this.mScreenWidth = pScreenWidth;

		mUseColorMap = pUseColormap;

		initialize();
	}
	
	

	/**
     * Reads and opens a MBTiles file and loads its tiles into this layer.
     * @param file
     */
    private void initialize() {
    	
		this.mTileSize = getTileSizePixels();

		this.mRasterBandCount = this.mRasterIO.getBands().size();

		if(mRasterBandCount == 3){
			hasRGBBands = checkIfHasRGBBands();
		}
    	
		double[] bb = mRasterIO.getBoundingBox();

		LatLng sw = new LatLng(bb[1],bb[0]); 
		LatLng ne = new LatLng(bb[3],bb[2]); 

//		mStartZoomLevel = 12;
		//TODO calculate
		mInternalZoom = 2;
		
		mMaximumZoomLevel = 20;//zoomLevelMax;
		//TODO calculate
		mMinimumZoomLevel = mStartZoomLevel = 8;

		mName = mSource;
		mDescription = "GDALLayer";

		mBoundingBox = new BoundingBox(ne, sw);
		mCenter = mBoundingBox.getCenter();

    }


    @Override
    public void detach() {
        
    	mRasterIO.close();
    }

    @Override
    public Drawable getDrawableFromTile(final MapTileDownloader downloader, final MapTile aTile, boolean hdpi) {

    	final int ts = aTile.getTileRect().right - aTile.getTileRect().left;  	
    	final int zoom = aTile.getZ();
    	final int h = mRasterIO.getRasterHeight();
    	final int w = mRasterIO.getRasterWidth();

    	long now = System.currentTimeMillis();

    	final double scaleFactor = scaleFactorAccordingToZoom(zoom);   

    	Point t = Projection.tileXYToPixelXY(aTile.getX(), aTile.getY(), null);
    	Point t2 = Projection.tileXYToPixelXY(aTile.getX() + 1, aTile.getY() + 1, null);

    	LatLng desiredPoint = Projection.pixelXYToLatLong(t.x, t.y, zoom);
    	LatLng oneOff = Projection.pixelXYToLatLong(t2.x, t2.y, zoom);

    	final double xRatio = (desiredPoint.getLongitude() - mBoundingBox.getLonWest()) / (mBoundingBox.getLonEast() - mBoundingBox.getLonWest());
    	final double yRatio = (desiredPoint.getLatitude() - mBoundingBox.getLatNorth()) / (mBoundingBox.getLatSouth() - mBoundingBox.getLatNorth());
    	final double xRatio2 = (oneOff.getLongitude() - mBoundingBox.getLonWest()) / (mBoundingBox.getLonEast() - mBoundingBox.getLonWest());

    	long readFromX = (long) (w * xRatio);
    	long readFromY = (long) (h * yRatio);
    	long readFromX2 = (long) (w * xRatio2);

    	int zoomedTS = (int) (readFromX2 - readFromX);// (int) (ts * scaleFactor);  

    	final int readAmountX = zoomedTS;
    	final int readAmountY = zoomedTS;

        Log.d(TAG, String.format("tile %d %d zoom %d read from %d %d amount %d %d",aTile.getX(), aTile.getY(),aTile.getZ(), aTile.getZ(),readFromX,readFromY,readAmountX,readAmountY));   
        
        if(readFromX < 0 || readFromX + readAmountX > w ||  readFromY < 0 || readFromY + readAmountY > h){
        	Log.e(TAG, "reading of ("+readAmountX+","+readAmountY +") from "+readFromX+","+readFromY+" of file {"+w+","+h+"}");

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
        			coveredXOrigin = (int) (ts - (availableX * scaleFactor));
        			gdalTargetXSize = (int) (availableX * (1 /  scaleFactor));
        			readFromX = 0;
        		}
        		if(readFromY < 0){        			
        			availableY = (int) (readAmountY - Math.abs(readFromY));
        			coveredYOrigin = (int) (ts - (availableY * scaleFactor));
        			gdalTargetYSize = (int) (availableY * (1 / scaleFactor));
        			readFromY = 0;
        		}
        	}

        	final ByteBuffer buffer = readPixels(new Rectangle((int)readFromX,(int)readFromY, availableX, availableY), new Dimension(gdalTargetXSize, gdalTargetYSize),mRasterIO.getDatatype());

        	int[] pixels = render(buffer, gdalTargetXSize *gdalTargetYSize, mRasterIO.getDatatype());

        	return createBoundsTile(downloader, aTile, pixels, coveredXOrigin,coveredYOrigin, gdalTargetXSize, gdalTargetYSize, ts, now);

        }else{ //this rectangle is fully covered by the file
        	Log.i(TAG, "reading of ("+readAmountX+","+readAmountY +") from "+readFromX+","+readFromY+" of file {"+w+","+h+"}");    	
        }  
        
        final ByteBuffer buffer = readPixels(new Rectangle((int)readFromX,(int)readFromY, readAmountX,readAmountY), new Dimension(ts, ts),mRasterIO.getDatatype());

        Bitmap bitmap = Bitmap.createBitmap(ts, ts, Config.ARGB_8888);
        bitmap.setPixels(render(buffer,ts * ts, mRasterIO.getDatatype()), 0, ts, 0, 0, ts, ts);


        Log.d(TAG, "tile  took "+((System.currentTimeMillis() - now) / 1000.0f)+ " s");

        CacheableBitmapDrawable result = downloader.getCache().putTileBitmap(aTile, bitmap);
        if (result == null) {
        	Log.d(TAG, "error reading stream from mbtiles");
        }
        
        return result;
    	

    }
    
	/**
	 * read a region @param src from this raster
	 * and scale the resulting area to the dimension @param dst
	 * according to the datatype @param datatype
	 * @param src the region to read
	 * @param dst the desired destination dimension
	 * @return a ByteBuffer containing the read data 
	 */
	public ByteBuffer readPixels(final Rectangle src, final Dimension dst,final DataType dataType){
		
		final int bufferSize = dst.getSize() * mRasterIO.getDatatype().size() * mRasterBandCount;
		ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
		buffer.order(ByteOrder.nativeOrder()); 
		synchronized(this){			
			mRasterIO.read(src, dst, buffer);
		}

		return buffer;
	}
	

	public int[] render(final ByteBuffer buffer, final int tilePixelAmount, final DataType dataType){

		
		int[] pixels = null;
		
		if(hasRGBBands){
			pixels = mColorMapProcessing.generateThreeBandedRGBPixels(buffer, tilePixelAmount , mRasterIO.getDatatype());
		}else if(mColorMapProcessing.hasColorMap() && mUseColorMap){
			pixels = mColorMapProcessing.generatePixelsWithColorMap(buffer, tilePixelAmount, mRasterIO.getDatatype());
		}else{        	
			pixels = mColorMapProcessing.generateGrayScalePixelsCalculatingMinMax(buffer, tilePixelAmount, mRasterIO.getDatatype());
		} 

		
		return pixels;
	}
	
	/**
	 * returns a tile which partially contains raster data, the rest is filled with white pixels
	 * @param bitmap to modify the pixels
	 * @param gdalPixels the pixels with the raster data
	 * @param coveredOriginX the x coord of the covered area's origin
	 * @param coveredOriginY the y coord of the covered area's origin
	 * @param coveredAreaX the covered area's width
	 * @param coveredAreaY the covered area's height
	 * @param destinationTileSize the destination tileSize
	 * @param timestamp when the creation of this tile started
	 * @return the modified bitmap
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

		Log.d(TAG, "tile  took "+((System.currentTimeMillis() - timestamp) / 1000.0f)+ " s");


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
		
		List<Band> bands = mRasterIO.getBands();
		
		return bands.size() == 3 &&
			   bands.get(0).GetColorInterpretation() == gdalconstConstants.GCI_RedBand &&
			   bands.get(1).GetColorInterpretation() == gdalconstConstants.GCI_GreenBand &&
			   bands.get(2).GetColorInterpretation() == gdalconstConstants.GCI_BlueBand;
	}
	
	
	public int getStartZoomLevel(){
		return mStartZoomLevel;
	}
	
	public LatLng getStartPos(){
		return mStartPos;
	}
}
