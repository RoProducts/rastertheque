package de.rooehler.rastertheque.processing.resampling;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.os.Environment;
import android.util.Log;

import com.vividsolutions.jts.geom.Envelope;

import de.rooehler.rastertheque.core.DataType;
import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.io.mbtiles.MBTilesResampler;
import de.rooehler.rastertheque.processing.Interpolation.ResampleMethod;
import de.rooehler.rastertheque.processing.RasterOp;
import de.rooehler.rastertheque.processing.RasterOps;
import de.rooehler.rastertheque.util.Hints;
import de.rooehler.rastertheque.util.Hints.Key;
import de.rooehler.rastertheque.util.ProgressListener;

public class OpenCVResampler implements RasterOp, Serializable {
	
	private static final long serialVersionUID = -5251254282161549821L;

	static {
		if (!OpenCVLoader.initDebug()) {
			// Handle initialization error

			Log.e(MBTilesResampler.class.getSimpleName(), "error initialising OpenCV");
		} 
	}

	@Override
	public void execute(Raster raster,Map<Key,Serializable> params,Hints hints, ProgressListener listener) {
	
		Envelope dstDimension = null;
		if(params != null && params.containsKey(Hints.KEY_SIZE)){
			dstDimension = (Envelope) params.get(Hints.KEY_SIZE);
		}else{
			throw new IllegalArgumentException("no target dimension provided, cannot continue");
		}
		
		ResampleMethod method = ResampleMethod.BILINEAR;
		if(params != null && params.containsKey(Hints.KEY_INTERPOLATION)){
			method = (ResampleMethod) params.get(Hints.KEY_INTERPOLATION);
		}
		
		final int srcWidth = (int) raster.getDimension().getWidth();
		final int srcHeight = (int) raster.getDimension().getHeight();
		
		if(Double.compare(srcWidth,  dstDimension.getWidth()) == 0 &&
		   Double.compare(srcHeight, dstDimension.getHeight()) == 0){
			return;
		}
		
		final int dstWidth = (int) dstDimension.getWidth();
		final int dstHeight = (int) dstDimension.getHeight();
		
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
//		long now = System.currentTimeMillis();
		
		final Mat srcMat = matAccordingToDatatype(
					raster.getBands().get(0).datatype(),
					raster.getData(),
					(int) raster.getBoundingBox().getWidth(),
					(int) raster.getBoundingBox().getHeight());

//		Log.d(OpenCVResampler.class.getSimpleName(), "creating mat took : "+(System.currentTimeMillis() - now));

		Mat dstMat = new Mat();
		
		Imgproc.resize(srcMat, dstMat, new Size(dstWidth, dstHeight), 0, 0, i);
//		Log.d(OpenCVResampler.class.getSimpleName(), "resizing  took : "+(System.currentTimeMillis() - now));
		
		final int bufferSize = dstWidth * dstHeight * raster.getBands().size() * raster.getBands().get(0).datatype().size();
		
		raster.setDimension(dstDimension);
		
		raster.setData(bytesFromMat(
				dstMat,
				raster.getBands().get(0).datatype(),
				bufferSize));
//		Log.d(OpenCVResampler.class.getSimpleName(), "reconverting to bytes took : "+(System.currentTimeMillis() - now));
		
	}
	
	@Override
	public String getOperationName() {
		
		return RasterOps.RESIZE;
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
	public Mat matAccordingToDatatype(DataType type, final ByteBuffer buffer, final int width, final int height) {
		
		//dataypes -> http://answers.opencv.org/question/5/how-to-get-and-modify-the-pixel-of-mat-in-java/
		
		switch(type){
		case BYTE:
			
			Mat byteMat = new Mat(height, width, CvType.CV_8U);
			byteMat.put(0, 0, buffer.array());
			//for direct bytebuffer
//			byteMat.put(0, 0, Arrays.copyOfRange(buffer.array(),0, width * height));
			return byteMat;
			
		case CHAR:
			
			//Use short for chars
			//TODO test			
			Mat charMat = new Mat(height, width, CvType.CV_32SC1);
			
			final short[] chars = new short[height * width];
		    
		    buffer.asShortBuffer().get(chars);

		    charMat.put(0,0,chars);
			
			return charMat;
			
			
		case DOUBLE:
			
			Mat doubleMat = new Mat(height, width, CvType.CV_64FC1);
			
			final double[] doubles = new double[height * width];
		    
		    buffer.asDoubleBuffer().get(doubles);

		    doubleMat.put(0,0,doubles);
			
			return doubleMat;

		case FLOAT:
			
			Mat floatMat = new Mat(height, width, CvType.CV_32FC1);
						
			final float[] dst = new float[height * width];
			
		    buffer.asFloatBuffer().get(dst);

		    floatMat.put(0,0,dst);
		    
			return floatMat;

		case INT:
			
			Mat intMat = new Mat(height, width, CvType.CV_32SC1);
			
			final int[] ints = new int[height * width];
		    
		    buffer.asIntBuffer().get(ints);

		    intMat.put(0,0,ints);
			
			return intMat;
			
		case LONG:
			
			//use double for long as Mat does not have an appropriate data type
			//TODO test
			Mat longMat = new Mat(height, width, CvType.CV_64FC1);
			
			final double[] longs = new double[height * width];
		    
		    buffer.asDoubleBuffer().get(longs);

		    longMat.put(0,0,longs);
			
			return longMat;
			
		case SHORT:
			
			Mat shortMat = new Mat(height, width, CvType.CV_16SC1);
			
			final short[] shorts = new short[height * width];
		    
		    buffer.asShortBuffer().get(shorts);

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
		

		ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
		buffer.order(ByteOrder.nativeOrder());
			
		switch(type){
		case BYTE:
			mat.get(0, 0, buffer.array());
			
			break;
			
		case CHAR:
			
			//TODO test char
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
			
			//TODO test long
			//there seems to be no long support in OpenCV
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
