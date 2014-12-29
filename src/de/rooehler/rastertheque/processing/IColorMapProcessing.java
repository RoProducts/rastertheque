package de.rooehler.rastertheque.processing;

import java.nio.ByteBuffer;

import de.rooehler.rastertheque.core.DataType;

public interface IColorMapProcessing {
	
	public boolean hasColorMap();
	
	public int[] generateGrayScalePixelsCalculatingMinMax(final ByteBuffer pBuffer, final int bufferSize, final DataType dataType);
	
	public int[] generatePixelsWithColorMap(final ByteBuffer pBuffer, final int bufferSize, final DataType dataType);
	
	public int[] generateThreeBandedRGBPixels(final ByteBuffer pBuffer,final int bufferSize, final DataType dataType);

}
