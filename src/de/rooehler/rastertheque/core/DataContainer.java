package de.rooehler.rastertheque.core;

import java.nio.ByteBuffer;

import de.rooehler.rastertheque.io.gdal.DataType;

public class DataContainer {
	
	private ByteBuffer mBuffer;
	
	private DataType mDataType;
	
	private int mBufferSize;

	public DataContainer(ByteBuffer pBuffer, DataType pDataType, int pBufferSize) {
		
		this.mBuffer = pBuffer;
		this.mDataType = pDataType;
		this.mBufferSize = pBufferSize;
	}

	/**
	 * @return the mBuffer
	 */
	public ByteBuffer getBuffer() {
		return mBuffer;
	}

	/**
	 * @return the mDataType
	 */
	public DataType getDataType() {
		return mDataType;
	}

	/**
	 * @return the mBufferSize
	 */
	public int getBufferSize() {
		return mBufferSize;
	}
	
	
	
	
	

}
