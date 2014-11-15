package de.rooehler.rastertheque.io.mbtiles.gdal;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteOrder;

/**
 * 
 * @author Robert Oehler
 * 
 * Adaption to read from a bytebuffer based on :
 * 
 * https://github.com/geosolutions-it/imageio-ext/blob/master/library/streams/src/main/java/it/geosolutions/imageio/stream/eraf/EnhancedRandomAccessFile.java
 * 
 * @author Simone Giannecchini, GeoSolutions.
 * 
 * -- NOTES from a class we derived this class from --
 * 
 * 
 * RandomAccessFile.java. By Russ Rew, based on BufferedRandomAccessFile by Alex
 * McManus, based on Sun's source code for java.io.RandomAccessFile. For Alex
 * McManus version from which this derives, see his <a
 * href="http://www.aber.ac.uk/~agm/Java.html"> Freeware Java Classes</a>.
 * 
 * A buffered drop-in replacement for java.io.RandomAccessFile. Instances of
 * this class realise substantial speed increases over java.io.RandomAccessFile
 * through the use of buffering. This is a subclass of Object, as it was not
 * possible to subclass java.io.RandomAccessFile because many of the methods are
 * final. However, if it is necessary to use RandomAccessFile and
 * java.io.RandomAccessFile interchangeably, both classes implement the
 * DataInput and DataOutput interfaces.
 * 
 * @author Alex McManus
 * @author Russ Rew
 * @author john caron
 * 
 * @version $Id: EnhancedRandomAccessFile.java 1117 2007-02-20 09:46:00Z simboss $
 * @see DataInput
 * @see DataOutput
 * @see java.io.RandomAccessFile
 * @todo optimize {@link #readLine()}
 * @task {@link ByteOrder} is not respected with writing
 */

public class ByteBufferReader {

	private byte[] buffer;
	protected long filePosition;
	
	/**
	 * The offset in bytes of the start of the buffer, from the start of the
	 * eraf.
	 */
	protected long bufferStart;

	/**
	 * The offset in bytes of the end of the data in the buffer, from the start
	 * of the eraf. This can be calculated from
	 * <code>bufferStart + dataSize</code>, but it is cached to speed up the
	 * read( ) method.
	 */
	protected long dataEnd;
	
	/**
	 * The size of the data stored in the buffer, in bytes. This may be less
	 * than the size of the buffer.
	 */
	protected int dataSize;

	
	/** The current endian (big or little) mode of the eraf. */
	protected boolean bigEndian;
	
	public ByteBufferReader(byte[] pBuffer, final ByteOrder pOrder){
		
		this.buffer = pBuffer;

		this.bigEndian = (pOrder == ByteOrder.BIG_ENDIAN);
		
		init();
	}
	
	public void init(){
		// Initialise the buffer
		bufferStart = 0;
		dataEnd =  this.buffer.length;
		dataSize = 0;
		filePosition = 0;
		
	}
	
	
	/**
	 * Read a byte of data from the eraf, blocking until data is available.
	 * 
	 * @return the next byte of data, or -1 if the end of the eraf is reached.
	 * @exception IOException
	 *                if an I/O error occurrs.
	 */
	private int read() throws IOException {

		// If the eraf position is within the data, return the byte...
		if (filePosition < dataEnd) {
			int pos = (int) (filePosition - bufferStart);
			filePosition++;
			return (buffer[pos] & 0xff);

			// ...or should we indicate EOF...
		} 
		return -1;
	}
	/**
	 * Read up to <code>len</code> bytes into an array, at a specified offset.
	 * This will block until at least one byte has been read.
	 * 
	 * @param b
	 *            the byte array to receive the bytes.
	 * @param off
	 *            the offset in the array where copying will start.
	 * @param len
	 *            the number of bytes to copy.
	 * @return the actual number of bytes read, or -1 if there is not more data
	 *         due to the end of the eraf being reached.
	 * @exception IOException
	 *                if an I/O error occurrs.
	 */
	private int read(byte b[], int off, int len) throws IOException {
		return readBytes(b, off, len);
	}
	
