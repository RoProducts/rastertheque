package de.rooehler.rastertheque.core.util;

import java.io.IOException;

import android.util.Log;
import de.rooehler.rastertheque.core.DataType;

public class ByteBufferReaderUtil {
	
	/**
	 * retrieve a value from the ByteBufferReader according to its datatype
	 * actually the data is read and for a unified return type is cast to double
	 * @param reader the reader to read from
	 * @param dataType the datatype according to which the data is read
	 * @return the value of the pixel
	 */
	public static double getValue(ByteBufferReader reader,final DataType dataType){

		double d = 0.0d;
		try{
			switch(dataType) {
			case CHAR:
				char _char = reader.readChar();
				d = (double) _char;
				break;
			case BYTE:
				byte _byte = reader.readByte();
				d = (double) _byte;
				break;
			case SHORT:
				short _short = reader.readShort();
				d = (double) _short;
				break;
			case INT:
				int _int = reader.readInt();
				d = (double) _int;
				break;
			case LONG:
				long _long = reader.readLong();
				d = (double) _long;
				break;
			case FLOAT:
				float _float = reader.readFloat();
				d = (double) _float;
				break;
			case DOUBLE:
				double _double =  reader.readDouble();
				d = _double;
				break;
			}
		}catch(IOException  e){
			Log.e(ByteBufferReaderUtil.class.getSimpleName(), "error reading from byteBufferedReader");
		}

		return d;
	}

}
