package de.rooehler.rastertheque.processing.reprojecting;

import java.io.Serializable;
import java.util.Map;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.osgeo.proj4j.CoordinateReferenceSystem;

import android.util.Log;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.util.AffineTransformation;
import com.vividsolutions.jts.geom.util.AffineTransformationBuilder;

import de.rooehler.rastertheque.core.DataType;
import de.rooehler.rastertheque.core.Raster;
import de.rooehler.rastertheque.core.util.ReferencedEnvelope;
import de.rooehler.rastertheque.processing.Interpolation.ResampleMethod;
import de.rooehler.rastertheque.processing.RasterOp;
import de.rooehler.rastertheque.proj.Proj;
import de.rooehler.rastertheque.util.Hints;
import de.rooehler.rastertheque.util.Hints.Key;
import de.rooehler.rastertheque.util.OpenCVConvert;
import de.rooehler.rastertheque.util.ProgressListener;
/**
 * This implementation of the Reproject operation
 * using the OpenCV library 
 * 
 * is not working yet.
 * The question is: are all transformation affine
 * i.e. can affine transformation be used for reprojections?
 * 
 * @TODO to complete/test
 * 
 * @author Robert Oehler
 *
 */
public class OpenCVReproject extends Reproject implements RasterOp  {

	@Override
	public Priority getPriority() {
		return Priority.LOW;
	}

