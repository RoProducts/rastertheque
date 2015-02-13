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


		return interpolation.interpolate(
				values[0],
				values[1],
				values[2],
				values[3],
				calcSubSampleFrac(interpolation.getSubsampleBitsH(),x_diff), 
				calcSubSampleFrac(interpolation.getSubsampleBitsV(),y_diff));

	}

	private static int calcSubSampleFrac(int subsampleBits, float ratio){

		return (int) (Math.pow(2 , subsampleBits) * ratio);
	}
}
