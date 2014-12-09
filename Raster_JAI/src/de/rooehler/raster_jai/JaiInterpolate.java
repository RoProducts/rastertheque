package de.rooehler.raster_jai;

import javax.media.jai.InterpolationBilinear;

public class JaiInterpolate {


	public static void interpolate2D(int[] srcPixels, int srcSize, int[]dstPixels, int dstSize){


		//		BufferedImage srcBI = new BufferedImage(srcSize, srcSize, BufferedImage.TYPE_INT_ARGB);
		//		
		//		final int[] a = ( (DataBufferInt) srcBI.getRaster().getDataBuffer() ).getData();
		//		System.arraycopy(srcPixels, 0, a, 0, srcPixels.length);
		//		
		//		final float scaleFactor = dstSize / srcSize;
		//		
		//		RenderedOp rescaled = ScaleDescriptor.create(srcBI,
		//				scaleFactor, scaleFactor, //scale
		//                new Float(0.0f), new Float(0.0f), //translate
		//                Interpolation.getInstance(Interpolation.INTERP_BILINEAR),
		//                null);//hints
		//
		//				
		//		BufferedImage bi = rescaled.getAsBufferedImage();
		//		
		//		dstPixels = ((DataBufferInt) bi.getRaster().getDataBuffer()).getData();

		InterpolationBilinear ib = new InterpolationBilinear();

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

				red = ib.interpolate(
						((a >> 16) & 0xff),
						((b >> 16) & 0xff), 
						((c >> 16) & 0xff),
						((d >> 16) & 0xff),
						getSubSampleBits(x_diff), getSubSampleBits(y_diff));
				green = ib.interpolate(
						((a >> 8) & 0xff),
						((b >> 8) & 0xff),
						((c >> 8) & 0xff),
						((d >> 8) & 0xff),
						getSubSampleBits(x_diff), getSubSampleBits(y_diff));
				blue = ib.interpolate(
						(a & 0xff),
						(b & 0xff),
						(c & 0xff),
						(d & 0xff),
						getSubSampleBits(x_diff), getSubSampleBits(y_diff));

				dstPixels[offset++] = 0xff000000 |
						((((int) red) << 16) & 0xff0000) |
						((((int) green) << 8) & 0xff00)	 |
						((int) blue);
			}
		}

	}

	public  static int getSubSampleBits(float ratio){

		return (int) (256 * ratio);
	}

}
