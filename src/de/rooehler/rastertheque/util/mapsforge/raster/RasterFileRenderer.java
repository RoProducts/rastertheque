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
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.model.LatLong;
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
	
	private boolean isWorking = true;
	
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
	
	private final boolean useColorMap = true;
	
	private LatLong origin;
	private byte mInitialZoom;
	
	private int mWidth;
	private int mHeight;
	


	public RasterFileRenderer(GraphicFactory graphicFactory, final Dataset pDataset, final String filePath) {

		this.graphicFactory = graphicFactory;

		this.dataset = pDataset;
		
		this.mRasterProperty = GDALDecoder.getRasterProperties(pDataset);
		
		this.mInitialZoom = GDALDecoder.getStartZoomLevel(GDALDecoder.getBoundingBox().getCenterPoint());
		
		this.mWidth = this.dataset.GetRasterXSize();
		
		this.mHeight = this.dataset.getRasterYSize();
		
		SpatialReference hProj = new SpatialReference(this.dataset.GetProjectionRef());
		
		SpatialReference hLatLong =  hProj.CloneGeogCS();
		
		this.mTransformation = CoordinateTransformation.CreateCoordinateTransformation(hProj, hLatLong);
		
		double[] adfGeoTransform = new double[6];

		dataset.GetGeoTransform(adfGeoTransform);

		if (adfGeoTransform[2] == 0.0 && adfGeoTransform[4] == 0.0) {
			
			double[] transPoint = new double[3];
			mTransformation.TransformPoint(transPoint, adfGeoTransform[0], adfGeoTransform[3], 0);
			Log.d(TAG,"Origin(raw) : ("+ transPoint[0] +", "+transPoint[1]+ ")");
			origin = new LatLong(transPoint[1], transPoint[0]);
			Log.d(TAG,"Origin (ll) : ("+ origin.longitude +", "+origin.latitude+ ")");
			
			
		}else{
			//what is it ?
			//TODO handle
			throw new IllegalArgumentException("unexpected transformation");
		}

		if(useColorMap){
			
			final String colorMapFilePath = filePath.substring(0, filePath.lastIndexOf(".") + 1) + "sld";

			File file = new File(colorMapFilePath);

			if(file.exists()){

				mColorMap = SLDColorMapParser.parseColorMapFile(file);
			}
		}
	}

	/**
	 * called from RasterFileWorkerThread : executes a mapgeneratorJob and modifies the @param bitmap which will be the
	 * result according to the parameters inside @param mapGeneratorJob
	 */
	public TileBitmap executeJob(RasterFileJob job) {

		long now = System.currentTimeMillis();

		final int tileSize = job.tile.tileSize;
		
		final byte zoom = job.tile.zoomLevel;


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
         final double scaleFactor = scaleFactorAccordingToZoom(zoom);
        Log.d(TAG, String.format("scaleFactor for initial zoom %d current zoom %d :  %f",this.mInitialZoom,zoom,scaleFactor));
        int zoomedTS = (int) (tileSize * scaleFactor);
        
        Log.d(TAG, String.format("tile %d %d zoom %d zoomedTileSize %d",job.tile.tileX,job.tile.tileY,job.tile.zoomLevel,zoomedTS));
        
        long pixelX =  MercatorProjection.tileToPixel(job.tile.tileX, zoomedTS);
        long pixelY =  MercatorProjection.tileToPixel(job.tile.tileY, zoomedTS);
        
        double originX = MercatorProjection.tileToPixel(MercatorProjection.longitudeToTileX(origin.longitude, zoom),zoomedTS);
        double originY = MercatorProjection.tileToPixel(MercatorProjection.latitudeToTileY(origin.latitude, zoom), zoomedTS);
        
        long readFromX = (long) (pixelX - originX);
        long readFromY = (long) (pixelY - originY);
        
        if(readFromX < 0 || readFromX > this.mWidth ||  readFromY < 0 || readFromY > this.mHeight){
        	Log.e(TAG, "reading from "+readFromX+","+readFromY+" from file {"+this.mWidth+","+this.mHeight+"}");
        }else{
        	Log.i(TAG, "reading from "+readFromX+","+readFromY+" from file {"+this.mWidth+","+this.mHeight+"}");    	
        }
        
        if(readFromX + zoomedTS > mRasterProperty.getmRasterXSize() && 
           readFromY + zoomedTS > mRasterProperty.getmRasterYSize()){
        	//x and y bounds hit
        	zoomedTS = Math.min((int) (mRasterProperty.getmRasterXSize() - (pixelX + zoomedTS)),
        						(int) (mRasterProperty.getmRasterYSize() - (pixelY + zoomedTS)));
        }else if(readFromX + zoomedTS > mRasterProperty.getmRasterXSize()){
        	//x bounds hit
        	zoomedTS = (int) (mRasterProperty.getmRasterXSize() - (pixelX + zoomedTS));
		}else if(readFromY + zoomedTS > mRasterProperty.getmRasterYSize()){
        	//y bounds hit
        	zoomedTS = (int) (mRasterProperty.getmRasterYSize() - (pixelY + zoomedTS));
        }
        
        final Band band = bands.get(0);
        final int bufferSize = tileSize * tileSize * datatype.size();
        ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
        
        buffer.order(ByteOrder.nativeOrder()); 
        
        //Log.d(TAG, String.format("conversion from tile %d %d to coords %d %d", job.tile.tileX, job.tile.tileY,pixelX,pixelY));
        
            // single band, read in same units as requested buffer
        band.ReadRaster_Direct((int)readFromX,(int)readFromY, zoomedTS, zoomedTS, tileSize,tileSize, RasterHelper.toGDAL(datatype), buffer);

		// copy all pixels from the color array to the tile bitmap
		bitmap.setPixels(generatePixels(buffer,bufferSize, datatype, tileSize, tileSize), tileSize);
		Log.d(TAG, "tile  took "+((System.currentTimeMillis() - now) / 1000.0f)+ " s");
		return bitmap;
	}
	/**
	 * @param a Databuffer according to the datatype of this raster
	 * @param pixelsWidth
	 * @param pixelsHeight
	 * @return
	 * @throws Exception
	 */
	public int[] generatePixels(final ByteBuffer buffer,final int bufferSize, final DataType dataType, int pixelsWidth, int pixelsHeight){

		int wordSize = dataType.bits();
		int byteSize = wordSize / 8;
		
		int pixelSize = bufferSize / byteSize;
        int[] pixels = new int[pixelSize];
        double max =  Double.MIN_VALUE;
        double min =  Double.MAX_VALUE;
        
        
        if(mColorMap == null){ //only needed if colormap not available
        	while(buffer.hasRemaining()){
        		switch(dataType) {
        		case CHAR:
        			char _char = buffer.getChar();
        			if(_char > max){
        				max = _char;
        			}
        			if(_char < min){
        				min = _char;
        			}
        			break;
        		case BYTE:
        			byte _byte = buffer.get();
        			if(_byte > max){
        				max = _byte;
        			}
        			if(_byte < min){
        				min = _byte;
        			}
        			break;
        		case SHORT:
        			short _short = buffer.getShort();
        			if(_short > max){
        				max = _short;
        			}
        			if(_short < min){
        				min = _short;
        			}
        			break;
        		case INT:
        			int _int = buffer.getInt();
        			if(_int > max){
        				max = _int;
        			}
        			if(_int < min){
        				min = _int;
        			}
        			break;
        		case LONG:
        			long _long = buffer.getLong();
        			if(_long > max){
        				max = _long;
        			}
        			if(_long < min){
        				min = _long;
        			}
        			break;
        		case FLOAT:
        			float _float = buffer.getFloat();
        			if(_float > max){
        				max = _float;
        			}
        			if(_float < min){
        				min = _float;
        			}
        			break;
        		case DOUBLE:
        			double _double = buffer.getDouble();
        			if(_double > max){
        				max = _double;
        			}
        			if(_double < min){
        				min = _double;
        			}
        			break;
        		}
        	}
       
        	Log.d(TAG, "rawdata min "+min +" max "+max);
        }
        
        
        buffer.rewind();
        
        double d = 0.0d;
 
        for (int i = 0; i < pixelSize; i++) {
        	
    		switch(dataType) {
    		case CHAR:
    			char _char = buffer.getChar();
    			d = (double) _char;
    			break;
    		case BYTE:
    			byte _byte = buffer.get();
    			d = (double) _byte;

    			break;
    		case SHORT:
    			short _short = buffer.getShort();
    			d = (double) _short;

    			break;
    		case INT:
    			int _int = buffer.getInt();
    			d = (double) _int;

    			break;
    		case LONG:
    			long _long = buffer.getLong();
    			d = (double) _long;

    			break;
    		case FLOAT:
    			float _float = buffer.getFloat();
    			d = (double) _float;

    			break;
    		case DOUBLE:
    			double _double =  buffer.getDouble();
    			d = _double;
    		}


    		if(mColorMap != null){
    			pixels[i] = pixelValueForColorMapAccordingToData(d);
    		}else{
    			pixels[i] = pixelValueForGrayScale(d, min, max);
    		}
        }

        return pixels;
    } 
	
	public int pixelValueForColorMapAccordingToData(double val){
		
    		int rgb = mColorMap.getColorAccordingToValue(val);
    		int r = (rgb >> 16) & 0x000000FF;
    		int g = (rgb >> 8 ) & 0x000000FF;
    		int b = (rgb)       & 0x000000FF;
    		return 0xff000000 | ((((int) r) << 16) & 0xff0000) | ((((int) g) << 8) & 0xff00) | ((int) b);
	}
	
	public int pixelValueForGrayScale(double val, double min, double max){
		
		final double color = (val - min) / (max - min);
		int grey = (int) (color * 256);
		return 0xff000000 | ((((int) grey) << 16) & 0xff0000) | ((((int) grey) << 8) & 0xff00) | ((int) grey);
		
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
		
		
		return Math.pow(2,  -(zoom - this.mInitialZoom));
		
	}
	
//	public double scaleFactorAccordingToZoom(short zoom){
//		
//		switch (zoom) {
//
//		case 8  : return 0.03125;
//		case 7  : return 0.0625;
//		case 6  : return 0.125;
//		case 5  : return 0.25;
//		case 4  : return 0.5;
//		case 3  : return 1;
//		case 2  : return 2;
//		case 1  : return 4;
//
//		default:
//			break;
//		}
//		return 0;
//	}

}
