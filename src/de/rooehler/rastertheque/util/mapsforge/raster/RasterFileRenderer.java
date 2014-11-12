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

public class RasterFileRenderer {

	private final static String TAG = RasterFileRenderer.class.getSimpleName();

	private GraphicFactory graphicFactory;
	
	private Dataset dataset;

	private CoordinateTransformation mTransformation;
	
	private boolean isWorking = true;
	
//	private final String EPSG_3857 = "PROJCS[\"WGS 84 / Pseudo-Mercator\","+
//    "GEOGCS[\"WGS 84\","+
//        "DATUM[\"WGS_1984\","+
//            "SPHEROID[\"WGS 84\",6378137,298.257223563,"+
//                "AUTHORITY[\"EPSG\",\"7030\"]],"+
//            "AUTHORITY[\"EPSG\",\"6326\"]],"+
//        "PRIMEM[\"Greenwich\",0,"+
//            "AUTHORITY[\"EPSG\",\"8901\"]],"+
//        "UNIT[\"degree\",0.0174532925199433,"+
//            "AUTHORITY[\"EPSG\",\"9122\"]],"+
//        "AUTHORITY[\"EPSG\",\"4326\"]],"+
//    "PROJECTION[\"Mercator_1SP\"],"+
//    "PARAMETER[\"central_meridian\",0],"+
//    "PARAMETER[\"scale_factor\",1],"+
//    "PARAMETER[\"false_easting\",0],"+
//    "PARAMETER[\"false_northing\",0],"+
//    "UNIT[\"metre\",1,"+
//        "AUTHORITY[\"EPSG\",\"9001\"]],"+
//    "AXIS[\"X\",EAST],"+
//    "AXIS[\"Y\",NORTH],"+
//    "EXTENSION[\"PROJ4\",\"+proj=merc +a=6378137 +b=6378137 +lat_ts=0.0 +lon_0=0.0 +x_0=0.0 +y_0=0 +k=1.0 +units=m +nadgrids=@null +wktext  +no_defs\"],"+
//    "AUTHORITY[\"EPSG\",\"3857\"]]";
	
	private ColorMap mColorMap;
	
	private final boolean useColorMap = true;

	private byte mInitialZoom = -1;
	
	private int mRasterWidth;
	private int mRasterHeight;
	
	private float mWidthHeightRatio;
	
	private boolean distortQuadratic = false;


