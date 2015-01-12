package de.rooehler.rastertheque.processing.resampling;

import de.rooehler.rastertheque.processing.Resampling;

public class MBilinearInterpolator  implements Resampling{
	
	/**
	 * Bilinear interpolation http://en.wikipedia.org/wiki/Bilinear_interpolation
	 * 
	 * Must be a quadratic image having with the Dimension ( srcSize , srcSize )
	 * 
	 * @param srcPixels the source pixels
	 * @param srcSize  width / height of the src 
	 * @param dstPixels the pixels of the resample image, allocated
	 * @param dstSize the width/height of the resampled image
	 */
	@Override
	public void resampleBilinear(int srcPixels[], int srcSize, int dstPixels[], int dstSize) {

		int a, b, c, d, x, y, index;
		float x_ratio = ((float) (srcSize - 1)) / dstSize;
		float y_ratio = ((float) (srcSize - 1)) / dstSize;
		float x_diff, y_diff, blue, red, green;
		int offset = 0;

		for (int i = 0; i < dstSize; i++) {
			for (int j = 0; j < dstSize; j++) {

				// src pix coords
				x = (int) (x_ratio * j);
				y = (int) (y_ratio * i);

				// offsets from the current pos to the pos in the new array
				x_diff = (x_ratio * j) - x;
				y_diff = (y_ratio * i) - y;

				// current pos
				index = (y * srcSize + x);

				a = srcPixels[index];
				b = srcPixels[index + 1];
				c = srcPixels[index + srcSize];
				d = srcPixels[index + srcSize + 1];

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

}
