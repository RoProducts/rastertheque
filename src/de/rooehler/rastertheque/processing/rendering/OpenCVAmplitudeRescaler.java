package de.rooehler.rastertheque.processing.rendering;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Map;

import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import android.os.Build;
import android.util.Log;
import de.rooehler.rastertheque.core.DataType;
import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.core.util.ByteBufferReader;
import de.rooehler.rastertheque.core.util.ByteBufferReaderUtil;
import de.rooehler.rastertheque.processing.RasterOp;
import de.rooehler.rastertheque.util.Hints;
import de.rooehler.rastertheque.util.Hints.Key;
import de.rooehler.rastertheque.util.ProgressListener;

public class OpenCVAmplitudeRescaler extends AmplitudeRescaler implements RasterOp, Serializable{


	private static final long serialVersionUID = -5961287990881266046L;
	
	private final static String TAG = OpenCVAmplitudeRescaler.class.getSimpleName();

	@Override
	public void execute(Raster raster, Map<Key, Serializable> params,Hints hints, ProgressListener listener) {
		
		
		double[] minMax = null;
		if(params != null){
			if(params.containsKey(KEY_MINMAX)){
				 minMax = (double[]) params.get(KEY_MINMAX);									
			}
		}
		
		final int raster_width  = raster.getDimension().right - raster.getDimension().left;
		final int raster_height = raster.getDimension().bottom - raster.getDimension().top;
		
		final int pixelAmount = raster_width * raster_height;
		
		if(minMax == null){
			final Mat srcMat = matAccordingToDatatype(
					raster.getBands().get(0).datatype(),
					raster.getData(),
					raster_width,
					raster_height);

			MinMaxLocResult result = Core.minMaxLoc(srcMat);
			minMax = new double[]{result.minVal, result.maxVal};
		}
		
		int[] pixels = new int[pixelAmount];
 
		final ByteBufferReader reader = new ByteBufferReader(raster.getData().array(), ByteOrder.nativeOrder());

    	Log.d(OpenCVAmplitudeRescaler.class.getSimpleName(), "rawdata min "+minMax[0] +" max "+minMax[1]);


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
	 * converts the bytes from a raster into an OpenCV Mat 
	 * having width * height cells of datatype according to the rasters datatype
	 * @param type the datatype of the raster
	 * @param bytes the data
	 * @param width the width of the raster
	 * @param height the height of the raster
	 * @return the Mat object containing the data in the given format
	 * @throws IOException
	 */
    // ////////////////////////////////////////////////////////////////////
    //
    // due to a bug in Android 5 (Lollipop) the native memory is aligned within 7 bytes 
	// and causes problems reading it direct in a certain format like as...Buffer()
	//
	// as workaround the buffer is read one by one
	//
	// TODO check this issue in future
    //
	// https://code.google.com/p/android/issues/detail?id=80064
	//
	// the workaround needs to be edited/removed for every datatype
	//
    // ////////////////////////////////////////////////////////////////////
	public Mat matAccordingToDatatype(DataType type, final ByteBuffer buffer, final int width, final int height) {
		
		//dataypes -> http://answers.opencv.org/question/5/how-to-get-and-modify-the-pixel-of-mat-in-java/
		final int size = height * width;
		
		switch(type){
		case BYTE:
			
			Mat byteMat = new Mat(height, width, CvType.CV_8S);
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
				
				byteMat.put(0, 0, Arrays.copyOfRange(buffer.array(),0, width * height));
			}else{

				byteMat.put(0, 0, buffer.array());

			}
			return byteMat;
			
		case CHAR:
				
			Mat charMat = new Mat(height, width, CvType.CV_16UC1);
			
			final char[] chars = new char[size];
			
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){

				ByteBufferReader reader = new ByteBufferReader(buffer.array(), ByteOrder.nativeOrder());
				for(int i = 0; i < size; i++){
					try {
						chars[i] = reader.readChar();
					} catch (IOException e) {
						Log.e(TAG, "error reading char");
					}
				}

			}else{
				
				buffer.asCharBuffer().get(chars);
				
			}
		    for(int i = 0; i < height;i++){
		    	for(int j = 0; j < width; j++){
		    		
		    		final char _char = chars[i * width + j];
		    		charMat.put(i,j,_char);
		    	}
		    }
			
			return charMat;
			
			
		case DOUBLE:
			
			Mat doubleMat = new Mat(height, width, CvType.CV_64FC1);
			
			final double[] doubles = new double[size];
		    
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
				
				ByteBufferReader reader = new ByteBufferReader(buffer.array(), ByteOrder.nativeOrder());
				for(int i = 0; i < size; i++){
					try {
						doubles[i] = reader.readDouble();
					} catch (IOException e) {
						Log.e(TAG, "error reading double");
					}
				}
				
			}else{
				
				buffer.asDoubleBuffer().get(doubles);
				
			}
			
		    doubleMat.put(0,0,doubles);
			
			return doubleMat;

		case FLOAT:
			
			Mat floatMat = new Mat(height, width, CvType.CV_32FC1);
						
			final float[] dst = new float[size];

			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
				
				ByteBufferReader reader = new ByteBufferReader(buffer.array(), ByteOrder.nativeOrder());
				for(int i = 0; i < size; i++){
					try {
						dst[i] = reader.readFloat();
					} catch (IOException e) {
						Log.e(TAG, "error reading float");
					}
				}
				
			}else{

				buffer.asFloatBuffer().get(dst);
			}

			
			floatMat.put(0,0,dst);
		    
			return floatMat;

		case INT:
			
			Mat intMat = new Mat(height, width, CvType.CV_32SC1);
			
			final int[] ints = new int[size];

			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){

				ByteBufferReader reader = new ByteBufferReader(buffer.array(), ByteOrder.nativeOrder());
				for(int i = 0; i < size; i++){
					try {
						ints[i] = reader.readInt();
					} catch (IOException e) {
						Log.e(TAG, "error reading int");
					}
				}

			}else{
				buffer.asIntBuffer().get(ints);
			}
		    intMat.put(0,0,ints);
			
			return intMat;
			
		case LONG:
			
			//use double for long as Mat does not have an appropriate data type			
			Mat longMat = new Mat(height, width, CvType.CV_64FC1);
			
			final double[] longs = new double[size];
			
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
				
				ByteBufferReader reader = new ByteBufferReader(buffer.array(), ByteOrder.nativeOrder());
				for(int i = 0; i < size; i++){
					try {
						longs[i] = reader.readLong();
					} catch (IOException e) {
						Log.e(TAG, "error reading long");
					}
				}
				
			}else{
				buffer.asDoubleBuffer().get(longs);
			}
		    longMat.put(0,0,longs);
			
			return longMat;
			
		case SHORT:
			
			Mat shortMat = new Mat(height, width, CvType.CV_16SC1);
			
			final short[] shorts = new short[size];
			
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){

				ByteBufferReader reader = new ByteBufferReader(buffer.array(), ByteOrder.nativeOrder());
				for(int i = 0; i < size; i++){
					try {
						shorts[i] = reader.readShort();
					} catch (IOException e) {
						Log.e(TAG, "error reading short");
					}
				}

			}else{
				buffer.asShortBuffer().get(shorts);
			}
		    shortMat.put(0,0,shorts);
			
			return shortMat;
			
		}
		throw new IllegalArgumentException("Invalid datatype");
	}
	
	@Override
	public Priority getPriority() {
	
		return Priority.HIGH;
	}
	


}
