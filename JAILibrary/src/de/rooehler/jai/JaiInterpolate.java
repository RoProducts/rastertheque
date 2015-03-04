package de.rooehler.jai;

import javax.media.jai.Interpolation;
/**
 * class which connects to the JAI library 
 * to execute interpolations on a very basic (neighbourhood) level
 * As this does not make use of the Java awt package
 * it is also usable under Android
 * 
 * Generally this applies an interpolation method to a neighbourhood 
 * of values in the form
 * 
 *                    s00    s01                                     
                                                                      
                          .      < yfrac                      
                                                                      
                      s10    s11                                     
                          ^                                           
                         xfrac  
 * 
 * The values of x_diff and y_diff must lie between 0.0 and 1.0 exclusive.
 * For integer interpolation they are converted to subsampleBits.
 * 
 * See 
 * 
 * http://docs.oracle.com/cd/E17802_01/products/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/Interpolation.html
 * 
 * for a more profound introduction
 * 
 * @author Robert Oehler
 *
 */
public class JaiInterpolate {

	public static double interpolateRawDoubles(double[] values, float x_diff, float y_diff, int _interpolation){
		
		return getInterpolation(_interpolation).interpolate(values[0],values[1],values[2],values[3], x_diff, y_diff);
		
	}
	
	public static float interpolateRawFloats(float[] values, float x_diff, float y_diff, int _interpolation){
		
		return getInterpolation(_interpolation).interpolate(values[0],values[1],values[2],values[3], x_diff, y_diff);
	}
	
	public static int interpolateRawInts(int[] values, float x_diff, float y_diff, int _interpolation){
		
		
		Interpolation interpolation = getInterpolation(_interpolation);

		return interpolation.interpolate(
				values[0],
				values[1],
				values[2],
				values[3],
				calcSubSampleFrac(interpolation.getSubsampleBitsH(),x_diff), 
				calcSubSampleFrac(interpolation.getSubsampleBitsV(),y_diff));

	}
	
	private static Interpolation getInterpolation(int _interpolation){
		
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
		default:
			interpolation = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
		}
		return interpolation;
	}

	private static int calcSubSampleFrac(int subsampleBits, float ratio){

		return (int) (Math.pow(2 , subsampleBits) * ratio);
	}
}
