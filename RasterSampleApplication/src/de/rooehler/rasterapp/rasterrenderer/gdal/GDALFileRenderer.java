package de.rooehler.rasterapp.rasterrenderer.gdal;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import org.gdal.gdal.Band;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.util.MercatorProjection;

import android.util.Log;
import de.rooehler.rasterapp.rasterrenderer.RasterJob;
import de.rooehler.rasterapp.rasterrenderer.RasterRenderer;
import de.rooehler.rastertheque.core.Rectangle;
import de.rooehler.rastertheque.io.gdal.DataType;
import de.rooehler.rastertheque.io.gdal.GDALRaster;
/**
 * A GDALFileRenderer
 * @author Robert Oehler
 *
 */
public class GDALFileRenderer implements RasterRenderer {

	private final static String TAG = GDALFileRenderer.class.getSimpleName();

	private GraphicFactory graphicFactory;

	private byte mInitialZoom = -1;
	
//	private boolean distortQuadratic = false;
	
	private boolean isWorking = true;
	
	private GDALRaster mRaster;

	public GDALFileRenderer(GraphicFactory graphicFactory, final GDALRaster pRaster) {
		
		this.graphicFactory = graphicFactory;
		
		this.mRaster = pRaster;

	}

	/**
	 * called from RasterFileWorkerThread : executes a mapgeneratorJob and modifies the @param bitmap which will be the
	 * result according to the parameters inside @param mapGeneratorJob
	 */
	@Override
	public TileBitmap executeJob(RasterJob job) {
		
		final int ts = job.tile.tileSize;
		final byte zoom = job.tile.zoomLevel;
		final int h = mRaster.getRasterHeight();
		final int w = mRaster.getRasterWidth();
		
		if(mInitialZoom == -1){
			
			this.mInitialZoom = mRaster.getStartZoomLevel(mRaster.getBoundingBox().centre(),ts);
			
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

		List<Band> bands = mRaster.getBands();

        //TODO render all bands
        final Band band = bands.get(0);
        
        final double scaleFactor = scaleFactorAccordingToZoom(zoom);
        
        int zoomedTS = (int) (ts * scaleFactor);
        
        //final double whr = mRaster.getWidthHeightRatio();
        
        final int readAmountX = zoomedTS;//distortQuadratic ? (int) (whr < 1.0 ? zoomedTS / whr : zoomedTS) : zoomedTS;
        final int readAmountY = zoomedTS;//distortQuadratic ? (int) (whr > 1.0 ? zoomedTS / whr : zoomedTS) : zoomedTS;
        
        long readFromX =  MercatorProjection.tileToPixel(job.tile.tileX, readAmountX);
        long readFromY =  MercatorProjection.tileToPixel(job.tile.tileY, readAmountY);
          
//        Log.d(TAG, String.format("tile %d %d zoom %d read from %d %d amount %d %d",job.tile.tileX,job.tile.tileY,job.tile.zoomLevel,readFromX,readFromY,readAmountX,readAmountY));   
        
        if(readFromX < 0 || readFromX + readAmountX > w ||  readFromY < 0 || readFromY + readAmountY > h){
        	Log.e(TAG, "reading from "+readFromX+","+readFromY+" from file {"+w+","+h+"}");

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
        	
        	final int bufferSize = gdalTargetXSize * gdalTargetYSize * mRaster.getDatatype().size();
            ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
            
            buffer.order(ByteOrder.nativeOrder()); 
            Rectangle src = new Rectangle((int)readFromX,(int)readFromY, availableX, availableY);
            band.ReadRaster_Direct((int)readFromX,(int)readFromY, availableX, availableY, gdalTargetXSize, gdalTargetYSize, DataType.toGDAL(mRaster.getDatatype()), buffer);
            try{
            	bitmap.setPixels(mRaster.extractBoundsPixels(buffer,coveredXOrigin,coveredYOrigin, gdalTargetXSize, gdalTargetYSize, ts, mRaster.getDatatype()), ts);
            }catch(ArrayIndexOutOfBoundsException e){
    			Log.e("ColorMap", "ioob",e);
    			return bitmap;
    		}
    		Log.d(TAG, "tile  took "+((System.currentTimeMillis() - now) / 1000.0f)+ " s");
    		return bitmap;


        }else{
        	Log.i(TAG, "reading from "+readFromX+","+readFromY+" from file {"+w+","+h+"}");    	
        }

        final int bufferSize = ts * ts * mRaster.getDatatype().size();
        ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
        
        buffer.order(ByteOrder.nativeOrder()); 
        Rectangle src = new Rectangle((int)readFromX,(int)readFromY, readAmountX,readAmountY);
        Rectangle dst = new Rectangle(0,0, ts,ts);
        
        mRaster.read(band,src, dst, buffer);

		bitmap.setPixels(mRaster.generatePixels(buffer,(ts * ts), mRaster.getDatatype()), ts);
		
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
	
	public void toggleColorMap(){
		this.mRaster.toogleUseColorMap();
	}
	public void applyProjection(String wkt){
		this.mRaster.applyProjection(wkt);
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

	@Override
	public String getFilePath() {
		
		return mRaster.getFilePath();
	}
}
