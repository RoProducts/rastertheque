package de.rooehler.rastertheque.interfaces;

import java.nio.ByteBuffer;

import de.rooehler.rastertheque.core.Dimension;
import de.rooehler.rastertheque.core.Rectangle;

public interface RasterIO {
	
	/**
	 * Opens a file
	 * @param filePath the file to open
	 * @return true if opening was successfull, false otherwise
	 */
	public boolean open(String filePath);
	
	/**
	 * read from a file 
	 * @param src the region to read
	 * @param dst apply resampling to scale pixels to this destination size
	 * @param buffer write the result into this buffer
	 */
	public void read(
			final Rectangle src,
			final Dimension dstDim,
			final ByteBuffer buffer);
	/**
	 * read from a file 
	 * @param src the region to read
	 * @param buffer write the result into this buffer
	 */
	public void read(
			final Rectangle src,
			final ByteBuffer buffer);

}
