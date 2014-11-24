package de.rooehler.rastertheque.interfaces;

import java.nio.ByteBuffer;

import de.rooehler.rastertheque.io.gdal.DataType;

public interface RasterProcessing {
	
	public int[] generateGrayScalePixelsCalculatingMinMax(final ByteBuffer pBuffer,final int bufferSize, final DataType dataType);
	
	public int[] generatePixelsWithColorMap(final ByteBuffer pBuffer,final int bufferSize, final DataType dataType);

}
