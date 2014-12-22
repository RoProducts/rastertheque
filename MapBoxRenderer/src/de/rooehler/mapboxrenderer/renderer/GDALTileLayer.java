package de.rooehler.mapboxrenderer.renderer;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import org.gdal.gdal.Band;
import org.gdal.gdalconst.gdalconstConstants;

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

import de.rooehler.rastertheque.io.IRasterIO;
import de.rooehler.rastertheque.core.Dimension;
import de.rooehler.rastertheque.core.Rectangle;
import de.rooehler.rastertheque.io.gdal.DataType;
import de.rooehler.rastertheque.io.gdal.GDALRasterIO;
import de.rooehler.rastertheque.processing.IColorMapProcessing;
import de.rooehler.rastertheque.processing.colormap.MColorMapProcessing;
import de.rooehler.rastertheque.util.Formulae;

public class GDALTileLayer extends TileLayer {
	
	private final static String TAG = GDALTileLayer.class.getSimpleName();
	
	private byte mInternalZoom = 1;
	
	private GDALRasterIO mRasterIO;
	
	private IColorMapProcessing mColorMapProcessing;
	
	private boolean mUseColorMap;
	
	private int mRasterBandCount = 1;
	
	private boolean hasRGBBands;
	
	private final int mScreenWidth;
	
	private final String mSource;
	
	private int mStartZoomLevel;
	
	private LatLng mStartPos;
	
	private int mTileSize;

	private Projection mProj;
	
	private static final int NO_DATA_COLOR = 0xff000000;
	
	public GDALTileLayer(final File file, final GDALRasterIO gdalRaster,final MColorMapProcessing pColorMapProcessing, final int pScreenWidth, final boolean pUseColormap, final Projection pProj) {
		super(file.getName(), file.getAbsolutePath());

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

		double res_in_Meters = Formulae.distanceBetweenInMeters(bb[1],bb[0], bb[3],bb[2]) / Math.hypot(mRasterIO.getRasterHeight(),mRasterIO.getRasterWidth()); //lon pp
		
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


    @Override
    public Drawable getDrawableFromTile(final MapTileDownloader downloader, final MapTile aTile, boolean hdpi) {

    	int ts = mTileSize;
    	if(aTile .getTileRect() != null){
    		ts = aTile.getTileRect().right - aTile.getTileRect().left;  	
    	}
    	final int zoom = aTile.getZ();
    	final int h = mRasterIO.getRasterHeight();
    	final int w = mRasterIO.getRasterWidth();

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
    	int readAmountY = 0;
    	
    	final String proj_ = mRasterIO.getProjection();
//
//    	CoordinateReferenceSystem crs = null;
//		try {
//			crs = new ProjWKTParser().parse(proj_);
//		} catch (ParseException e) {
//			Log.e(TAG, "doh");
//		}
    	

//    	if(Proj.equal(crs, Proj.EPSG_900913)){
//    		Log.i(TAG, "is a 900913 dataset");
    		readAmountY = zoomedTS;
//
//    	}else{ 		
//    		Log.i(TAG, "is not a 900913 dataset");
//    		readAmountY = (int) (zoomedTS * ((float) h / w));
//    	}
    	
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

        	final ByteBuffer buffer = readPixels(new Rectangle((int)readFromX,(int)readFromY, availableX, availableY), new Dimension(gdalTargetXSize, gdalTargetYSize),mRasterIO.getDatatype());

        	int[] pixels = render(buffer, gdalTargetXSize *gdalTargetYSize, mRasterIO.getDatatype());

        	return createBoundsTile(downloader, aTile, pixels, coveredXOrigin,coveredYOrigin, gdalTargetXSize, gdalTargetYSize, ts, now);

        }else{ //this rectangle is fully covered by the file
        	Log.i(TAG, "reading of ("+readAmountX+","+readAmountY +") from "+readFromX+","+readFromY+" of file {"+w+","+h+"}");    	
        }  
        
        final ByteBuffer buffer = readPixels(new Rectangle((int)readFromX,(int)readFromY, readAmountX,readAmountY), new Dimension(ts, ts),mRasterIO.getDatatype());

        Bitmap bitmap = Bitmap.createBitmap(ts, ts, Config.ARGB_8888);
        bitmap.setPixels(render(buffer,ts * ts, mRasterIO.getDatatype()), 0, ts, 0, 0, ts, ts);


//        Log.d(TAG, "tile  took "+((System.currentTimeMillis() - now) / 1000.0f)+ " s");

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
		
		List<Band> bands = mRasterIO.getBands();
		
		return bands.size() == 3 &&
			   bands.get(0).GetColorInterpretation() == gdalconstConstants.GCI_RedBand &&
			   bands.get(1).GetColorInterpretation() == gdalconstConstants.GCI_GreenBand &&
			   bands.get(2).GetColorInterpretation() == gdalconstConstants.GCI_BlueBand;
	}


    @Override
    public void detach() {
    }
	public void close(){
		mRasterIO.close();
				
	}
	
	public int getStartZoomLevel(){
		return mStartZoomLevel;
	}
	
	public LatLng getStartPos(){
		return mStartPos;
	}
	public IRasterIO getRasterIO(){
		return mRasterIO;
	}
}
