package de.rooehler.rastertheque.processing.resampling;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.os.Environment;
import android.util.Log;
import de.rooehler.rastertheque.core.DataType;
import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.core.util.ByteBufferReader;
import de.rooehler.rastertheque.processing.RawResampler;
import de.rooehler.rastertheque.processing.Resampler.ResampleMethod;

public class OpenCVRawResampler implements RawResampler {

	@Override
	public void resample(Raster raster, ResampleMethod method) {
	
		final int srcWidth = (int) raster.getBoundingBox().getWidth();
		final int srcHeight = (int) raster.getBoundingBox().getHeight();
		
		if(Double.compare(srcWidth,  raster.getDimension().getWidth()) == 0 &&
		   Double.compare(srcHeight, raster.getDimension().getHeight()) == 0){
			return;
		}
		
		int i = 0;
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
		long now = System.currentTimeMillis();
		
		Mat srcMat = null;
		try {
			srcMat = matAccordingToDatatype(
					raster.getBands().get(0).datatype(),
					raster.getData().array(),
					(int) raster.getBoundingBox().getWidth(),
					(int) raster.getBoundingBox().getHeight());
		} catch (IOException e) {
			Log.e(OpenCVRawResampler.class.getSimpleName(), "Error creating mat from raster",e);
		}
		Log.d(OpenCVRawResampler.class.getSimpleName(), "creating mat took : "+(System.currentTimeMillis() - now));

		Mat dstMat = new Mat();
		
		Imgproc.resize(srcMat, dstMat, new Size(raster.getDimension().getWidth(), raster.getDimension().getHeight()), 0, 0, i);
		Log.d(OpenCVRawResampler.class.getSimpleName(), "resizing  took : "+(System.currentTimeMillis() - now));
		
		final int bufferSize = ((int)raster.getDimension().getWidth()) * ((int)raster.getDimension().getHeight()) * raster.getBands().size() * raster.getBands().get(0).datatype().size();
		
		raster.setData(bytesFromMat(
				dstMat,
				raster.getBands().get(0).datatype(),
				bufferSize));
		Log.d(OpenCVRawResampler.class.getSimpleName(), "reconverting to bytes took : "+(System.currentTimeMillis() - now));
		
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
	public Mat matAccordingToDatatype( DataType type, final byte[] bytes, final int width, final int height) throws IOException{
		
		//dataypes -> http://answers.opencv.org/question/5/how-to-get-and-modify-the-pixel-of-mat-in-java/
		
		switch(type){
		case BYTE:
			
			Mat byteMat = new Mat(height, width, CvType.CV_8U);
			byteMat.put(0, 0, Arrays.copyOfRange(bytes,0, width * height));
			return byteMat;
			
		case CHAR:
			
			Mat charMat = new Mat(height, width, CvType.CV_16UC1);
			
			ByteBufferReader charReader = new ByteBufferReader(bytes, ByteOrder.nativeOrder());

			for(int y = 0; y < height; y++){
				for(int x = 0; x < width ; x++){
					charMat.put(y, x, charReader.readChar());
				}
			}
			
			return charMat;
			
		case DOUBLE:
			
			Mat doubleMat = new Mat(height, width, CvType.CV_64FC1);
			
			ByteBufferReader doubleReader = new ByteBufferReader(bytes, ByteOrder.nativeOrder());

			for(int y = 0; y < height; y++){
				for(int x = 0; x < width ; x++){
					doubleMat.put(y, x, doubleReader.readDouble());
				}
			}
			
			return doubleMat;

		case FLOAT:
			
			Mat floatMat = new Mat(height, width, CvType.CV_32FC1);
			
			ByteBufferReader floatReader = new ByteBufferReader(bytes, ByteOrder.nativeOrder());

			for(int y = 0; y < height; y++){
				for(int x = 0; x < width ; x++){
					float f = floatReader.readFloat();
					floatMat.put(y, x, f);
				}
			}
			
			return floatMat;

		case INT:
			
			Mat intMat = new Mat(height, width, CvType.CV_32SC1);
			
			ByteBufferReader intReader = new ByteBufferReader(bytes, ByteOrder.nativeOrder());

			for(int y = 0; y < height; y++){
				for(int x = 0; x < width ; x++){
					intMat.put(y, x, intReader.readInt());
				}
			}
			
			return intMat;
			
		case LONG:
			
			Mat longMat = new Mat(height, width, CvType.CV_32SC2);
			
			ByteBufferReader longReader = new ByteBufferReader(bytes, ByteOrder.nativeOrder());

			for(int y = 0; y < height; y++){
				for(int x = 0; x < width ; x++){
					longMat.put(y, x, longReader.readLong());
				}
			}
			
			return longMat;
			
		case SHORT:
			
			Mat shortMat = new Mat(height, width, CvType.CV_16SC1);
			
			ByteBufferReader shortReader = new ByteBufferReader(bytes, ByteOrder.nativeOrder());

			for(int y = 0; y < height; y++){
				for(int x = 0; x < width ; x++){
					shortMat.put(y, x, shortReader.readShort());
				}
			}
			
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
		

		ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
		buffer.order(ByteOrder.nativeOrder());
		final int height = (int) mat.size().height;
		final int width = (int) mat.size().width;
		
		switch(type){
		case BYTE:
			mat.get(0, 0, buffer.array());
			return buffer;
			
		case CHAR:
			for(int y = 0; y < height; y++){
				for(int x = 0; x < width ; x++){
					
					double[] doh = mat.get(y,x);
					buffer.putChar( (char) doh[0]);
				}
			}
			break;
		case DOUBLE:
			
			for(int y = 0; y < height; y++){
				for(int x = 0; x < width ; x++){
					
					double[] doh = mat.get(y,x);
					buffer.putDouble(doh[0]);
				}
			}

			break;
		case FLOAT:

			for(int y = 0; y < height; y++){
				for(int x = 0; x < width ; x++){
					
					double[] doh = mat.get(y,x);
					buffer.putFloat((float) doh[0]);
				}
			}

			break;

		case INT:
			for(int y = 0; y < height; y++){
				for(int x = 0; x < width ; x++){
					
					double[] doh = mat.get(y,x);
					int i = (int) doh[0];
					buffer.putInt(i);
				}
			}
			
			break;
			
		case LONG:
			for(int y = 0; y < height; y++){
				for(int x = 0; x < width ; x++){
					
					double[] doh = mat.get(y,x);
					buffer.putLong((long)doh[0]);
				}
			}
			break;
			
		case SHORT:
			for(int y = 0; y < height; y++){
				for(int x = 0; x < width ; x++){
					
					double[] doh = mat.get(y,x);
					buffer.putShort((short)doh[0]);
				}
			}
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
