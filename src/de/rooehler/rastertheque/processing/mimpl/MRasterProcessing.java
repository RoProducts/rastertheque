package de.rooehler.rastertheque.processing.mimpl;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.util.Log;
import de.rooehler.rastertheque.interfaces.RasterProcessing;
import de.rooehler.rastertheque.io.gdal.ByteBufferReader;
import de.rooehler.rastertheque.io.gdal.DataType;
import de.rooehler.rastertheque.processing.mimpl.colormap.ColorMap;
import de.rooehler.rastertheque.processing.mimpl.colormap.SLDColorMapParser;

public class MRasterProcessing implements RasterProcessing{
	
	private final static String TAG = MRasterProcessing.class.getSimpleName();
	
	private ColorMap mColorMap;
	
	public MRasterProcessing(final String pFilePath){
		
		final String colorMapFilePath = pFilePath.substring(0, pFilePath.lastIndexOf(".") + 1) + "sld";

		File file = new File(colorMapFilePath);

		if(file.exists()){

			this.mColorMap = SLDColorMapParser.parseColorMapFile(file);

		}
	}

	public int[] generatePixelsWithColorMap(final ByteBuffer pBuffer,final int bufferSize, final DataType dataType){
		
		if(mColorMap == null){
			throw new IllegalArgumentException("no colorMap available");
		}
		
		final ByteBufferReader reader = new ByteBufferReader(pBuffer.array(), ByteOrder.nativeOrder());
		
        int[] pixels = new int[bufferSize];
        
        for (int i = 0; i < bufferSize; i++) {
        	
        	double d = getValue(reader, dataType);

    		pixels[i] = pixelValueForColorMapAccordingToData(mColorMap,d);

        }

        return pixels;
	}
	
	
	
	@Override
	public int[] generateGrayScalePixelsCalculatingMinMax(ByteBuffer pBuffer,int bufferSize, DataType dataType) {

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


	private int pixelValueForGrayScale(double val, double min, double max){

		final double color = (val - min) / (max - min);
		int grey = (int) (color * 256);
		return 0xff000000 | ((((int) grey) << 16) & 0xff0000) | ((((int) grey) << 8) & 0xff00) | ((int) grey);

	}
	
	
	private int pixelValueForColorMapAccordingToData(final ColorMap colorMap, final double val){

		return colorMap.getColorAccordingToValue(val);
	}
	
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

	public void setColorMap(final ColorMap pColorMap){
		this.mColorMap = pColorMap;
	}
}
