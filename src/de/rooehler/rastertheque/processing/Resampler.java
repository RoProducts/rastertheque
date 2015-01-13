package de.rooehler.rastertheque.processing;

public abstract class Resampler {
	
	public enum ResampleMethod
	{
		BILINEAR,
		BICUBIC;
	}
	
	protected ResampleMethod mResampleMethod;
	
	protected abstract void resampleBilinear(int srcPixels[], int srcWidth, int srcHeight, int dstPixels[], int dstWidth, int dstHeight);
	
	protected abstract void resampleBicubic(int srcPixels[], int srcWidth, int srcHeight, int dstPixels[], int dstWidth, int dstHeight);
	
	
	public Resampler(ResampleMethod method){
		
		this.mResampleMethod = method;
	}
	
	
	public void resample(int[] srcPixels, int srcWidth, int srcHeight, int[] dstPixels, int dstWidth, int dstHeight) {
	
		switch(mResampleMethod){
		
		case BICUBIC:
			resampleBicubic(srcPixels, srcWidth, srcHeight, dstPixels, dstWidth, dstHeight);
			break;
		case BILINEAR:
			resampleBilinear(srcPixels, srcWidth, srcHeight, dstPixels, dstWidth, dstHeight);
			break;
		}
		
	}
	
	public void setResamplingMethod(final ResampleMethod pMethod){
		
		this.mResampleMethod = pMethod;
				
	}

	
}
