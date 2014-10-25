/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.rooehler.rastertheque.util.mapsforge.raster;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.util.MercatorProjection;

import android.util.Log;
import de.rooehler.rastertheque.colormap.ColorMap;
import de.rooehler.rastertheque.colormap.SLDColorMapParser;
import de.rooehler.rastertheque.gdal.GDALDecoder;
import de.rooehler.rastertheque.util.RasterProperty;

public class RasterFileRenderer {

	private final static String TAG = RasterFileRenderer.class.getSimpleName();

	private GraphicFactory graphicFactory;
	
	private Dataset dataset;

	private CoordinateTransformation mTransformation;
	
	private boolean isWorking = false;
	
	private final String EPSG_3857 = "PROJCS[\"WGS 84 / Pseudo-Mercator\","+
    "GEOGCS[\"WGS 84\","+
        "DATUM[\"WGS_1984\","+
            "SPHEROID[\"WGS 84\",6378137,298.257223563,"+
                "AUTHORITY[\"EPSG\",\"7030\"]],"+
            "AUTHORITY[\"EPSG\",\"6326\"]],"+
        "PRIMEM[\"Greenwich\",0,"+
            "AUTHORITY[\"EPSG\",\"8901\"]],"+
        "UNIT[\"degree\",0.0174532925199433,"+
            "AUTHORITY[\"EPSG\",\"9122\"]],"+
        "AUTHORITY[\"EPSG\",\"4326\"]],"+
    "PROJECTION[\"Mercator_1SP\"],"+
    "PARAMETER[\"central_meridian\",0],"+
    "PARAMETER[\"scale_factor\",1],"+
    "PARAMETER[\"false_easting\",0],"+
    "PARAMETER[\"false_northing\",0],"+
    "UNIT[\"metre\",1,"+
        "AUTHORITY[\"EPSG\",\"9001\"]],"+
    "AXIS[\"X\",EAST],"+
    "AXIS[\"Y\",NORTH],"+
    "EXTENSION[\"PROJ4\",\"+proj=merc +a=6378137 +b=6378137 +lat_ts=0.0 +lon_0=0.0 +x_0=0.0 +y_0=0 +k=1.0 +units=m +nadgrids=@null +wktext  +no_defs\"],"+
    "AUTHORITY[\"EPSG\",\"3857\"]]";
	
	private RasterProperty mRasterProperty;
	private ColorMap mColorMap;

	public RasterFileRenderer(GraphicFactory graphicFactory, final Dataset pDataset, final String filePath) {

		this.graphicFactory = graphicFactory;

		this.dataset = pDataset;
		
		this.mRasterProperty = GDALDecoder.getRasterProperties(pDataset);
		
		//create a transformation which will turn WGS 84 / Pseudo-Mercator (EPSG:3857) coordinates coming from the job
		// to coordinates available in the dataset
		
		final SpatialReference inSpatialRef = new SpatialReference(EPSG_3857);
		
		final SpatialReference outSpatialRef = new SpatialReference(this.dataset.GetProjectionRef());
		
		this.mTransformation = CoordinateTransformation.CreateCoordinateTransformation(inSpatialRef, outSpatialRef);
		
		final String colorMapFilePath = filePath.substring(0, filePath.lastIndexOf(".") + 1) + "sld";
		
		File file = new File(colorMapFilePath);
		
		if(file.exists()){

			mColorMap = SLDColorMapParser.parseColorMapFile(file);
		}
	}