	/**
	 * Read up to <code>len</code> bytes into an array, at a specified offset.
	 * This will block until at least one byte has been read.
	 * 
	 * @param b
	 *            the byte array to receive the bytes.
	 * @param off
	 *            the offset in the array where copying will start.
	 * @param len
	 *            the number of bytes to copy.
	 * @return the actual number of bytes read, or -1 if there is not more data
	 *         due to the end of the eraf being reached.
	 * @exception IOException
	 *                if an I/O error occurrs.
	 */
	private int readBytes(byte b[], int off, int len) throws IOException {

		// See how many bytes are available in the buffer - if none,
		// seek to the eraf position to update the buffer and try again.
		int bytesAvailable = (int) (dataEnd - filePosition);


		// Copy as much as we can.
		int copyLength = (bytesAvailable >= len) ? len : bytesAvailable;
		System.arraycopy(buffer, (int) (filePosition - bufferStart), b, off,
				copyLength);
		filePosition += copyLength;

		// If there is more to copy...
		if (copyLength < len) {
			throw new IllegalArgumentException("there is more to copy than was available, should not happen");
		}

		// Return the amount copied.
		return copyLength;
	}

	

	//
	// DataInput methods.
	//

	/**
	 * Reads a <code>boolean</code> from this eraf. This method reads a single
	 * byte from the eraf. A value of <code>0</code> represents
	 * <code>false</code>. Any other value represents <code>true</code>.
	 * This method blocks until the byte is read, the end of the stream is
	 * detected, or an exception is thrown.
	 * 
	 * @return the <code>boolean</code> value read.
	 * @exception EOFException
	 *                if this eraf has reached the end.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	public boolean readBoolean() throws IOException {
		int ch = this.read();
		if (ch < 0) {
			throw new EOFException();
		}
		return (ch != 0);
	}

	/**
	 * Reads a signed 8-bit value from this eraf. This method reads a byte from
	 * the eraf. If the byte read is <code>b</code>, where
	 * <code>0&nbsp;&lt;=&nbsp;b&nbsp;&lt;=&nbsp;255</code>, then the result
	 * is:
	 * <ul>
	 * <code>
	 *     (byte)(b)
	 * </code>
	 * </ul>
	 * <p>
	 * This method blocks until the byte is read, the end of the stream is
	 * detected, or an exception is thrown.
	 * 
	 * @return the next byte of this eraf as a signed 8-bit <code>byte</code>.
	 * @exception EOFException
	 *                if this eraf has reached the end.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	public byte readByte() throws IOException {
		int ch = this.read();
		if (ch < 0) {
			throw new EOFException();
		}
		return (byte) (ch);
	}

	/**
	 * Reads an unsigned 8-bit number from this eraf. This method reads a byte
	 * from this eraf and returns that byte.
	 * <p>
	 * This method blocks until the byte is read, the end of the stream is
	 * detected, or an exception is thrown.
	 * 
	 * @return the next byte of this eraf, interpreted as an unsigned 8-bit
	 *         number.
	 * @exception EOFException
	 *                if this eraf has reached the end.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	public int readUnsignedByte() throws IOException {
		int ch = this.read();
		if (ch < 0) {
			throw new EOFException();
		}
		return ch;
	}

	/**
	 * Reads a signed 16-bit number from this eraf. The method reads 2 bytes
	 * from this eraf. If the two bytes read, in order, are <code>b1</code>
	 * and <code>b2</code>, where each of the two values is between
	 * <code>0</code> and <code>255</code>, inclusive, then the result is
	 * equal to:
	 * <ul>
	 * <code>
	 *     (short)((b1 &lt;&lt; 8) | b2)
	 * </code>
	 * </ul>
	 * <p>
	 * This method blocks until the two bytes are read, the end of the stream is
	 * detected, or an exception is thrown.
	 * 
	 * @return the next two bytes of this eraf, interpreted as a signed 16-bit
	 *         number.
	 * @exception EOFException
	 *                if this eraf reaches the end before reading two bytes.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	public short readShort() throws IOException {
		final byte b[] = new byte[2];
		if (read(b, 0, 2) < 0) {
			throw new EOFException();
		}
		if (bigEndian) {
			return (short) (((b[0] & 0xFF) << 8) + (b[1] & 0xFF));
		} else {
			return (short) (((b[1] & 0xFF) << 8) + (b[0] & 0xFF));
		}
	}

	/**
	 * _more_
	 * 
	 * @param pa
	 *            _more_
	 * @param start
	 *            _more_
	 * @param n
	 *            _more_
	 * 
	 * @throws IOException
	 *             _more_
	 */
	private void readShort(short[] pa, int start, int n) throws IOException {
		for (int i = start, end = n + start; i < end; i++) {
			pa[i] = readShort();
		}
	}

