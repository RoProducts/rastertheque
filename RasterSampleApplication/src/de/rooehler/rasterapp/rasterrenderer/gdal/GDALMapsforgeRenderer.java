package de.rooehler.rasterapp.rasterrenderer.gdal;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.util.MercatorProjection;

import android.util.Log;
import de.rooehler.rasterapp.rasterrenderer.RasterJob;
import de.rooehler.rasterapp.rasterrenderer.RasterRenderer;
import de.rooehler.rastertheque.core.Dimension;
import de.rooehler.rastertheque.core.Rectangle;
import de.rooehler.rastertheque.interfaces.RasterProcessing;
import de.rooehler.rastertheque.io.gdal.GDALRasterIO;
/**
 * A GDALFileRenderer
 * @author Robert Oehler
 *
 */
public class GDALMapsforgeRenderer implements RasterRenderer {

	private final static String TAG = GDALMapsforgeRenderer.class.getSimpleName();

	private GraphicFactory graphicFactory;

	private byte mInitialZoom = -1;
	
//	private boolean distortQuadratic = false;
	
	private boolean isWorking = true;
	
	private GDALRasterIO mRasterIO;
	
	private RasterProcessing mRasterProcessing;
	
	private boolean mUseColorMap;

	public GDALMapsforgeRenderer(GraphicFactory graphicFactory, final GDALRasterIO pRaster, final RasterProcessing pRasterProcessing, final boolean pUseColorMap) {
		
		this.graphicFactory = graphicFactory;
		
		this.mRasterIO = pRaster;
		
		this.mRasterProcessing = pRasterProcessing;
		
		this.mUseColorMap = pUseColorMap;

	}

	/**
	 * called from RasterFileWorkerThread : executes a mapgeneratorJob and modifies the @param bitmap which will be the
	 * result according to the parameters inside @param mapGeneratorJob
	 */
	@Override
	public TileBitmap executeJob(RasterJob job) {
		
		final int ts = job.tile.tileSize;
		final byte zoom = job.tile.zoomLevel;
		final int h = mRasterIO.getRasterHeight();
		final int w = mRasterIO.getRasterWidth();
		
		if(mInitialZoom == -1){
			
			this.mInitialZoom = mRasterIO.getStartZoomLevel(mRasterIO.getBoundingBox().centre(),ts);
			
			if(h < ts || w < ts){
				int necessary = Math.min(h, w);
				int desired   = ts;
				while(desired > necessary){
					this.mInitialZoom--;
					desired /= 2;
				}
			}
		}

		long now = System.currentTimeMillis();

		TileBitmap bitmap = this.graphicFactory.createTileBitmap(job.displayModel.getTileSize(), job.hasAlpha);
        
        final double scaleFactor = scaleFactorAccordingToZoom(zoom);
        
        int zoomedTS = (int) (ts * scaleFactor);
        
        final int readAmountX = zoomedTS;
        final int readAmountY = zoomedTS;
        
        long readFromX =  MercatorProjection.tileToPixel(job.tile.tileX, readAmountX);
        long readFromY =  MercatorProjection.tileToPixel(job.tile.tileY, readAmountY);
          
//        Log.d(TAG, String.format("tile %d %d zoom %d read from %d %d amount %d %d",job.tile.tileX,job.tile.tileY,job.tile.zoomLevel,readFromX,readFromY,readAmountX,readAmountY));   
        
        if(readFromX < 0 || readFromX + readAmountX > w ||  readFromY < 0 || readFromY + readAmountY > h){
        	Log.e(TAG, "reading from "+readFromX+","+readFromY+" of file {"+w+","+h+"}");

        	//if entire desired area out of bounds return white tile
        	if(readFromX + readAmountX <= 0 || readFromX  > w ||
        	   readFromY + readAmountY <= 0 || readFromY  > h){
        		//cannot read, create white tile
        		return returnWhiteTile(bitmap, ts, now);
        	}
        	
        	//out out bounds, needs special treatment
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

            mRasterIO.read(new Rectangle((int)readFromX,(int)readFromY, availableX, availableY), new Dimension(gdalTargetXSize, gdalTargetYSize), buffer);

            try{
            	
            	int[] pixels = null;
                if(!mUseColorMap){
                	pixels  = mRasterProcessing.generateGrayScalePixelsCalculatingMinMax(buffer, (gdalTargetXSize * gdalTargetYSize), mRasterIO.getDatatype());
                }else{
                	pixels  = mRasterProcessing.generatePixelsWithColorMap(buffer, (gdalTargetXSize * gdalTargetYSize), mRasterIO.getDatatype());
                }
                
                return createBoundsTile(bitmap, pixels, coveredXOrigin,coveredYOrigin, gdalTargetXSize, gdalTargetYSize, ts, now);
            	
            }catch(ArrayIndexOutOfBoundsException e){
    			Log.e("ColorMap", "ioob",e);
    			return bitmap;
    		}

        }else{
        	Log.i(TAG, "reading from "+readFromX+","+readFromY+" of file {"+w+","+h+"}");    	
        }

        final int bufferSize = ts * ts * mRasterIO.getDatatype().size();
        ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
        
        buffer.order(ByteOrder.nativeOrder()); 
 
        mRasterIO.read(new Rectangle((int)readFromX,(int)readFromY, readAmountX,readAmountY), new Dimension(ts, ts), buffer);

        int[] pixels = null;
        if(!mUseColorMap){
        	pixels  = mRasterProcessing.generateGrayScalePixelsCalculatingMinMax(buffer, (ts * ts), mRasterIO.getDatatype());
        }else{
        	pixels  = mRasterProcessing.generatePixelsWithColorMap(buffer, (ts * ts), mRasterIO.getDatatype());
        }
        
		bitmap.setPixels(pixels, ts);
		
		Log.d(TAG, "tile  took "+((System.currentTimeMillis() - now) / 1000.0f)+ " s");
		return bitmap;
	}

	public TileBitmap returnWhiteTile(final TileBitmap bitmap, final int ts, final long now){
		//cannot read, create white tile
		int[] pixels = new int[ts * ts];
		for (int i = 0; i < pixels.length; i++) {
			pixels[i] = 0xffffffff;
		}
		bitmap.setPixels( pixels, ts);
		Log.d(TAG, "tile  took "+((System.currentTimeMillis() - now) / 1000.0f)+ " s");
		return bitmap;
		
	}
	
	public TileBitmap createBoundsTile(	
			final TileBitmap bitmap,
			final int[] gdalPixels,
			final int coveredOriginX,
			final int coveredOriginY,
			final int coveredAreaX,
			final int coveredAreaY,
			final int ts,
			final long now){

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
		Log.d(TAG, "tile  took "+((System.currentTimeMillis() - now) / 1000.0f)+ " s");
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
		
		return Math.pow(2,  -(zoom - this.mInitialZoom));
		
	}
	public void toggleUseColorMap(){
		
		this.mUseColorMap = !mUseColorMap;
		
	}

	@Override
	public String getFilePath() {
		
		return mRasterIO.getFilePath();
	}
}
