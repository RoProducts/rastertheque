package de.rooehler.raster_jai;

import javax.media.jai.Interpolation;

public class JaiInterpolate {

	public static double interpolateRawDoubles(double[] values, float x_diff, float y_diff, int _interpolation){
		
		Interpolation interpolation = null;
		switch (_interpolation) {
		case 0:
			interpolation = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
			break;
		case 1:
			interpolation = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
			break;
		case 2:
			interpolation = Interpolation.getInstance(Interpolation.INTERP_BICUBIC);
			break;
		}
		
		return interpolation.interpolate(values[0],values[1],values[2],values[3],x_diff, y_diff);
		
	}
	public static float interpolateRawFloats(float[] values, float x_diff, float y_diff, int _interpolation){
		
		Interpolation interpolation = null;
		switch (_interpolation) {
		case 0:
			interpolation = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
			break;
		case 1:
			interpolation = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
			break;
		case 2:
			interpolation = Interpolation.getInstance(Interpolation.INTERP_BICUBIC);
			break;
		}
		
		return interpolation.interpolate(values[0],values[1],values[2],values[3], x_diff, y_diff);
	}
	public static int interpolateRawInts(int[] values, float x_diff, float y_diff, int _interpolation){
		
		Interpolation interpolation = null;
		switch (_interpolation) {
		case 0:
			interpolation = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
			break;
		case 1:
			interpolation = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
			break;
		case 2:
			interpolation = Interpolation.getInstance(Interpolation.INTERP_BICUBIC);
			break;
		}


		return interpolation.interpolate(values[0],values[1],values[2],values[3],getSubSampleBits(x_diff), getSubSampleBits(y_diff));

	}

	public static void interpolate(int srcPixels[], int srcWidth, int srcHeight, int dstPixels[], int dstWidth, int dstHeight, final Interpolation ib){


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
		
//		InterpolationBilinear ib = new InterpolationBilinear();

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
				index = (y * srcWidth + x);

				a = srcPixels[index];
				b = srcPixels[index + 1];
				c = srcPixels[index + srcWidth];
				d = srcPixels[index + srcWidth + 1];

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
		


//		ParameterBlockJAI obj = new ParameterBlockJAI("Scale");

	}

	public static int getSubSampleBits(float ratio){

//		return (int) (256 * ratio);
		return (int) Math.pow(2 ,ratio);
	}
}