	/**
	 * Reads an unsigned 16-bit number from this eraf. This method reads two
	 * bytes from the eraf. If the bytes read, in order, are <code>b1</code>
	 * and <code>b2</code>, where
	 * <code>0&nbsp;&lt;=&nbsp;b1, b2&nbsp;&lt;=&nbsp;255</code>, then the
	 * result is equal to:
	 * <ul>
	 * <code>
	 *     (b1 &lt;&lt; 8) | b2
	 * </code>
	 * </ul>
	 * <p>
	 * This method blocks until the two bytes are read, the end of the stream is
	 * detected, or an exception is thrown.
	 * 
	 * @return the next two bytes of this eraf, interpreted as an unsigned
	 *         16-bit integer.
	 * @exception EOFException
	 *                if this eraf reaches the end before reading two bytes.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	public int readUnsignedShort() throws IOException {
		byte b[] = new byte[2];
		if (read(b, 0, 2) < 0) {
			throw new EOFException();
		}
		if (bigEndian) {
			return ((b[0] & 0xFF) << 8) + (b[1] & 0xFF) & 0xFFFF;
		} else {
			return ((b[1] & 0xFF) << 8) + (b[0] & 0xFF) & 0xFFFF;
		}
	}

	/**
	 * Reads a Unicode character from this eraf. This method reads two bytes
	 * from the eraf. If the bytes read, in order, are <code>b1</code> and
	 * <code>b2</code>, where
	 * <code>0&nbsp;&lt;=&nbsp;b1,&nbsp;b2&nbsp;&lt;=&nbsp;255</code>, then
	 * the result is equal to:
	 * <ul>
	 * <code>
	 *     (char)((b1 &lt;&lt; 8) | b2)
	 * </code>
	 * </ul>
	 * <p>
	 * This method blocks until the two bytes are read, the end of the stream is
	 * detected, or an exception is thrown.
	 * 
	 * @return the next two bytes of this eraf as a Unicode character.
	 * @exception EOFException
	 *                if this eraf reaches the end before reading two bytes.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	public char readChar() throws IOException {
		final byte b[] = new byte[2];
		if (read(b, 0, 2) < 0) {
			throw new EOFException();
		}
		if (bigEndian) {
			return (char) (((b[0] & 0xFF) << 8) + (b[1] & 0xFF));
		} else {
			return (char) (((b[1] & 0xFF) << 8) + (b[0] & 0xFF));
		}
	}

	/**
	 * Reads a signed 32-bit integer from this eraf. This method reads 4 bytes
	 * from the eraf. If the bytes read, in order, are <code>b1</code>,
	 * <code>b2</code>, <code>b3</code>, and <code>b4</code>, where
	 * <code>0&nbsp;&lt;=&nbsp;b1, b2, b3, b4&nbsp;&lt;=&nbsp;255</code>,
	 * then the result is equal to:
	 * <ul>
	 * <code>
	 *     (b1 &lt;&lt; 24) | (b2 &lt;&lt; 16) + (b3 &lt;&lt; 8) + b4
	 * </code>
	 * </ul>
	 * <p>
	 * This method blocks until the four bytes are read, the end of the stream
	 * is detected, or an exception is thrown.
	 * 
	 * @return the next four bytes of this eraf, interpreted as an
	 *         <code>int</code>.
	 * @exception EOFException
	 *                if this eraf reaches the end before reading four bytes.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	public int readInt() throws IOException {
		final byte b[] = new byte[4];
		if (read(b, 0, 4) < 0) {
			throw new EOFException();
		}
		if (bigEndian) {
			return (((b[0] & 0xFF) << 24) + ((b[1] & 0xFF) << 16)
					+ ((b[2] & 0xFF) << 8) + ((b[3] & 0xFF)));
		} else {
			return (((b[3] & 0xFF) << 24) + ((b[2] & 0xFF) << 16)
					+ ((b[1] & 0xFF) << 8) + ((b[0] & 0xFF)));
		}
	}

	public long readUnsignedInt() throws IOException {
		// retaining only the first 4 bytes, ignoring sign when extending
		return ((long) readInt()) & 0xFFFFFFFFL;
	}


	/**
	 * Reads a signed 24-bit integer from this eraf. This method reads 3 bytes
	 * from the eraf. If the bytes read, in order, are <code>b1</code>,
	 * <code>b2</code>, and <code>b3</code>, where
	 * <code>0&nbsp;&lt;=&nbsp;b1, b2, b3&nbsp;&lt;=&nbsp;255</code>, then
	 * the result is equal to:
	 * <ul>
	 * <code>
	 *     (b1 &lt;&lt; 16) | (b2 &lt;&lt; 8) + (b3 &lt;&lt; 0)
	 * </code>
	 * </ul>
	 * <p>
	 * This method blocks until the three bytes are read, the end of the stream
	 * is detected, or an exception is thrown.
	 * 
	 * @param pa
	 *            _more_
	 * @param start
	 *            _more_
	 * @param n
	 *            _more_
	 * @exception EOFException
	 *                if this eraf reaches the end before reading four bytes.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	private void readInt(int[] pa, int start, int n) throws IOException {
		for (int i = start, end = n + start; i < end; i++) {
			pa[i] = readInt();
		}
	}

	/**
	 * Reads a signed 64-bit integer from this eraf. This method reads eight
	 * bytes from the eraf. If the bytes read, in order, are <code>b1</code>,
	 * <code>b2</code>, <code>b3</code>, <code>b4</code>,
	 * <code>b5</code>, <code>b6</code>, <code>b7</code>, and
	 * <code>b8,</code> where:
	 * <ul>
	 * <code>
	 *     0 &lt;= b1, b2, b3, b4, b5, b6, b7, b8 &lt;=255,
	 * </code>
	 * </ul>
	 * <p>
	 * then the result is equal to:
	 * <p>
	 * <blockquote>
	 * 
	 * <pre>
	 * ((long) b1 &lt;&lt; 56) + ((long) b2 &lt;&lt; 48) + ((long) b3 &lt;&lt; 40) + ((long) b4 &lt;&lt; 32)
	 * 		+ ((long) b5 &lt;&lt; 24) + ((long) b6 &lt;&lt; 16) + ((long) b7 &lt;&lt; 8) + b8
	 * </pre>
	 * 
	 * </blockquote>
	 * <p>
	 * This method blocks until the eight bytes are read, the end of the stream
	 * is detected, or an exception is thrown.
	 * 
	 * @return the next eight bytes of this eraf, interpreted as a
	 *         <code>long</code>.
	 * @exception EOFException
	 *                if this eraf reaches the end before reading eight bytes.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	public long readLong() throws IOException {
		final byte b[] = new byte[8];
		if (read(b, 0, 8) < 0) {
			throw new EOFException();
		}
		if (bigEndian) {
			return (((b[0] & 0xFFL) << 56) + ((b[1] & 0xFFL) << 48)
					+ ((b[2] & 0xFFL) << 40) + ((b[3] & 0xFFL) << 32)
					+ ((b[4] & 0xFFL) << 24) + ((b[5] & 0xFFL) << 16)
					+ ((b[6] & 0xFFL) << 8) + ((b[7] & 0xFFL)));
		} else {
			return (((b[7] & 0xFFL) << 56) + ((b[6] & 0xFFL) << 48)
					+ ((b[5] & 0xFFL) << 40) + ((b[4] & 0xffL) << 32)
					+ ((b[3] & 0xFFL) << 24) + ((b[2] & 0xFFL) << 16)
					+ ((b[1] & 0xFFL) << 8) + ((b[0] & 0xFFL)));
		}
	}

	/**
	 * _more_
	 * 
	 * @param pa
	 *            _more_
	 * @param start
	 *            _more_
	 * @param n
	 *            _more_
	 * 
	 * @throws IOException
	 *             _more_
	 */
	private void readLong(long[] pa, int start, int n) throws IOException {
		for (int i = start, end = n + start; i < end; i++) {
			pa[i] = readLong();
		}
	}

