package de.rooehler.rastertheque.io.mbtiles;


import android.util.Log;

import com.vividsolutions.jts.geom.Coordinate;

import de.rooehler.rastertheque.processing.Interpolation.ResampleMethod;

public class MResampler {
	

	public void resample(int srcPixels[], int srcWidth, int srcHeight, int dstPixels[], int dstWidth, int dstHeight,final ResampleMethod method){
		
		if(srcWidth == dstWidth && srcHeight == dstHeight){
			System.arraycopy(srcPixels, 0, dstPixels, 0, srcPixels.length);
			return;
		}
		
		switch (method) {
		case NEARESTNEIGHBOUR:
			resampleNN(srcPixels, srcWidth, srcHeight, dstPixels, dstWidth, dstHeight);
			break;
		case BILINEAR:
			resampleBilinear(srcPixels, srcWidth, srcHeight, dstPixels, dstWidth, dstHeight);
			break;
		case BICUBIC:
			resampleBicubic(srcPixels, srcWidth, srcHeight, dstPixels, dstWidth, dstHeight);
			break;
		}
		
	}
	
	/**
	 * Bilinear interpolation http://en.wikipedia.org/wiki/Bilinear_interpolation
	 * 
	 * 
	 * @param srcPixels the source pixels
	 * @param srcWidth  width  of the src 
	 * @param srcHeight height of the src 
	 * @param dstPixels the pixels of the resampled image, allocated
	 * @param dstWidth the width of the resampled image
	 * @param dstHeight the height of the resampled image
	 */
	private void resampleBilinear(int srcPixels[], int srcWidth, int srcHeight, int dstPixels[], int dstWidth, int dstHeight){

		int a, b, c, d, x, y, index;
		float x_ratio = ((float) (srcWidth - 1)) / dstWidth;
		float y_ratio = ((float) (srcHeight - 1)) / dstHeight;
		float x_diff, y_diff, blue, red, green;
		int offset = 0;

		for (int i = 0; i < dstHeight; i++) {
			for (int j = 0; j < dstWidth; j++) {

				// src pix coords
				x = (int) (x_ratio * j);
				y = (int) (y_ratio * i);

				// offsets from the current pos to the pos in the new array
				x_diff = (x_ratio * j) - x;
				y_diff = (y_ratio * i) - y;

				// current pos
				index = y * srcWidth + x;

				a = srcPixels[index];
				b = srcPixels[index + 1];
				c = srcPixels[index + srcWidth];
				d = srcPixels[index + srcWidth + 1];

				// having the four pixels, interpolate

				// blue element
				// Yb = Ab(1-w)(1-h) + Bb(w)(1-h) + Cb(h)(1-w) + Db(wh)
				blue = (a & 0xff) * (1 - x_diff) * (1 - y_diff) + (b & 0xff) * (x_diff) * (1 - y_diff) + (c & 0xff)
						* (y_diff) * (1 - x_diff) + (d & 0xff) * (x_diff * y_diff);

				// green element
				// Yg = Ag(1-w)(1-h) + Bg(w)(1-h) + Cg(h)(1-w) + Dg(wh)
				green = ((a >> 8) & 0xff) * (1 - x_diff) * (1 - y_diff) + ((b >> 8) & 0xff) * (x_diff) * (1 - y_diff)
						+ ((c >> 8) & 0xff) * (y_diff) * (1 - x_diff) + ((d >> 8) & 0xff) * (x_diff * y_diff);

				// red element
				// Yr = Ar(1-w)(1-h) + Br(w)(1-h) + Cr(h)(1-w) + Dr(wh)
				red = ((a >> 16) & 0xff) * (1 - x_diff) * (1 - y_diff) + ((b >> 16) & 0xff) * (x_diff) * (1 - y_diff)
						+ ((c >> 16) & 0xff) * (y_diff) * (1 - x_diff) + ((d >> 16) & 0xff) * (x_diff * y_diff);

				dstPixels[offset++] = 0xff000000 | ((((int) red) << 16) & 0xff0000) | ((((int) green) << 8) & 0xff00)
						| ((int) blue);
			}
		}
	}
	
