package de.rooehler.rastertheque.processing.resampling;

import java.nio.ByteBuffer;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import de.rooehler.rastertheque.core.DataType;
import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.processing.RawResampler;
import de.rooehler.rastertheque.processing.Resampler.ResampleMethod;

public class OpenCVRawResampler implements RawResampler {

	@Override
	public void resample(Raster raster, ResampleMethod method) {
	
		final int srcWidth = (int) raster.getBoundingBox().getWidth();
		final int srcHeight = (int) raster.getBoundingBox().getHeight();
		
		if(Double.compare(srcWidth, raster.getDimension().getWidth()) == 0 ||
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
		
		
		Mat srcMat = new Mat(srcHeight,srcWidth,datatypeToOpenCVDatatype(raster.getBands().get(0).datatype()));
		
//		for(int y = 0; y < srcHeight; y++){
//			for(int x = 0; x < srcWidth; x++){
//			
//				srcMat.put(y, x, raster.getData().array()[y * srcWidth + x ]);
//			}
//		}
		srcMat.put(0, 0, raster.getData().array());
		
		Mat dstMat = new Mat();
		
		Imgproc.resize(srcMat, dstMat, new Size(raster.getDimension().getWidth(), raster.getDimension().getHeight()), 0, 0, i);
		
		int bufferSize = ((int)raster.getDimension().getWidth()) * ((int)raster.getDimension().getHeight()) * raster.getBands().size() * raster.getBands().get(0).datatype().size();
		
		byte[] bytes = new byte[bufferSize];
		
		dstMat.get(0, 0, bytes);
		
		raster.setData(ByteBuffer.wrap(bytes));
		
	}
	
	public int datatypeToOpenCVDatatype(DataType type){
		
		switch(type){
		case BYTE:
			return CvType.CV_8UC1;
			
		case CHAR:
			return CvType.CV_16SC1;
			
		case DOUBLE:
			return CvType.CV_64FC1;

		case FLOAT:
			return CvType.CV_32FC1;

		case INT:
			return CvType.CV_32SC1;
			
		case LONG:
			return CvType.CV_32SC2;
			
		case SHORT:
			return CvType.CV_16SC1;
			
		}
		return -1;
	}

}
