package de.rooehler.mapsforgerenderer.rasterrenderer.gdal;


import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.osgeo.proj4j.CoordinateReferenceSystem;

import android.graphics.Bitmap;
import android.util.Log;

import com.vividsolutions.jts.geom.Envelope;

import de.rooehler.mapsforgerenderer.rasterrenderer.RasterJob;
import de.rooehler.mapsforgerenderer.rasterrenderer.RasterRenderer;
import de.rooehler.rastertheque.core.Band.Color;
import de.rooehler.rastertheque.core.DataType;
import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.core.RasterQuery;
import de.rooehler.rastertheque.io.gdal.GDALDataset;
import de.rooehler.rastertheque.processing.RenderingHints.Key;
import de.rooehler.rastertheque.processing.ops.RenderOp;
import de.rooehler.rastertheque.processing.ops.ResampleOp;
import de.rooehler.rastertheque.processing.rendering.OpenCVRenderer;
/**
 * A Renderer of gdal data for Mapsforge
 * @author Robert Oehler
 *
 */
public class GDALMapsforgeRenderer implements RasterRenderer {

	private final static String TAG = GDALMapsforgeRenderer.class.getSimpleName();
	
	private static final int NO_DATA_COLOR = 0xffffffff;

	private GraphicFactory graphicFactory;

	private byte mInternalZoom = 1;
	
	private final byte NATIVE_ZOOM_RANGE = 5;
	
	private boolean isWorking = true;
	
	private GDALDataset mRasterDataset;
	


	public GDALMapsforgeRenderer(GraphicFactory graphicFactory, final GDALDataset pRaster) {
		
		this.graphicFactory = graphicFactory;
		
		this.mRasterDataset = pRaster;
		
	}
	/**
	 * checks if this dataset consists of three bands red, green and blue
	 * which can be used for rendering
	 * @return if this dataset has three bands and their colors are RED GREEN and BLUE
	 */
	private boolean checkIfHasRGBBands() {
		
		List<de.rooehler.rastertheque.core.Band> bands = mRasterDataset.getBands();
		
		return bands.size() == 3 &&
			   bands.get(0).color() == Color.RED &&
			   bands.get(1).color() == Color.GREEN &&
			   bands.get(2).color() == Color.BLUE;
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
		final Envelope dim = mRasterDataset.getDimension();
		final int h = (int) dim.getHeight();
		final int w = (int) dim.getWidth();
		final DataType datatype = mRasterDataset.getBands().get(0).datatype();
		
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
        		return returnNoDataTile(bitmap, ts, now);
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

        	final Envelope targetDim = useGDALAsResampler(gdalTargetXSize , availableX) ?
        			new Envelope(0 , gdalTargetXSize,0,  gdalTargetYSize) :	new Envelope(0, availableX, 0, availableY);
        	
        	final int pixels[] = executeQuery(
        			new Envelope(readFromX, readFromX + availableX, readFromY, readFromY + availableY),
             		targetDim,
             		datatype,
             		!useGDALAsResampler(gdalTargetXSize , availableX),
             		gdalTargetXSize , gdalTargetYSize);

            	
            return createBoundsTile(bitmap, pixels, coveredXOrigin,coveredYOrigin, gdalTargetXSize, gdalTargetYSize, ts, now);
            

        }else{ //this rectangle is fully covered by the file
        	Log.i(TAG, "reading of ("+readAmountX+","+readAmountY +") from "+readFromX+","+readFromY+" of file {"+w+","+h+"}");    	
        } 
        
        final Envelope targetDim = useGDALAsResampler(ts , readAmountX) ? 
        		new Envelope(0, ts, 0, ts) : new Envelope(0, readAmountX, 0, readAmountY);
       
        final int pixels[] = executeQuery(
        		new Envelope(readFromX, readFromX + readAmountX, readFromY, readFromY + readAmountY),
        		targetDim,
        		datatype,
        		!useGDALAsResampler(ts , readAmountX),
        		ts , ts);