	/**
	 * Reads a <code>float</code> from this eraf. This method reads an
	 * <code>int</code> value as if by the <code>readInt</code> method and
	 * then converts that <code>int</code> to a <code>float</code> using the
	 * <code>intBitsToFloat</code> method in class <code>Float</code>.
	 * <p>
	 * This method blocks until the four bytes are read, the end of the stream
	 * is detected, or an exception is thrown.
	 * 
	 * @return the next four bytes of this eraf, interpreted as a
	 *         <code>float</code>.
	 * @exception EOFException
	 *                if this eraf reaches the end before reading four bytes.
	 * @exception IOException
	 *                if an I/O error occurs.
	 * @see java.io.RandomAccessFile#readInt()
	 * @see java.lang.Float#intBitsToFloat(int)
	 */
	public float readFloat() throws IOException {
		return Float.intBitsToFloat(readInt());
	}

	/**
	 * _more_
	 * 
	 * @param pa
	 *            _more_
	 * @param start
	 *            _more_
	 * @param n
	 *            _more_
	 * 
	 * @throws IOException
	 *             _more_
	 */
	private void readFloat(float[] pa, int start, int n) throws IOException {
		for (int i = start, end = n + start; i < end; i++) {
			pa[i] = Float.intBitsToFloat(readInt());
		}
	}
	
