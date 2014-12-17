package de.rooehler.rasterapp.rasterrenderer.gdal;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import org.gdal.gdal.Band;
import org.gdal.gdalconst.gdalconstConstants;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.TileBitmap;

import android.util.Log;
import de.rooehler.rasterapp.rasterrenderer.RasterJob;
import de.rooehler.rasterapp.rasterrenderer.RasterRenderer;
import de.rooehler.rastertheque.core.Dimension;
import de.rooehler.rastertheque.core.Rectangle;
import de.rooehler.rastertheque.io.gdal.DataType;
import de.rooehler.rastertheque.io.gdal.GDALRasterIO;
import de.rooehler.rastertheque.processing.ColorMapProcessing;
/**
 * A Renderer of gdal data for Mapsforge
 * @author Robert Oehler
 *
 */
public class GDALMapsforgeRenderer implements RasterRenderer {

	private final static String TAG = GDALMapsforgeRenderer.class.getSimpleName();

	private GraphicFactory graphicFactory;

	private byte mInternalZoom = 1;
	
	private final byte NATIVE_ZOOM_RANGE = 5;
	
	private boolean isWorking = true;
	
	private GDALRasterIO mRasterIO;
	
	private ColorMapProcessing mColorMapProcessing;
	
	private boolean mUseColorMap;
	
	private int mRasterBandCount;
	
	private boolean hasRGBBands;

	public GDALMapsforgeRenderer(GraphicFactory graphicFactory, final GDALRasterIO pRaster, final ColorMapProcessing pColorMapProcessing, final boolean pUseColorMap) {
		
		this.graphicFactory = graphicFactory;
		
		this.mRasterIO = pRaster;
		
		this.mColorMapProcessing = pColorMapProcessing;
		
		this.mUseColorMap = pUseColorMap;
		
		this.mRasterBandCount = this.mRasterIO.getBands().size();
		
		if(mRasterBandCount == 3){
			hasRGBBands = checkIfHasRGBBands();
		}

	}
	
	private boolean checkIfHasRGBBands() {
		
		List<Band> bands = mRasterIO.getBands();
		
		return bands.size() == 3 &&
			   bands.get(0).GetColorInterpretation() == gdalconstConstants.GCI_RedBand &&
			   bands.get(1).GetColorInterpretation() == gdalconstConstants.GCI_GreenBand &&
			   bands.get(2).GetColorInterpretation() == gdalconstConstants.GCI_BlueBand;
	}
	/**
	 * calculates an appropriate first zoom level for this raster, i.e. :
	 * 
	 * it must provide enough tiles to show the entire map
	 * 1 -> 4 tiles
	 * 2 -> 8 tiles etc...
	 * 
	 * @param tileSize the target tileSize of this mapView
	 * @param screenWidth the current screenwidth of this device
	 * @return the start zoom level for use in the mapsforge framework
	 */
	public byte calculateStartZoomLevel(int tileSize,int screenWidth){
		
		double tilesEnter = (double) screenWidth / tileSize;

		double zoom_ = Math.log(tilesEnter) / Math.log(2);

		byte zoom = (byte) Math.max(1, Math.round(zoom_));
		
		Log.d(TAG, "calculated start zoom : " + zoom);
		
		return zoom;
	}
	/**
	 * on startup, an optimal representation for raster files needs to be calculated
	 * For large files this "zooms out" , increasing the internalZoom, until this raster fits
	 * inside the current screenwidth 
	 * For small files it "zooms" in until the rendered size fits at least the tileSize
	 * 
	 * According to this internalZoom a maximum zoom range is calculated, and the max zoom level returned
	 * 
	 * @param tileSize the tileSize of this mapView
	 * @param screenWidth  the current screenwidth of this device 
	 * @param rasterWidth the width of this raster
	 * @param rasterHeight the height of this raster
	 * @return the max zoom level for use in the mapsforge framework
	 */
	public byte calculateZoomLevelsAndStartScale(int tileSize, int screenWidth, int rasterWidth, int rasterHeight) {

		double tilesEnter = rasterWidth / tileSize;

		int nativeZoom = (int) (Math.log(tilesEnter) / Math.log(2));
		
		int offset = nativeZoom - 1;
		
		byte maxZoom = (byte) (NATIVE_ZOOM_RANGE + offset);
		
		if(rasterWidth > screenWidth){
			//if raster larger than screen
			int available = rasterWidth;
			while(available / 2 > screenWidth){
				this.mInternalZoom++;
				available /= 2.0;
			}
		}else if(rasterHeight < tileSize || rasterWidth < tileSize){
			//if raster smaller than tilesize
			int necessary = Math.min(rasterHeight, rasterWidth);
			int desired   = tileSize;
			while(desired > necessary){
				this.mInternalZoom--;
				desired /= 2;
			}
		}
		
		return maxZoom;
	}

