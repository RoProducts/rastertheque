package de.rooehler.rastertheque.processing.rendering;

import java.io.EOFException;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

import android.util.Log;
import de.rooehler.rastertheque.core.DataType;
import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.core.util.ByteBufferReader;
import de.rooehler.rastertheque.core.util.ByteBufferReaderUtil;
import de.rooehler.rastertheque.processing.RasterOp;
import de.rooehler.rastertheque.processing.RasterOps;
import de.rooehler.rastertheque.util.Hints;
import de.rooehler.rastertheque.util.Hints.Key;
import de.rooehler.rastertheque.util.ProgressListener;
/**
 *  Manual implementation of the amplitude rescaling operation
 *  
 *  if no min/max parameter is provided is determines them
 *  
 *  and subsequently interpolates raster values as grey scale pixels
 *  
 *  according to the determined range
 *  
 * @author Robert Oehler
 *
 */
public class MAmplitudeRescaler extends AmplitudeRescaler implements RasterOp, Serializable {


	private static final long serialVersionUID = 2138351083682731966L;
	
	/**
	 * generates an array of colored gray-scale
	 *  pixels for a buffer of raster pixels
	 *  
	 * @param pBuffer the buffer to read from
	 * @param pixelAmount amount of raster pixels
	 * @param dataType the dataType of the raster pixels
	 * @return the array of color pixels
	 */
	@Override
	public void execute(Raster raster, Map<Key, Serializable> params, Hints hints, ProgressListener listener) {
		
		double[] minMax = null;
		if(params != null){
			if(params.containsKey(KEY_MINMAX)){
				 minMax = (double[]) params.get(KEY_MINMAX);									
			}
		}
		
		final int raster_width  = raster.getDimension().width();
		final int raster_height = raster.getDimension().height();
		
		final int pixelAmount = raster_width * raster_height;
		int[] pixels = new int[pixelAmount];
		final ByteBufferReader reader = new ByteBufferReader(raster.getData().array(), ByteOrder.nativeOrder());
		
		if(minMax == null){
			minMax = new double[2];
			getMinMax(minMax, reader, pixelAmount, raster.getBands().get(0).datatype());
			reader.init();
		}
    	Log.d(MAmplitudeRescaler.class.getSimpleName(), "rawdata min "+minMax[0] +" max "+minMax[1]);

    	for (int i = 0; i < pixelAmount; i++) {
        	
        	double d = ByteBufferReaderUtil.getValue(reader, raster.getBands().get(0).datatype());

    		pixels[i] = pixelValueForGrayScale(d, minMax[0], minMax[1]);

        }

    	ByteBuffer buffer = ByteBuffer.allocate(pixels.length * 4);
		
		buffer.asIntBuffer().put(pixels);
		
		raster.setData(buffer);
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
				Log.e(MAmplitudeRescaler.class.getSimpleName(), "error reading from byteBufferedReader");
			}
		}
		result[0] = min;
		result[1] = max;
	}
	
	@Override
	public String getOperationName() {

		return RasterOps.AMPLITUDE_RESCALING;
	}

	@Override
	public Priority getPriority() {
	
		return Priority.NORMAL;
	}
	
}
