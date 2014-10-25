package de.rooehler.rastertheque.util;

public class RasterProperty {
	
	private final int mRasterXSize;
	private final int mRasterYSize;
	private final double mXRes;
	private final double mYRes;

	public RasterProperty(final int pRasterXSize, final int pRasterYSize, final double pXRes, final double pYRes){
		
		this.mRasterXSize = pRasterXSize;
		this.mRasterYSize = pRasterYSize;
		this.mXRes = pXRes;
		this.mYRes = pYRes;
	}

	public int getmRasterXSize() {
		return mRasterXSize;
	}

	public int getmRasterYSize() {
		return mRasterYSize;
	}

	public double getmXRes() {
		return mXRes;
	}

	public double getmYRes() {
		return mYRes;
	}
	
	
}