        bitmap.setPixels(pixels, ts);
		
		Log.d(TAG, "tile  took "+((System.currentTimeMillis() - now) / 1000.0f)+ " s");

		return bitmap;
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
        
        final Raster raster = mRasterDataset.read(query);
        
		HashMap<Key,Object> renderParams = new HashMap<>();

//		renderParams.put(RenderOp.KEY_RENDERER, new MRenderer(mRasterDataset.getSource(), true));
		renderParams.put(RenderOp.KEY_RENDERER, new OpenCVRenderer());
		renderParams.put(RenderOp.KEY_FILEPATH, mRasterDataset.getSource());
		renderParams.put(RenderOp.KEY_RGB_BANDS, Boolean.valueOf(checkIfHasRGBBands()));	

        if(resample){
        
        	HashMap<Key,Object> resizeParams = new HashMap<>();

			resizeParams.put(ResampleOp.KEY_SIZE, new Envelope(0, targetWidth, 0, targetHeight));

			ResampleOp.resample(raster, resizeParams, null, null);
			
			return RenderOp.render(raster, renderParams, null, null);

        }else{
        	
        	return RenderOp.render(raster, renderParams, null, null);
        }
	}

	/**
	 * returns a Tile filled with pixels according to the NO_DATA_COLOR
	 * as the desired coordinates were not covered by the dataset
	 * @param bitmap to modify the pixels 
	 * @param ts destination tilesize
	 * @param timestamp when the creation of this tile started
	 * @return the modified bitmap
	 */
	public TileBitmap returnNoDataTile(final TileBitmap bitmap, final int ts, final long timestamp){
		//cannot read, create white tile
		int[] pixels = new int[ts * ts];
		for (int i = 0; i < pixels.length; i++) {
			pixels[i] = NO_DATA_COLOR;
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
					//NO_DATA_COLOR;
					pixels[pos] =  NO_DATA_COLOR;
				}

			}
		}
		bitmap.setPixels( pixels, destinationTileSize);
		Log.d(TAG, "tile  took "+((System.currentTimeMillis() - timestamp) / 1000.0f)+ " s");
		return bitmap;
	}
	
	/**
	 * saves a created TileBitmap to the applications folder for debugging 
	 * 
	 * the file is saved in the folder of the "parent" raster
	 * being named tile_x_y_zoom.png
	 * 
	 * @param tilebitmap
	 * @param job
	 */
	@SuppressWarnings("unused")
	private void saveBitmap(final TileBitmap tilebitmap,final RasterJob job){
		
		final String newFileName = String.format("tile_%d_%d_%d.png", job.tile.tileX, job.tile.tileY, job.tile.zoomLevel);
		final String fileName = mRasterDataset.getSource().substring(0, mRasterDataset.getSource().lastIndexOf("/") + 1) + newFileName;
		
		FileOutputStream out = null;
		try {

		    out = new FileOutputStream(fileName);
		    AndroidGraphicFactory.getBitmap(tilebitmap).compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
		    // PNG is a lossless format, the compression factor (100) is ignored
		} catch (Exception e) {
		    Log.e(TAG, "error saving bitmap");
		} finally {
		    try {
		        if (out != null) {
		            out.close();
		        }
		    } catch (IOException e) {
		    	Log.e(TAG, "error saving bitmap");
		    }
		}
	}
	
	public CoordinateReferenceSystem getCurrentCRS(){
		
		return mRasterDataset.getCRS();
	}
	
	public void setDesiredCRS(final String wkt){
		
		try{
			
			mRasterDataset.applyProjection(wkt);
			
		}catch(Exception e){
			Log.e(TAG, "Error setting desired CRS to : \n"+wkt);
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

	@Override
	public String getFilePath() {
		
		return mRasterDataset.getSource();
	}
	

}