	public RasterFileRenderer(GraphicFactory graphicFactory, final Dataset pDataset, final String filePath) {

		this.graphicFactory = graphicFactory;

		this.dataset = pDataset;
				
		this.mRasterWidth = this.dataset.GetRasterXSize();
		
		this.mRasterHeight = this.dataset.getRasterYSize();
		
		this.mWidthHeightRatio = GDALDecoder.getWidthGeightRatio();
		
		SpatialReference hProj = new SpatialReference(this.dataset.GetProjectionRef());
		
		SpatialReference hLatLong =  hProj.CloneGeogCS();
		
		this.mTransformation = CoordinateTransformation.CreateCoordinateTransformation(hProj, hLatLong);

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
		
		if(mInitialZoom == -1){
			this.mInitialZoom = GDALDecoder.getStartZoomLevel(GDALDecoder.getBoundingBox().getCenterPoint(),job.tile.tileSize);
			
			if(this.mRasterHeight < job.tile.tileSize || this.mRasterWidth < job.tile.tileSize){
				int necessary = Math.min(this.mRasterHeight, this.mRasterWidth);
				int desired   = job.tile.tileSize;
				while(desired > necessary){
					this.mInitialZoom--;
					desired /= 2;
				}
				
			}
		}

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
        //TODO render all bands
        final Band band = bands.get(0);
        
        final double scaleFactor = scaleFactorAccordingToZoom(zoom);
        
//        final double desiredLat = MercatorProjection.tileYToLatitude(job.tile.tileY, zoom);
//        final double desiredLon = MercatorProjection.tileXToLongitude(job.tile.tileX, zoom);
//        
//        Log.i(TAG, String.format("desired %f  %f", desiredLat,desiredLon));
        
        int zoomedTS = (int) (tileSize * scaleFactor);
        
        final int readAmountX = distortQuadratic ? (int) (this.mWidthHeightRatio < 1.0 ? zoomedTS / this.mWidthHeightRatio : zoomedTS) : zoomedTS;
        final int readAmountY = distortQuadratic ? (int) (this.mWidthHeightRatio > 1.0 ? zoomedTS / this.mWidthHeightRatio : zoomedTS) : zoomedTS;
        
        long readFromX =  MercatorProjection.tileToPixel(job.tile.tileX, readAmountX);
        long readFromY =  MercatorProjection.tileToPixel(job.tile.tileY, readAmountY);
          
//        Log.d(TAG, String.format("tile %d %d zoom %d read from %d %d amount %d %d",job.tile.tileX,job.tile.tileY,job.tile.zoomLevel,readFromX,readFromY,readAmountX,readAmountY));

        
        
        if(readFromX < 0 || readFromX + readAmountX > this.mRasterWidth ||  readFromY < 0 || readFromY + readAmountY > this.mRasterHeight){
        	Log.e(TAG, "reading from "+readFromX+","+readFromY+" from file {"+this.mRasterWidth+","+this.mRasterHeight+"}");

        	//if entire desired area out of bounds return white tile
        	if(readFromX + readAmountX <= 0 || readFromX  > this.mRasterWidth ||
        	   readFromY + readAmountY <= 0 || readFromY  > this.mRasterHeight){
        		//cannot read, create white tile
        		int[] pixels = new int[tileSize * tileSize];
        		for (int i = 0; i < pixels.length; i++) {
        			pixels[i] = 0xffffffff;
        		}
        		bitmap.setPixels( pixels, tileSize);
        		Log.d(TAG, "tile  took "+((System.currentTimeMillis() - now) / 1000.0f)+ " s");
        		return bitmap;
        	}
        	
        	//out out bounds, needs special treatment
        	int availableX = readAmountX, availableY = readAmountY;
        	int gdalTargetXSize = tileSize, gdalTargetYSize = tileSize;
            int coveredXOrigin = 0, coveredYOrigin = 0;

        	if(readFromX + readAmountX > this.mRasterWidth || 	readFromY + readAmountY > this.mRasterHeight){
        		//max x or y bounds hit
        		if(readFromX + readAmountX > this.mRasterWidth){        			
        			availableX = (int) (this.mRasterWidth -  readFromX);   			
        			gdalTargetXSize = (int) (availableX * (1 / scaleFactor));
        		}
        		if(readFromY + readAmountY > this.mRasterHeight){        			
        			availableY = (int) (this.mRasterHeight - readFromY);  			
        			gdalTargetYSize = (int) (availableY * (1 / scaleFactor));
        		}
        	}
        	
        	if(readFromX < 0 || readFromY < 0){
        		//min x or y bounds hit
        		if(readFromX < 0){        			
        			availableX = (int) (readAmountX - Math.abs(readFromX));
        			coveredXOrigin = (int) (tileSize - (availableX * scaleFactor));
        			gdalTargetXSize = (int) (availableX * (1 /  scaleFactor));
        			readFromX = 0;
        		}
        		if(readFromY < 0){        			
        			availableY = (int) (readAmountY - Math.abs(readFromY));
        			coveredYOrigin = (int) (tileSize - (availableY * scaleFactor));
        			gdalTargetYSize = (int) (availableY * (1 / scaleFactor));
        			readFromY = 0;
        		}
            }
        	
        	final int bufferSize = gdalTargetXSize * gdalTargetYSize * datatype.size();
            ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
            
            buffer.order(ByteOrder.nativeOrder()); 

            band.ReadRaster_Direct((int)readFromX,(int)readFromY, availableX, availableY, gdalTargetXSize, gdalTargetYSize, RasterHelper.toGDAL(datatype), buffer);
            try{
            	bitmap.setPixels( generateOutOfBoundsPixels(buffer,coveredXOrigin,coveredYOrigin, gdalTargetXSize, gdalTargetYSize, tileSize, datatype), tileSize);
            }catch(ArrayIndexOutOfBoundsException e){
    			Log.e("ColorMap", "ioob",e);
    			int doh = 3;
    			return bitmap;
    		}
    		Log.d(TAG, "tile  took "+((System.currentTimeMillis() - now) / 1000.0f)+ " s");
    		return bitmap;


        }else{
        	Log.i(TAG, "reading from "+readFromX+","+readFromY+" from file {"+this.mRasterWidth+","+this.mRasterHeight+"}");    	
        }

        final int bufferSize = tileSize * tileSize * datatype.size();
        ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
        
        buffer.order(ByteOrder.nativeOrder()); 

        band.ReadRaster_Direct((int)readFromX,(int)readFromY, readAmountX, readAmountY, tileSize,tileSize, RasterHelper.toGDAL(datatype), buffer);

		bitmap.setPixels( generatePixels(buffer,(tileSize * tileSize), datatype), tileSize);
		
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
	public int[] generatePixels(final ByteBuffer pBuffer,final int tileSize, final DataType dataType){
	
		final ByteBufferReader reader = new ByteBufferReader(pBuffer.array(), ByteOrder.nativeOrder());
		
        int[] pixels = new int[tileSize];
        double[] minMax = new double[2];
        
        if(mColorMap == null){ // if colormap not available, calculate min max for grayscale

        	getMinMax(minMax, reader, tileSize, dataType);
       
        	Log.d(TAG, "rawdata min "+minMax[0] +" max "+minMax[1]);
        	reader.init();
        }
 
        for (int i = 0; i < tileSize; i++) {
        	
        	double d = getValue(reader, dataType);

    		if(mColorMap != null){
    			pixels[i] = pixelValueForColorMapAccordingToData(d);
    		}else{
    			pixels[i] = pixelValueForGrayScale(d, minMax[0], minMax[1]);
    		}
        }

        return pixels;
    } 
	/**
	 * @param a Databuffer according to the datatype of this raster
	 * @param pixelsWidth
	 * @param pixelsHeight
	 * @return
	 * @throws Exception
	 */
	public int[] generateOutOfBoundsPixels(final ByteBuffer pBuffer,
			final int coveredOriginX,
			final int coveredOriginY,
			final int coveredAreaX,
			final int coveredAreaY,
			final int tileSize,
			final DataType dataType){
	
		final ByteBufferReader reader = new ByteBufferReader(pBuffer.array(), ByteOrder.nativeOrder());
		
        int[] pixels = new int[tileSize * tileSize];
        double[] minMax = new double[2];
        
        if(mColorMap == null){ // if colormap not available, calculate min max for grayscale

        	getMinMax(minMax, reader, coveredAreaX * coveredAreaY, dataType);
       
        	Log.d(TAG, "rawdata min "+minMax[0] +" max "+minMax[1]);
        	reader.init();
        }
 
        for (int y = 0; y < tileSize; y++) {
        	for (int x = 0; x < tileSize; x++) {

        		int pos = y * tileSize + x;
        		
        		if( x  >= coveredOriginX && y >= coveredOriginY && x < coveredOriginX + coveredAreaX && y < coveredOriginY + coveredAreaY){
        			//gdalpixel
        			double d = getValue(reader, dataType);
        			
        			if(mColorMap != null){
        				pixels[pos] = pixelValueForColorMapAccordingToData(d);
        			}else{
        				pixels[pos] = pixelValueForGrayScale(d, minMax[0], minMax[1]);
        			}
        		}else {
        			//white pixel;
        			pixels[pos] =  0xffffffff;
        		}

        	}
        }

        return pixels;
    } 
	
	public double getValue(ByteBufferReader reader,final DataType dataType){

		double d = 0.0d;
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

		return d;
	}
	
	public void getMinMax(double[] result, ByteBufferReader reader, int pixelSize, final DataType dataType){
		double max =  Double.MIN_VALUE;
		double min =  Double.MAX_VALUE;

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
		result[0] = min;
		result[1] = max;
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
