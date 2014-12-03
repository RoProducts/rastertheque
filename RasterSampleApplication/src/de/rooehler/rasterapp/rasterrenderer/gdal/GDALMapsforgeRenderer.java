package de.rooehler.rasterapp.rasterrenderer.gdal;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.TileBitmap;

import android.util.Log;
import de.rooehler.rasterapp.rasterrenderer.RasterJob;
import de.rooehler.rasterapp.rasterrenderer.RasterRenderer;
import de.rooehler.rastertheque.core.Dimension;
import de.rooehler.rastertheque.core.Rectangle;
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
	
//	private float mScaleFactor = 1.0f;
	
	private boolean isWorking = true;
	
	private GDALRasterIO mRasterIO;
	
	private ColorMapProcessing mColorMapProcessing;
	
	private boolean mUseColorMap;

	public GDALMapsforgeRenderer(GraphicFactory graphicFactory, final GDALRasterIO pRaster, final ColorMapProcessing pColorMapProcessing, final boolean pUseColorMap) {
		
		this.graphicFactory = graphicFactory;
		
		this.mRasterIO = pRaster;
		
		this.mColorMapProcessing = pColorMapProcessing;
		
		this.mUseColorMap = pUseColorMap;

	}
	
	public byte calculateZoomLevelsAndStartScale(int tileSize, int screenWidth, int rasterWidth,	int rasterHeight) {
		

		double tilesEnter = rasterWidth / tileSize;

		int nativeZoom = (int) (Math.log(tilesEnter) / Math.log(2));
		
		int offset = nativeZoom - 1;
		
		byte maxZoom = (byte) (5 + offset);
		
		if(rasterWidth > screenWidth){
			//if raster larger than screen
			int available = rasterWidth;
			while(available > screenWidth){
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
	 * called from RasterWorkerThread : executes a rasterJob and returns a bitmap which will be the
	 * result according to the parameters inside the job or null 
	 */
	@Override
	public TileBitmap executeJob(RasterJob job) {
		
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
        	Log.e(TAG, "reading from "+readFromX+","+readFromY+" of file {"+w+","+h+"}");

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
        	
        	final int bufferSize = gdalTargetXSize * gdalTargetYSize * mRasterIO.getDatatype().size();
            ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
            buffer.order(ByteOrder.nativeOrder()); 
            //read the available pixels
            

            mRasterIO.read(new Rectangle((int)readFromX,(int)readFromY, availableX, availableY), new Dimension(gdalTargetXSize, gdalTargetYSize), buffer);

            try{
            	
            	int[] pixels = null;
                if(mColorMapProcessing.hasColorMap() && mUseColorMap){
                	pixels  = mColorMapProcessing.generatePixelsWithColorMap(buffer, (gdalTargetXSize * gdalTargetYSize), mRasterIO.getDatatype());
                } else {
                	pixels  = mColorMapProcessing.generateGrayScalePixelsCalculatingMinMax(buffer, (gdalTargetXSize * gdalTargetYSize), mRasterIO.getDatatype());
                }
                return createBoundsTile(bitmap, pixels, coveredXOrigin,coveredYOrigin, gdalTargetXSize, gdalTargetYSize, ts, now);
            	
            }catch(ArrayIndexOutOfBoundsException e){
    			Log.e(TAG, "error creating the out of bounds tile",e);
    			return bitmap;
    		}

        }else{ //this rectangle is fully covered by the file
        	Log.i(TAG, "reading from "+readFromX+","+readFromY+" of file {"+w+","+h+"}");    	
        }

        final int bufferSize = ts * ts * mRasterIO.getDatatype().size();
        ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
        
        buffer.order(ByteOrder.nativeOrder()); 
 
        mRasterIO.read(new Rectangle((int)readFromX,(int)readFromY, readAmountX,readAmountY), new Dimension(ts, ts), buffer);

        int[] pixels = null;
         if(mColorMapProcessing.hasColorMap() && mUseColorMap){
        	pixels  = mColorMapProcessing.generatePixelsWithColorMap(buffer, (ts * ts), mRasterIO.getDatatype());
        }else{        	
        	pixels  = mColorMapProcessing.generateGrayScalePixelsCalculatingMinMax(buffer, (ts * ts), mRasterIO.getDatatype());
        }         
        
		bitmap.setPixels(pixels, ts);
		
		Log.d(TAG, "tile  took "+((System.currentTimeMillis() - now) / 1000.0f)+ " s");
		return bitmap;
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
	 * @param ts the destination tileSize
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
			final int ts,
			final long timestamp){

		int[] pixels = new int[ts * ts];
		int gdalPixelCounter = 0;

		for (int y = 0; y < ts; y++) {
			for (int x = 0; x < ts; x++) {

				int pos = y * ts + x;

				if( x  >= coveredOriginX && y >= coveredOriginY && x < coveredOriginX + coveredAreaX && y < coveredOriginY + coveredAreaY){
					//gdalpixel
					pixels[pos] = gdalPixels[gdalPixelCounter++];

				}else {
					//white pixel;
					pixels[pos] =  0xffffffff;
				}

			}
		}
		bitmap.setPixels( pixels, ts);
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


}
