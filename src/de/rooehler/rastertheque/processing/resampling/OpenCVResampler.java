package de.rooehler.rastertheque.processing.resampling;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Map;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.graphics.Rect;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import de.rooehler.rastertheque.core.DataType;
import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.core.util.ByteBufferReader;
import de.rooehler.rastertheque.io.mbtiles.MBTilesResampler;
import de.rooehler.rastertheque.processing.Interpolation.ResampleMethod;
import de.rooehler.rastertheque.processing.RasterOp;
import de.rooehler.rastertheque.util.Hints;
import de.rooehler.rastertheque.util.Hints.Key;
import de.rooehler.rastertheque.util.ProgressListener;

public class OpenCVResampler extends Resampler implements RasterOp, Serializable {
	
	private static final long serialVersionUID = -5251254282161549821L;
	
	private final static String TAG = OpenCVResampler.class.getSimpleName();

	static {
		if (!OpenCVLoader.initDebug()) {
			// Handle initialization error

			Log.e(MBTilesResampler.class.getSimpleName(), "error initialising OpenCV");
		} 
	}

	@Override
	public void execute(Raster raster,Map<Key,Serializable> params,Hints hints, ProgressListener listener) {
	
		double scaleX = 0;
		double scaleY = 0;
		if(params != null && params.containsKey(KEY_SIZE)){
			Double[] factors = (Double[]) params.get(KEY_SIZE);
			scaleX = factors[0];
			scaleY = factors[1];
		}else{
			throw new IllegalArgumentException("no scale factors provided, cannot continue");
		}
		
		ResampleMethod method = ResampleMethod.BILINEAR;
		if(hints != null && hints.containsKey(Hints.KEY_INTERPOLATION)){
			method = (ResampleMethod) hints.get(Hints.KEY_INTERPOLATION);
		}
	
		final int srcWidth  = raster.getDimension().right - raster.getDimension().left;
		final int srcHeight = raster.getDimension().bottom - raster.getDimension().top;
				
		final int dstWidth = (int) (srcWidth * scaleX);
		final int dstHeight = (int) (srcHeight * scaleY);
		
		int i = 0;
		
		if(raster.getBands().get(0).datatype() == DataType.INT){
			//Due to a bug (?!) in OpenCV it is not possible to resize datatypes with depth of 4 bytes
			//with other interpolation methods than INTER_NEAREST
			//
			//source: https://github.com/Itseez/opencv/blob/2.4.10.x-prep/modules/imgproc/src/imgwarp.cpp
			//Line 2114 fails as the func for depth 4 is 0
			//using inter nearest this can be avoided

			i = Imgproc.INTER_NEAREST;
		}else{
			switch (method) {
			case NEARESTNEIGHBOUR:
				i = Imgproc.INTER_NEAREST;
				break;
			case BILINEAR:
				i = Imgproc.INTER_LINEAR;
				break;
			case BICUBIC:
				i = Imgproc.INTER_CUBIC;
				break;
			}	
		}

		final Mat srcMat = matAccordingToDatatype(
					raster.getBands().get(0).datatype(),
					raster.getData(),
					srcWidth,
					srcHeight,
					raster.getBands().size());

		Mat dstMat = new Mat();
		
		Imgproc.resize(srcMat, dstMat, new Size(dstWidth, dstHeight), 0, 0, i);
	
		final int newBufferSize = dstWidth * dstHeight * raster.getBands().size() * raster.getBands().get(0).datatype().size();
		
		raster.setDimension(new Rect(0, 0, dstWidth, dstHeight));
		
		raster.setData(bytesFromMat(
				dstMat,
				raster.getBands().get(0).datatype(),
				newBufferSize));

	}
	
	@Override
	public Priority getPriority() {
		
		return Priority.HIGHEST;
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
	public Mat matAccordingToDatatype(DataType type, final ByteBuffer buffer, final int width, final int height, final int bandCount) {
		
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
			//TODO test
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
	 * converts a Mat (with likely the result of some operation) 
	 * into a ByteBuffer according to the datatype
	 * @param mat the Mat to convert
	 * @param type the datatype of the data
	 * @param bufferSize the size of the ByteBuffer to create
	 * @return a ByteBuffer containing the data of the Mat
	 */
	public ByteBuffer bytesFromMat(Mat mat, DataType type, int bufferSize){

		//TODO use earlier buffer, do not allocate a new one

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
	


	
	@SuppressWarnings("unused")
	private static void testDirectRead(){
		
		String root = Environment.getExternalStorageDirectory().getAbsolutePath();
		File file = new File(root + "/rastertheque/HN+24_900913.tif");    
		File writefile = new File(root + "/rastertheque/dem_openCV.tif");    

		Mat m = Highgui.imread(file.getAbsolutePath());

		int depth = m.depth();
		int channels = m.channels();

		Mat dst = new Mat();

		Imgproc.resize(m, dst, new Size(),2,2, Imgproc.INTER_LINEAR);

		Highgui.imwrite(writefile.getAbsolutePath(), dst);
	    
	}

}
