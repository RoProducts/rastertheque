package de.rooehler.rastertheque.processing.colormap;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.util.Log;
import de.rooehler.rastertheque.io.gdal.ByteBufferReader;
import de.rooehler.rastertheque.io.gdal.DataType;
import de.rooehler.rastertheque.processing.IColorMapProcessing;

public class MColorMapProcessing implements IColorMapProcessing{
	
	private final static String TAG = MColorMapProcessing.class.getSimpleName();
	
	private ColorMap mColorMap;
	
	public MColorMapProcessing(final String pFilePath){
		
		final String colorMapFilePath = pFilePath.substring(0, pFilePath.lastIndexOf(".") + 1) + "sld";

		File file = new File(colorMapFilePath);

		if(file.exists()){

			this.mColorMap = SLDColorMapParser.parseColorMapFile(file);

		}
	}
	
	@Override
	public boolean hasColorMap() {
		
		return this.mColorMap != null;
	}
	
	@Override
	public int[] generateThreeBandedRGBPixels(final ByteBuffer pBuffer, final int bufferSize,final DataType dataType) {
		
		final ByteBufferReader reader = new ByteBufferReader(pBuffer.array(), ByteOrder.nativeOrder());
		
		int [] pixels = new int[bufferSize];
		
		double[] pixelsR = new double[bufferSize];
		double[] pixelsG = new double[bufferSize];
		double[] pixelsB = new double[bufferSize];
           
		for (int i = 0; i < bufferSize; i++) {	
			pixelsR[i] =  getValue(reader, dataType);
		}
		for (int j = 0; j < bufferSize; j++) {	
			pixelsG[j] =  getValue(reader, dataType);
		}
		for (int k = 0; k < bufferSize; k++) {	
			pixelsB[k] =  getValue(reader, dataType);
		}
		
        for (int l = 0; l < bufferSize; l++) {	
        	
        	double r = pixelsR[l];
        	double g = pixelsG[l];
        	double b = pixelsB[l];
        	
        	pixels[l] = 0xff000000 | ((((int) r) << 16) & 0xff0000) | ((((int) g) << 8) & 0xff00) | ((int) b);
        }
        
		return pixels;
	}

	/**
	 * generates an array of colored pixels for a buffer of raster pixels according to a priorly loaded ColorMap
	 * if the colorMap is not created priorly by either setting it or by placing a .sld file of the same name as the
	 * raster file in the same directory like the raster file an exception is thrown
	 * @param pBuffer the buffer to read from
	 * @param bufferSize amount of raster pixels
	 * @param dataType the dataType of the raster pixels
	 * @return the array of color pixels
	 */
	@Override
	public int[] generatePixelsWithColorMap(final ByteBuffer pBuffer,final int bufferSize, final DataType dataType){
		
		if(mColorMap == null){
			throw new IllegalArgumentException("no colorMap available");
		}
		
		final ByteBufferReader reader = new ByteBufferReader(pBuffer.array(), ByteOrder.nativeOrder());
		
        int[] pixels = new int[bufferSize];
        
        for (int i = 0; i < bufferSize; i++) {
        	
        	double d = getValue(reader, dataType);

    		pixels[i] = pixelValueForColorMapAccordingToData(d);

        }

        return pixels;
	}
	
	
	
	/**
	 * generates an array of colored gray-scale pixels for a buffer of raster pixels
	 * @param pBuffer the buffer to read from
	 * @param bufferSize amount of raster pixels
	 * @param dataType the dataType of the raster pixels
	 * @return the array of color pixels
	 */
	@Override
	public int[] generateGrayScalePixelsCalculatingMinMax(final ByteBuffer pBuffer,final int bufferSize,final DataType dataType) {


		int[] pixels = new int[bufferSize];
	    double[] minMax = new double[2];
			 
		final ByteBufferReader reader = new ByteBufferReader(pBuffer.array(), ByteOrder.nativeOrder());
		
	 	getMinMax(minMax, reader, bufferSize, dataType);
	       
    	Log.d(TAG, "rawdata min "+minMax[0] +" max "+minMax[1]);
    	reader.init();

    	for (int i = 0; i < bufferSize; i++) {
        	
        	double d = getValue(reader, dataType);

    		pixels[i] = pixelValueForGrayScale(d, minMax[0], minMax[1]);

        }

        return pixels;
	}

	/**
	 * returns a (grayscale color) int value according to the @param val inside the range of @param min and @param max  
	 * @param pixel value to calculate a color for
	 * @param min value
	 * @param max value
	 * @return the calculated color value
	 */
	private int pixelValueForGrayScale(double val, double min, double max){

		final double color = (val - min) / (max - min);
		int grey = (int) (color * 256);
		return 0xff000000 | ((((int) grey) << 16) & 0xff0000) | ((((int) grey) << 8) & 0xff00) | ((int) grey);

	}
	
	/**
	 * returns a (color) int value accroding to the @param val in the colorMap
	 * @param pixel value to get a color for
	 * @return the color value according to the value
	 */
	private int pixelValueForColorMapAccordingToData(final double val){

		return mColorMap.getColorAccordingToValue(val);
	}
	/**
	 * retrieve a value from the ByteBufferReader according to its datatype
	 * actually the data is read and for a unified return type is cast to double
	 * @param reader the reader to read from
	 * @param dataType the datatype according to which the data is read
	 * @return the value of the pixel
	 */
	private double getValue(ByteBufferReader reader,final DataType dataType){

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
	/**
	 * iterates over the pixelsize, determining min and max value of the data in 
	 * the ByteBufferReader according to its datatype
	 * @param result array in order {min, max}
	 * @param reader the reader to read from 	
	 * @param pixelSize the amount of pixels to check
	 * @param dataType the datatype according to which the data is read
	 */
	private void getMinMax(double[] result, ByteBufferReader reader, int pixelSize, final DataType dataType){
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
	/**
	 * set a ColorMap to be used by this object
	 * @param pColorMap the colorMap to use
	 */
	public void setColorMap(final ColorMap pColorMap){
		this.mColorMap = pColorMap;
	}


}