	/**
	 * resamples an array of pixels using bicubic interpolation
	 * 
	 * @param srcPixels the source pixels
	 * @param srcWidth  width  of the src 
	 * @param srcHeight height of the src 
	 * @param dstPixels the pixels of the resampled image, allocated
	 * @param dstWidth the width of the resampled image
	 * @param dstHeight the height of the resampled image
	 */
	private void resampleBicubic(int srcPixels[], int srcWidth, int srcHeight, int dstPixels[], int dstWidth, int dstHeight){

		float x_ratio = ((float) (srcWidth - 1)) / dstWidth;
		float y_ratio = ((float) (srcHeight - 1)) / dstHeight;
		int offset = 0;

		for (int i = 0; i < dstHeight; i++) {
			for (int j = 0; j < dstWidth; j++) {
				
				Coordinate c = new Coordinate(x_ratio * j,y_ratio * i);

				dstPixels[offset++] = getInterpolatedPixel(srcPixels, srcWidth, c , 0);
			}
		}
	}
	/**
	 * interpolates a pixel bicubic
	 * @reference Burger/Burge Digital Image Processing pp. 424
	 * 
	 * @param pixels the source pixel array
	 * @param srcWidth the width of the source image
	 * @param x0 the x coordinate to interpolate
	 * @param y0 the y coordinate to interpolate
	 * @param a  the guidance coefficient
	 * @return the interpolated pixel 
	 */
	private int getInterpolatedPixel(int[] pixels, int srcWidth, Coordinate coord, double a) {

		final double x = coord.x;
		final double y = coord.y;
		
		final int x0 = (int) Math.floor(x);	//use floor to handle negative coordinates too
		final int y0 = (int) Math.floor(y);

		double qR = 0, qB = 0, qG = 0;
		for (int j = 0; j < 4; j++) {
			final int v = y0 - 1 + j;
			double  pR = 0, pG = 0, pB = 0;
			
			for (int i = 0; i < 4; i++) {
				final int u = x0 - 1 + i;
				final int index = v * srcWidth + u;
				int pixel = 0;
				try{
					pixel = pixels[index];
				}catch(ArrayIndexOutOfBoundsException e){
					
					if( v < 0){
						if(u >= 0){
							pixel = pixels[u];
						}else{
							pixel = pixels[0];
						}
					}else if(u < 0){
						pixel = pixels[v * srcWidth];
					}else if(v >= srcWidth){
						if(u < srcWidth){
							pixel = pixels[srcWidth - 1 * srcWidth + u];							
						}else{
							pixel = pixels[srcWidth - 1 * srcWidth + (srcWidth - 1)];														
						}
					}else if(u >= srcWidth){
						pixel = pixels[v * srcWidth + (srcWidth - 1)];
					}else{
						Log.e("MResampler", "not handled " +v+" "+u);
					}
				}

			    pR = pR + ((pixel >> 16) & 0xff) * cubic(x - u, a);
				pG = pG + ((pixel >> 8)  & 0xff) * cubic(x - u, a);
				pB = pB + (pixel         & 0xff) * cubic(x - u, a);
			}

			qR = qR + pR * cubic(y - v, a);
			qG = qG + pG * cubic(y - v, a);
			qB = qB + pB * cubic(y - v, a);
		}

		return 0xff000000 | ((((int) qR) << 16) & 0xff0000) | ((((int) qG) << 8) & 0xff00)| ((int) qB);
	}
	
	private double cubic(double r, double a) {
		
		if (r < 0) {
			r = -r;
		}
		double w = 0;
		if (r < 1) {
			w = (a+2)*r*r*r - (a+3)*r*r + 1;
		}else if (r < 2) {			
			w = a*r*r*r - 5*a*r*r + 8*a*r - 4*a;
		}
		return w;
	}
	
	private void resampleNN(int[] srcPixels, int srcWidth, int srcHeight, int[] dstPixels, int dstWidth, int dstHeight) {

		int x, y, index;
		float x_ratio = ((float) (srcWidth - 1)) / dstWidth;
		float y_ratio = ((float) (srcHeight - 1)) / dstHeight;
		int offset = 0;

		for (int i = 0; i < dstHeight; i++) {
			for (int j = 0; j < dstWidth; j++) {

				
				// src pix coords
				x = (int) Math.rint(x_ratio * j);
				y = (int) Math.rint(y_ratio * i);

				// current pos
				index = y * srcWidth + x;

				dstPixels[offset++] = srcPixels[index];
				
			}
		}
		
	}

}
