package de.rooehler.rastertheque.processing.rendering;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import android.util.Log;
import de.rooehler.rastertheque.core.DataType;
import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.core.util.ByteBufferReader;
import de.rooehler.rastertheque.core.util.ByteBufferReaderUtil;
import de.rooehler.rastertheque.processing.ProgressListener;
import de.rooehler.rastertheque.processing.Renderer;
import de.rooehler.rastertheque.processing.RenderingHints;
import de.rooehler.rastertheque.processing.RenderingHints.Key;

public class OpenCVRenderer implements Renderer{

	@Override
	public int[] render(Raster raster, 
			Map <Key,Object> params,
			RenderingHints hints,
			ProgressListener listener) {
		
		Object rendering = null;
		if(hints != null && hints.containsKey(RenderingHints.KEY_SYMBOLIZATION)){
			
			rendering = params.get(RenderingHints.KEY_SYMBOLIZATION);
		}
		
		//TODO apply rendering object
		
		final Mat srcMat = matAccordingToDatatype(
				raster.getBands().get(0).datatype(),
				raster.getData(),
				(int) raster.getBoundingBox().getWidth(),
				(int) raster.getBoundingBox().getHeight());
		
		MinMaxLocResult result = Core.minMaxLoc(srcMat);
		
		final int pixelAmount = (int) raster.getDimension().getWidth() *  (int) raster.getDimension().getHeight();
		
		int[] pixels = new int[pixelAmount];

			 
		final ByteBufferReader reader = new ByteBufferReader(raster.getData().array(), ByteOrder.nativeOrder());

    	Log.d(OpenCVRenderer.class.getSimpleName(), "rawdata min "+result.minVal +" max "+result.maxVal);


    	for (int i = 0; i < pixelAmount; i++) {
        	
        	double d = ByteBufferReaderUtil.getValue(reader, raster.getBands().get(0).datatype());

    		pixels[i] = pixelValueForGrayScale(d, result.minVal, result.maxVal);

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
			
			Mat byteMat = new Mat(height, width, CvType.CV_8S);
			byteMat.put(0, 0, buffer.array());
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

}
