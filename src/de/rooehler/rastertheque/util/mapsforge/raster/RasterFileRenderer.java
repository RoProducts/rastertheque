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

import java.io.EOFException;
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
	
	private LatLong mOrigin;
	private byte mInitialZoom;
	
	private int mRasterWidth;
	private int mRasterHeight;
	


	public RasterFileRenderer(GraphicFactory graphicFactory, final Dataset pDataset, final String filePath) {

		this.graphicFactory = graphicFactory;

		this.dataset = pDataset;
		
		this.mRasterProperty = GDALDecoder.getRasterProperties(pDataset);
		
		this.mInitialZoom = GDALDecoder.getStartZoomLevel(GDALDecoder.getBoundingBox().getCenterPoint());
		
		this.mRasterWidth = this.dataset.GetRasterXSize();
		
		this.mRasterHeight = this.dataset.getRasterYSize();
		
		SpatialReference hProj = new SpatialReference(this.dataset.GetProjectionRef());
		
		SpatialReference hLatLong =  hProj.CloneGeogCS();
		
		this.mTransformation = CoordinateTransformation.CreateCoordinateTransformation(hProj, hLatLong);
		
		double[] adfGeoTransform = new double[6];

		dataset.GetGeoTransform(adfGeoTransform);

		if (adfGeoTransform[2] == 0.0 && adfGeoTransform[4] == 0.0) {
			
			double[] transPoint = new double[3];
			mTransformation.TransformPoint(transPoint, adfGeoTransform[0], adfGeoTransform[3], 0);
			Log.d(TAG,"Origin(raw) : ("+ transPoint[0] +", "+transPoint[1]+ ")");
			mOrigin = new LatLong(transPoint[1], transPoint[0]);
			Log.d(TAG,"Origin (ll) : ("+ mOrigin.longitude +", "+mOrigin.latitude+ ")");
			
			
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
        
        final long pixelX =  MercatorProjection.tileToPixel(job.tile.tileX, zoomedTS);
        final long pixelY =  MercatorProjection.tileToPixel(job.tile.tileY, zoomedTS);
        
        final long originX = MercatorProjection.tileToPixel(MercatorProjection.longitudeToTileX(mOrigin.longitude, zoom),zoomedTS);
        final long originY = MercatorProjection.tileToPixel(MercatorProjection.latitudeToTileY(mOrigin.latitude, zoom), zoomedTS);
        
        final long readFromX = pixelX - originX;
        final long readFromY = pixelY - originY;
        
        if(readFromX < 0 || readFromX > this.mRasterWidth ||  readFromY < 0 || readFromY > this.mRasterHeight){
        	Log.e(TAG, "reading from "+readFromX+","+readFromY+" from file {"+this.mRasterWidth+","+this.mRasterHeight+"}");
        }else{
        	Log.i(TAG, "reading from "+readFromX+","+readFromY+" from file {"+this.mRasterWidth+","+this.mRasterHeight+"}");    	
        }
        
        if(readFromX + zoomedTS > this.mRasterWidth && 
           readFromY + zoomedTS > this.mRasterHeight){
        	//x and y bounds hit
        	zoomedTS = Math.min((int) (this.mRasterWidth - (pixelX + zoomedTS)),
        						(int) (this.mRasterHeight - (pixelY + zoomedTS)));
        }else if(readFromX + zoomedTS > this.mRasterWidth){
        	//x bounds hit
        	zoomedTS = (int) (this.mRasterWidth - (pixelX + zoomedTS));
		}else if(readFromY + zoomedTS > mRasterProperty.getmRasterYSize()){
        	//y bounds hit
        	zoomedTS = (int) (this.mRasterHeight - (pixelY + zoomedTS));
        }
        
        if(readFromX + zoomedTS <= 0 ||
        		readFromX + zoomedTS > this.mRasterWidth ||
        		readFromY + zoomedTS <= 0 ||
        		readFromY + zoomedTS > this.mRasterHeight ||
        		zoomedTS <= 0){
        	//cannot read, create white tile
        	int[] pixels = new int[tileSize * tileSize];
        	for (int i = 0; i < pixels.length; i++) {
				pixels[i] = 0xffffffff;
			}
        	bitmap.setPixels( pixels, tileSize);
        	Log.d(TAG, "tile  took "+((System.currentTimeMillis() - now) / 1000.0f)+ " s");
        	return bitmap;
        }
        
        //TODO render all bands
        final Band band = bands.get(0);
        final int bufferSize = tileSize * tileSize * datatype.size();
        ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
        
        buffer.order(ByteOrder.nativeOrder()); 

        band.ReadRaster_Direct((int)readFromX,(int)readFromY, zoomedTS, zoomedTS, tileSize,tileSize, RasterHelper.toGDAL(datatype), buffer);

		bitmap.setPixels( generatePixels(buffer,bufferSize, datatype, tileSize, tileSize), tileSize);
		
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
	public int[] generatePixels(final ByteBuffer pBuffer,final int bufferSize, final DataType dataType, int pixelsWidth, int pixelsHeight){

		int wordSize = dataType.bits();
		int byteSize = wordSize / 8;
		
		final ByteBufferReader reader = new ByteBufferReader(pBuffer.array(), ByteOrder.nativeOrder());
		
		int pixelSize = bufferSize / byteSize;
        int[] pixels = new int[pixelSize];
        double max =  Double.MIN_VALUE;
        double min =  Double.MAX_VALUE;
        
        
        if(mColorMap == null){ //only needed if colormap not available
        	 for (int i = 0; i < pixelSize; i++) {
        		try{
        			switch(dataType) {
        			case CHAR:
        				char _char = reader.readChar();
        				if(_char > max){
        					max = _char;
        				}
        				if(_char < min){
        					min = _char;
        				}
        				break;
        			case BYTE:
        				byte _byte = reader.readByte();
        				if(_byte > max){
        					max = _byte;
        				}
        				if(_byte < min){
        					min = _byte;
        				}
        				break;
        			case SHORT:
        				short _short = reader.readShort();
        				if(_short > max){
        					max = _short;
        				}
        				if(_short < min){
        					min = _short;
        				}
        				break;
        			case INT:
        				int _int = reader.readInt();
        				if(_int > max){
        					max = _int;
        				}
        				if(_int < min){
        					min = _int;
        				}
        				break;
        			case LONG:
        				long _long = reader.readLong();
        				if(_long > max){
        					max = _long;
        				}
        				if(_long < min){
        					min = _long;
        				}
        				break;
        			case FLOAT:
        				float _float = reader.readFloat();
        				if(_float > max){
        					max = _float;
        				}
        				if(_float < min){
        					min = _float;
        				}
        				break;
        			case DOUBLE:
        				double _double = reader.readDouble();
        				if(_double > max){
        					max = _double;
        				}
        				if(_double < min){
        					min = _double;
        				}
        				break;
        			}
        		}catch(EOFException e){
        			break;
        		}catch(IOException  e){
        			Log.e(TAG, "error reading from byteBufferedReader");
        		}
        	}
       
        	Log.d(TAG, "rawdata min "+min +" max "+max);
        	reader.init();
        }

        
        double d = 0.0d;
 
        for (int i = 0; i < pixelSize; i++) {
        	try{
        		switch(dataType) {
        		case CHAR:
        			char _char = reader.readChar();
        			d = (double) _char;
        			break;
        		case BYTE:
        			byte _byte = reader.readByte();
        			d = (double) _byte;
        			break;
        		case SHORT:
        			short _short = reader.readShort();
        			d = (double) _short;
        			break;
        		case INT:
        			int _int = reader.readInt();
        			d = (double) _int;
        			break;
        		case LONG:
        			long _long = reader.readLong();
        			d = (double) _long;
        			break;
        		case FLOAT:
        			float _float = reader.readFloat();
        			d = (double) _float;
        			break;
        		case DOUBLE:
        			double _double =  reader.readDouble();
        			d = _double;
        			break;
        		}
        	}catch(IOException  e){
        		Log.e(TAG, "error reading from byteBufferedReader");
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