	/**
	 * Reads a <code>double</code> from this eraf. This method reads a
	 * <code>long</code> value as if by the <code>readLong</code> method and
	 * then converts that <code>long</code> to a <code>double</code> using
	 * the <code>longBitsToDouble</code> method in class <code>Double</code>.
	 * <p>
	 * This method blocks until the eight bytes are read, the end of the stream
	 * is detected, or an exception is thrown.
	 * 
	 * @return the next eight bytes of this eraf, interpreted as a
	 *         <code>double</code>.
	 * @exception EOFException
	 *                if this eraf reaches the end before reading eight bytes.
	 * @exception IOException
	 *                if an I/O error occurs.
	 * @see java.io.RandomAccessFile#readLong()
	 * @see java.lang.Double#longBitsToDouble(long)
	 */
	public double readDouble() throws IOException {
		return Double.longBitsToDouble(readLong());
	}

	/**
	 * _more_
	 * 
	 * @param pa
	 *            _more_
	 * @param start
	 *            _more_
	 * @param n
	 *            _more_
	 * 
	 * @throws IOException
	 *             _more_
	 */
	private void readDouble(double[] pa, int start, int n) throws IOException {
		for (int i = start, end = n + start; i < end; i++) {
			pa[i] = Double.longBitsToDouble(readLong());
		}
	}
	
	
}