	/**
	 * main method of this renderer :
	 * 
	 * executes a rasterJob and returns a bitmap with the rendered pixels
	 * according to the parameters of @param job 
	 * 
	 * it checks the bounds of this job to see if
	 * 1.the entire area is covered 
	 * 		read the entire area and return the tile with the rendered data
	 * 2.only a part
	 *      read the covered area and fill up the remaining area on the tile with white pixels
	 * 3.nothing
	 *      returns a bitmap containing white pixels
	 *      
	 * @param job - the rasterjob containing the properties of the area to render
	 * @return TileBitmap the rendered bitmap     
	 */
	@Override
	public TileBitmap executeJob(final RasterJob job) {
		
		final int ts = job.tile.tileSize;
		final byte zoom = job.tile.zoomLevel;
		final int h = mRasterIO.getRasterHeight();
		final int w = mRasterIO.getRasterWidth();
		
		long now = System.currentTimeMillis();

		TileBitmap bitmap = this.graphicFactory.createTileBitmap(job.displayModel.getTileSize(), job.hasAlpha);
        
        final double scaleFactor = scaleFactorAccordingToZoom(zoom);
        
        int zoomedTS = (int) (ts * scaleFactor);  

        final int readAmountX = zoomedTS;
        final int readAmountY = zoomedTS;
        
        long readFromX = job.tile.tileX * readAmountX;
        long readFromY = job.tile.tileY * readAmountY;
        
//        Log.d(TAG, String.format("tile %d %d zoom %d read from %d %d amount %d %d",job.tile.tileX,job.tile.tileY,job.tile.zoomLevel,readFromX,readFromY,readAmountX,readAmountY));   
        
        if(readFromX < 0 || readFromX + readAmountX > w ||  readFromY < 0 || readFromY + readAmountY > h){
        	Log.e(TAG, "reading of ("+readAmountX+","+readAmountY +") from "+readFromX+","+readFromY+" of file {"+w+","+h+"}");

        	//if entirely out of bounds -> return white tile
        	if(readFromX + readAmountX <= 0 || readFromX  > w ||
        	   readFromY + readAmountY <= 0 || readFromY  > h){
        		//cannot read, create white tile
        		return returnWhiteTile(bitmap, ts, now);
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
        	return createBoundsTile(bitmap, pixels, coveredXOrigin,coveredYOrigin, gdalTargetXSize, gdalTargetYSize, ts, now);

        }else{ //this rectangle is fully covered by the file
        	Log.i(TAG, "reading of ("+readAmountX+","+readAmountY +") from "+readFromX+","+readFromY+" of file {"+w+","+h+"}");    	
        }  
        
        final ByteBuffer buffer = readPixels(new Rectangle((int)readFromX,(int)readFromY, readAmountX,readAmountY), new Dimension(ts, ts),mRasterIO.getDatatype());
		bitmap.setPixels(render(buffer,ts * ts, mRasterIO.getDatatype()), ts);
		
		Log.d(TAG, "tile  took "+((System.currentTimeMillis() - now) / 1000.0f)+ " s");
		return bitmap;
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

		mRasterIO.read(src, dst, buffer);

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
	 * returns a Tile filled with white pixels as the desired coordinates are not covered by the file
	 * @param bitmap to modify the pixels 
	 * @param ts destination tilesize
	 * @param timestamp when the creation of this tile started
	 * @return the modified bitmap
	 */
	public TileBitmap returnWhiteTile(final TileBitmap bitmap, final int ts, final long timestamp){
		//cannot read, create white tile
		int[] pixels = new int[ts * ts];
		for (int i = 0; i < pixels.length; i++) {
			pixels[i] = 0xffffffff;
		}
		bitmap.setPixels( pixels, ts);
		Log.d(TAG, "tile  took "+((System.currentTimeMillis() - timestamp) / 1000.0f)+ " s");
		return bitmap;
		
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
	public TileBitmap createBoundsTile(	
			final TileBitmap bitmap,
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
					//white pixel;
					pixels[pos] =  0xffffffff;
				}

			}
		}
		bitmap.setPixels( pixels, destinationTileSize);
		Log.d(TAG, "tile  took "+((System.currentTimeMillis() - timestamp) / 1000.0f)+ " s");
		return bitmap;
	}
	
	public GDALRasterIO getRaster(){
		return mRasterIO;
	}

	@Override
	public void start() {

		this.isWorking = true;

	}
	@Override
	public void stop() {

		this.isWorking = false;

	}
	@Override
	public boolean isWorking() {

		return this.isWorking;
	}

	/**
	 * closes and destroys any resources needed
	 */
	@Override
	public void destroy() {

		stop();

	}
	public double scaleFactorAccordingToZoom(short zoom){
		
		return Math.pow(2,  -(zoom - this.mInternalZoom));
		
	}
	public void toggleUseColorMap(){
		
		this.mUseColorMap = !mUseColorMap;
		
	}

	@Override
	public String getFilePath() {
		
		return mRasterIO.getSource();
	}
	
	public boolean canSwitchColorMap(){
		
		return mRasterBandCount == 1;
	}

}