	@Override
	public void execute(Raster raster, Map<Key, Serializable> params, Hints hints, ProgressListener listener) {


		// src projection		
		CoordinateReferenceSystem src_crs = raster.getCRS();
		// target projection
		CoordinateReferenceSystem dst_crs = null;

		if(params != null && params.containsKey(Reproject.KEY_REPROJECT_TARGET_CRS)){
			String wkt = (String) params.get(Reproject.KEY_REPROJECT_TARGET_CRS);
			//if this is a proj parameter string convert to wkt
			if(wkt != null && wkt.startsWith("+proj")){
				wkt = Proj.proj2wkt(wkt);
			}
			//try to create a CoordinateReferenceSystem from it
			if(wkt!= null){				
				try{
					dst_crs = Proj.crs(wkt);
				}catch(RuntimeException e){
					Log.e(Reproject.class.getSimpleName(), "error parsing target projection String "+wkt);
					return;
				}
			}else{
				Log.e(MReproject.class.getSimpleName(), "no proj params String provided as dst crs parameter");
				return;
			}

		}else if(params == null){	
			Log.e(MReproject.class.getSimpleName(), "no params provided");
			return;
		}else if(!params.containsKey(Reproject.KEY_REPROJECT_TARGET_CRS)){
			Log.e(MReproject.class.getSimpleName(), "no parameter for the target crs provided");
			return;
		}
		if(src_crs == null){
			Log.e(MReproject.class.getSimpleName(), "src raster does not have a crs, cannot reproject");
			return;	
		}
		if(dst_crs == null){		
			Log.e(MReproject.class.getSimpleName(), "invalid well-known text provided as dst crs parameter");
			return;
		}

		ResampleMethod method = ResampleMethod.BILINEAR;
		if(hints != null && hints.containsKey(Hints.KEY_INTERPOLATION)){
			method = (ResampleMethod) hints.get(Hints.KEY_INTERPOLATION);
		}
		
		final String src_wkt = Proj.proj2wkt(src_crs.getParameterString());
		final String dst_wkt = Proj.proj2wkt(dst_crs.getParameterString());
		
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

		final DataType dataType = raster.getBands().get(0).datatype();
		final int srcWidth  = raster.getDimension().width();
		final int srcHeight = raster.getDimension().height();

		//source envelope
		ReferencedEnvelope	src_refEnv = new ReferencedEnvelope(raster.getBoundingBox(), src_crs);

		//transform the src envelope to the target envelope using the target crs
		//densify it with 10 additional points
		ReferencedEnvelope reprojected = src_refEnv.transform(dst_crs, 10);
		
//		double sx = 2.0;//: scale factor in x direction
//		double sy = 2.0;// scale factor in y direction
//		double tx = 1.0;//: offset in x direction
//		double ty = 1.0;//: offset in y direction
//		double delta = 0.0;//θ : angle of rotation clockwise around origin
//		double kx = 1.0;//: shearing parallel to x axis
//		double ky = 1.0;//: shearing parallel to y axis
		
		
//		double a11 = sx * ( (1 + kx * ky) * Math.cos(delta) + ky * Math.sin(delta));
//	    double a12 = sx * ( kx * Math.cos(delta) + Math.sin(delta) );
//	    double a13 = tx;
//	    double a21 = sy * ( -(1 + kx * ky) * Math.sin(delta) + ky * Math.cos(delta) );
//	    double a22 = sy * ( - kx * Math.sin(delta) + Math.cos(delta) );
//	    double a23 = ty;
		
//		double d = Math.toRadians(45.0);
//		
//		double[] matrix = 
//				new double[]{ Math.cos(d) , Math.sin(d) , 0.0,
//							 -Math.sin(d) , Math.cos(d) , 0.0
//							 };
		
//		Envelope src = GDALDataset.convertToLatLon(raster.getBoundingBox(), src_wkt);
//		Envelope dst = GDALDataset.convertToLatLon(reprojected.getEnvelope(), dst_wkt);
		
		Envelope src = raster.getBoundingBox();
		Envelope dst = reprojected.getEnvelope();
		

		Coordinate r1_src = new Coordinate(0                 ,  (srcHeight -1) / (double) 2);
		Coordinate r2_src = new Coordinate(srcWidth - 1      ,  (srcHeight -1)  / (double)  2);
		Coordinate r3_src = new Coordinate((srcWidth -1)  / (double)  2,  srcHeight - 1);
		
		Coordinate w1_src = new Coordinate(src.getMinX(),  src.centre().y);
		Coordinate w2_src = new Coordinate(src.getMaxX(),  src.centre().y);
		Coordinate w3_src = new Coordinate(src.centre().x, src.getMaxY());
		
		//2.src raster -> src world
		AffineTransformationBuilder src_r_src_w = new AffineTransformationBuilder(
				r1_src, r2_src, r3_src,
				w1_src, w2_src, w3_src);
		
		AffineTransformation src_raster_2_world = src_r_src_w.getTransformation();
		
		
		//2.src world -> dst world
		
		Coordinate w1_dst = new Coordinate(dst.getMinX(),  dst.centre().y);
		Coordinate w2_dst = new Coordinate(dst.getMaxX(),  dst.centre().y);
		Coordinate w3_dst = new Coordinate(dst.centre().x, dst.getMaxY());
	    
		AffineTransformationBuilder src_2_dst_w = new AffineTransformationBuilder(
				w1_src, w2_src, w3_src,
				w1_dst, w2_dst, w3_dst);
		
		AffineTransformation src_dst = src_2_dst_w.getTransformation();
		
		//3.dst world -> dst raster
		
		AffineTransformationBuilder dst_w_dst_r = new AffineTransformationBuilder(
				w1_dst, w2_dst, w3_dst,
				r1_src, r2_src, r3_src);
		
		AffineTransformation dst_world_2_raster = dst_w_dst_r.getTransformation();
		
		//forwards ?
//		src_raster_2_world.compose(src_dst).compose(dst_world_2_raster);
		//backwards ?
		dst_world_2_raster.composeBefore(src_dst).composeBefore(src_raster_2_world);
		
		double[] m = dst_world_2_raster.getMatrixEntries();
		
		for(int j = 0; j < 2; j++){
			Log.d("OpenCVReproject", String.format("[%f,%f,%f]",m[j*3],m[j*3+1],m[j*3+2] ));
		}
		

		//convert raster to srcMat
		final Mat srcMat = OpenCVConvert.matAccordingToDatatype(
							raster.getBands().get(0).datatype(),
							raster.getData(),
							srcWidth,
							srcHeight,
							raster.getBands().size());
		
		Mat affineTransMat = new Mat(2, 3, CvType.CV_64FC1);
		affineTransMat.put(0, 0, m);
		
		Mat dstMat = new Mat(srcMat.rows(),srcMat.cols(),srcMat.type());
		//Apply
		Imgproc.warpAffine(srcMat, dstMat, affineTransMat, srcMat.size(),i);
		
		//convert back
		raster.setData(OpenCVConvert.bytesFromMat(
				dstMat,
				dataType,
				srcWidth * srcHeight * raster.getBands().size() * dataType.size()));

	}

}
