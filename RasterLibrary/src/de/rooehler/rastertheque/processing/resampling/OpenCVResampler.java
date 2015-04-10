package de.rooehler.rastertheque.processing.resampling;

import java.io.Serializable;
import java.util.Map;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.graphics.Rect;
import android.util.Log;
import de.rooehler.rastertheque.core.DataType;
import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.processing.Interpolation.ResampleMethod;
import de.rooehler.rastertheque.processing.RasterOp;
import de.rooehler.rastertheque.util.Hints;
import de.rooehler.rastertheque.util.Hints.Key;
import de.rooehler.rastertheque.util.OpenCVConvert;
import de.rooehler.rastertheque.util.ProgressListener;
/**
 * Resampler which makes use of the OpenCV library
 * to resample raster in native code as implemented
 * by OpenCV
 * 
 * To achieve this functionality, raster objects need to be
 * converted to OpenCV's Mat object and vice-versa after 
 * the operation was executed
 * 
 * @author Robert Oehler
 *
 */
public class OpenCVResampler extends Resampler implements RasterOp, Serializable {
	
	private static final long serialVersionUID = -5251254282161549821L;

	//needs to initialize the OpenCV library when the class is loaded
	static {
		if (!OpenCVLoader.initDebug()) {
			Log.e(OpenCVResampler.class.getSimpleName(), "error initialising OpenCV");
		} 
	}

	@Override
	public void execute(Raster raster,Map<Key,Serializable> params,Hints hints, ProgressListener listener) {
	
		double scaleX = 0;
		double scaleY = 0;
		//assure scale factors
		if(params != null && params.containsKey(KEY_SIZE)){
			Double[] factors = (Double[]) params.get(KEY_SIZE);
			scaleX = factors[0];
			scaleY = factors[1];
		}else{
			throw new IllegalArgumentException("no scale factors provided, cannot continue");
		}
		//default interpolation
		ResampleMethod method = ResampleMethod.BILINEAR;
		//if hints contain another interpolation method, use it
		if(hints != null && hints.containsKey(Hints.KEY_INTERPOLATION)){
			method = (ResampleMethod) hints.get(Hints.KEY_INTERPOLATION);
		}
		//define src dimension
		final int srcWidth  = raster.getDimension().width();
		final int srcHeight = raster.getDimension().height();
		//define target dimension
		final int dstWidth = (int) Math.rint(srcWidth * scaleX);
		final int dstHeight = (int) Math.rint(srcHeight * scaleY);
		
		//select the interpolation method
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
			i = OpenCVConvert.getOpenCVInterpolation(method);	
		}
		//convert raster to srcMat
		final Mat srcMat = OpenCVConvert.matAccordingToDatatype(
					raster.getBands().get(0).datatype(),
					raster.getData(),
					srcWidth,
					srcHeight,
					raster.getBands().size());

		Mat dstMat = new Mat();
		//resize operation, resulting in the dstMat object
		Imgproc.resize(srcMat, dstMat, new Size(dstWidth, dstHeight), 0, 0, i);
	
		final int newBufferSize = dstWidth * dstHeight * raster.getBands().size() * raster.getBands().get(0).datatype().size();
		
		raster.setDimension(new Rect(0, 0, dstWidth, dstHeight));
		//convert dstMat back to a bytebuffer and set it as the rasters data
		raster.setData(OpenCVConvert.bytesFromMat(
				dstMat,
				raster.getBands().get(0).datatype(),
				newBufferSize));

	}
	
	@Override
	public Priority getPriority() {
		
		return Priority.HIGHEST;
	}
	


}