	/**
	 * called from RasterFileWorkerThread : executes a mapgeneratorJob and modifies the @param bitmap which will be the
	 * result according to the parameters inside @param mapGeneratorJob
	 */
	public TileBitmap executeJob(RasterFileJob job) {

		final int tileSize = job.tileSize;
		
		final short zoom = job.tile.zoomLevel;

		TileBitmap bitmap = this.graphicFactory.createTileBitmap(job.displayModel.getTileSize(), job.hasAlpha);

		List<Band> bands = RasterHelper.getBands(dataset);

        DataType datatype = DataType.BYTE;
        for (int i = 0 ; i < bands.size(); i++) {
            Band band = bands.get(i);
            DataType dt = RasterHelper.getDatatype(band);
            if (dt.compareTo(datatype) > 0) {
                datatype = dt;
            }
        }
        int zoomedTS = (int) (tileSize * scaleFactorAccordingToZoom(zoom));
        
        Log.d(TAG, String.format("tile %d %d zoom %d zoomedTileSize %d",job.tile.tileX,job.tile.tileY,job.tile.zoomLevel,zoomedTS));
        
        long pixelX =  MercatorProjection.tileToPixel(job.tile.tileX, zoomedTS);
        long pixelY =  MercatorProjection.tileToPixel(job.tile.tileY, zoomedTS);
        if(pixelX + zoomedTS > mRasterProperty.getmRasterXSize() && 
           pixelY + zoomedTS > mRasterProperty.getmRasterYSize()){
        	//x and y bounds hit
        	zoomedTS = Math.min((int) (mRasterProperty.getmRasterXSize() - (pixelX + zoomedTS)),
        						(int) (mRasterProperty.getmRasterYSize() - (pixelY + zoomedTS)));
        }else if(pixelX + zoomedTS > mRasterProperty.getmRasterXSize()){
        	//x bounds hit
        	zoomedTS = (int) (mRasterProperty.getmRasterXSize() - (pixelX + zoomedTS));
		}else if(pixelY + zoomedTS > mRasterProperty.getmRasterYSize()){
        	//y bounds hit
        	zoomedTS = (int) (mRasterProperty.getmRasterYSize() - (pixelY + zoomedTS));
        }
        
        final Band band = bands.get(0);
        
        ByteBuffer buffer = ByteBuffer.allocateDirect(tileSize * tileSize * datatype.size());
        
        buffer.order(ByteOrder.nativeOrder()); 
        
        Log.d(TAG, String.format("conversion from tile %d %d to coords %d %d", job.tile.tileX, job.tile.tileY,pixelX,pixelY));
        
            // single band, read in same units as requested buffer
        band.ReadRaster_Direct((int)pixelX,(int)pixelY, zoomedTS, zoomedTS, tileSize,tileSize, buffer);

		// copy all pixels from the color array to the tile bitmap
		bitmap.setPixels(generateGreyScalePixels(buffer.array(), tileSize, tileSize), tileSize);

		return bitmap;
	}
	/**
	 * apply a colormap to an array one dimensional (elevation) values
	 * @param rawdata
	 * @param pixelsWidth
	 * @param pixelsHeight
	 * @return
	 * @throws Exception
	 */
	public int[] generateGreyScalePixels(byte[] rawdata, int pixelsWidth, int pixelsHeight){

        int[] pixels = new int[rawdata.length];
        
    	
    	float max =  Byte.MIN_VALUE;
    	float min =  Byte.MAX_VALUE;
    	
    	for (int i = 0;i< rawdata.length;i++){
    		
    			if (rawdata[i] < min){
    				min = rawdata[i];
    			}
    			if(rawdata[i] > max){
    				max = rawdata[i];
    			}	
    	}        
    	
    	//Log.d(TAG, "rawdata min "+min +" max "+max);
        
        
        
        for( int i = 0; i < rawdata.length; i++ ){        
        	
           	
            	if(mColorMap != null){
            		int rgb = mColorMap.getColorAccordingToValue(rawdata[i]);
            		int r = (rgb >> 16) & 0x000000FF;
            		int g = (rgb >> 8 ) & 0x000000FF;
            		int b = (rgb)       & 0x000000FF;
            		pixels[i] = 0xff000000 | ((((int) r) << 16) & 0xff0000) | ((((int) g) << 8) & 0xff00) | ((int) b);
            	}else{
            		final float color = (float) ((rawdata[i] - min) / (max - min));
            		
            		int grey = (int) (color * 256);
            		pixels[i] = 0xff000000 | ((((int) grey) << 16) & 0xff0000) | ((((int) grey) << 8) & 0xff00) | ((int) grey);
            	}

        }

        return pixels;
    } 
	public class ByteBufferBackedInputStream extends InputStream {

	    ByteBuffer buf;

	    public ByteBufferBackedInputStream(ByteBuffer buf) {
	        this.buf = buf;
	    }

	    public int read() throws IOException {
	        if (!buf.hasRemaining()) {
	            return -1;
	        }
	        return buf.get() & 0xFF;
	    }

	    public int read(byte[] bytes, int off, int len) throws IOException {
	        if (!buf.hasRemaining()) {
	            return -1;
	        }

	        len = Math.min(len, buf.remaining());
	        buf.get(bytes, off, len);
	        return len;
	    }
	}

	public void start() {

		this.isWorking = true;

	}

	public void stop() {

		this.isWorking = false;

	}

	public boolean isWorking() {

		return this.isWorking;
	}

	/**
	 * closes and destroys any resources needed
	 */
	public void destroy() {

		stop();

	}
	
	public double scaleFactorAccordingToZoom(short zoom){
		
		switch (zoom) {

		case 8  : return 0.03125;
		case 7  : return 0.0625;
		case 6  : return 0.125;
		case 5  : return 0.25;
		case 4  : return 0.5;
		case 3  : return 1;
		case 2  : return 2;
		case 1  : return 4;

		default:
			break;
		}
		return 0;
	}

}
