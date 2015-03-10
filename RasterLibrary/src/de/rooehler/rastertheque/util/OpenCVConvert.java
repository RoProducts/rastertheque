package de.rooehler.rastertheque.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import android.os.Build;
import android.util.Log;
import de.rooehler.rastertheque.core.DataType;
import de.rooehler.rastertheque.core.util.ByteBufferReader;
import de.rooehler.rastertheque.processing.Interpolation.ResampleMethod;

public class OpenCVConvert {
	
	private final static String TAG = OpenCVConvert.class.getSimpleName();
	
	/**
	 * converts the bytes from a raster into an OpenCV Mat 
	 * having width * height cells of datatype according to the rasters datatype
	 * @param type the datatype of the raster
	 * @param bytes the data
	 * @param width the width of the raster
	 * @param height the height of the raster
	 * @return the Mat object containing the data in the given format
	 * @throws IOException if an error occured reading from the ByteBufferedReader which is used for some data type
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
	public static Mat matAccordingToDatatype(DataType type, final ByteBuffer buffer, final int width, final int height, final int bandCount) {
		
		//dataypes -> http://answers.opencv.org/question/5/how-to-get-and-modify-the-pixel-of-mat-in-java/
		final int size = height * width;
		
		switch(type){
		case BYTE:
			
			Mat byteMat = new Mat(height, width, CvType.CV_8UC(bandCount));

			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
				
				byteMat.put(0, 0, Arrays.copyOfRange(buffer.array(),0, width * height * bandCount));
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
					
			Mat intMat = new Mat(height, width, CvType.CV_32SC(bandCount));
			
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
	/**
	 * converts a Mat (with likely the result of the resample operation) 
	 * into a ByteBuffer according to the datatype
	 * @param mat the Mat to convert
	 * @param type the datatype of the data
	 * @param bufferSize the size of the ByteBuffer to create
	 * @return a ByteBuffer containing the data of the Mat
	 */
	public static ByteBuffer bytesFromMat(Mat mat, DataType type, int bufferSize){

		//TODO when the direct bytebuffer issed is resolved,
		//use the earlier buffer here, do not allocate a new one

		ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
		buffer.order(ByteOrder.nativeOrder());
			
		switch(type){
		case BYTE:
			mat.get(0, 0, buffer.array());
			
			break;
			
		case CHAR:
			
			final short[] chars = new short[bufferSize / 2];
			mat.get(0, 0, chars);
			buffer.asShortBuffer().put(chars);
						
			break;
		case DOUBLE:
			
			final double[] doubles = new double[bufferSize / 8];
			mat.get(0, 0, doubles);
			buffer.asDoubleBuffer().put(doubles);

			break;
		case FLOAT:

			final float[] dst = new float[bufferSize / 4];
			mat.get(0, 0, dst);
			buffer.asFloatBuffer().put(dst);
			
			break;

		case INT:
			
			final int[] ints = new int[bufferSize / 4];
			mat.get(0, 0, ints);
			buffer.asIntBuffer().put(ints);
						
			break;
			
		case LONG:
			//wrap longs with double
			final double[] longs = new double[bufferSize / 8];
			mat.get(0, 0, longs);
			buffer.asDoubleBuffer().put(longs);
			
			break;
			
		case SHORT:
			
			final short[] shorts = new short[bufferSize / 2];
			mat.get(0, 0, shorts);
			buffer.asShortBuffer().put(shorts);
			
			break;			
		}
		
		return buffer;
	}

	public static int getOpenCVInterpolation(final ResampleMethod method){
		
		switch (method) {
		case NEARESTNEIGHBOUR:
			return Imgproc.INTER_NEAREST;
		case BILINEAR:
			return Imgproc.INTER_LINEAR;
		case BICUBIC:
			return Imgproc.INTER_CUBIC;
		default:
			return Imgproc.INTER_LINEAR;
		}	
	}
}
