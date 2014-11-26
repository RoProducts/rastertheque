package de.rooehler.rastertheque.processing;

import java.nio.ByteBuffer;

import de.rooehler.rastertheque.io.gdal.DataType;

public interface ColorMapProcessing {
	
	public int[] generateGrayScalePixelsCalculatingMinMax(final ByteBuffer pBuffer,final int bufferSize, final DataType dataType);
	
	public int[] generatePixelsWithColorMap(final ByteBuffer pBuffer,final int bufferSize, final DataType dataType);

}