package de.rooehler.rastertheque.processing.resampling;


import android.util.Log;
import de.rooehler.rastertheque.processing.Resampler;

public class MResampler extends Resampler {
	
	public MResampler(ResampleMethod method) {
		super(method);
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
	@Override
	public void resampleBilinear(int srcPixels[], int srcWidth, int srcHeight, int dstPixels[], int dstWidth, int dstHeight){
		
		Log.d(MResampler.class.getSimpleName(), "doing bilinear");
		if(srcWidth == dstWidth && srcHeight == dstHeight){
			System.arraycopy(srcPixels, 0, dstPixels, 0, srcPixels.length);
			return;
		}

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
	
	@Override
	public void resampleBicubic(int srcPixels[], int srcWidth, int srcHeight, int dstPixels[], int dstWidth, int dstHeight){
		
		Log.d(MResampler.class.getSimpleName(), "doing bicubic");
		if(srcWidth == dstWidth && srcHeight == dstHeight){
			System.arraycopy(srcPixels, 0, dstPixels, 0, srcPixels.length);
			return;
		}

		int  x, y;
		float x_diff, y_diff;
		float x_ratio = ((float) (srcWidth - 1)) / dstWidth;
		float y_ratio = ((float) (srcHeight - 1)) / dstHeight;
		int offset = 0;

		for (int i = 0; i < dstHeight; i++) {
			for (int j = 0; j < dstWidth; j++) {

				// src pix coords
				x = (int) (x_ratio * j);
				y = (int) (y_ratio * i);
				
				x_diff = (x_ratio * j) - x;
				y_diff = (y_ratio * i) - y;

				dstPixels[offset++] = getInterpolatedPixel(srcPixels, srcWidth, x + x_diff, y + y_diff, -1);
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
	private int getInterpolatedPixel(int[] pixels, int srcWidth, double x0, double y0, double a) {

		int u0 = (int) Math.floor(x0);	//use floor to handle negative coordinates too
		int v0 = (int) Math.floor(y0);

		double  qR = 0, qB = 0, qG = 0;
		for (int j = 0; j <= 3; j++) {
			int v = v0 - 1 + j;
			double  pR = 0, pG = 0, pB = 0;
			
			for (int i = 0; i <= 3; i++) {
				int u = u0 - 1 + i;
				int index = v * srcWidth + u;
				int pixel = 0;
				try{
					pixel = pixels[index];
				}catch(ArrayIndexOutOfBoundsException e){}
				

			    pR += ((pixel >> 16) & 0xff) * cubic(x0 - u, a);
				pG += ((pixel >> 8) & 0xff)  * cubic(x0 - u, a);
				pB += (pixel & 0xff) * cubic(x0 - u, a);
			}

			qR += pR * cubic(y0 - v, a);
			qG += pG * cubic(y0 - v, a);
			qB += pB * cubic(y0 - v, a);
		}
		return 0xff000000 | ((((int) qR) << 16) & 0xff0000) | ((((int) qG) << 8) & 0xff00)| ((int) qB);
	}
	
	private double cubic(double x, double a) {
		if (x < 0) x = -x;
		double z = 0;
		if (x < 1) 
			z = (-a+2)*x*x*x + (a-3)*x*x + 1;
		else if (x < 2) 
			z = -a*x*x*x + 5*a*x*x - 8*a*x + 4*a;
		return z;
	}

}
