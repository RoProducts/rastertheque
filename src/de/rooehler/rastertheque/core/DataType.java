package de.rooehler.rastertheque.core;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import org.gdal.gdal.Band;

/**
 * Numeric data type enumeration.
 */
public enum DataType {

	CHAR {
		@Override
		protected int bits() {
			return Character.SIZE;
		}
	},
	BYTE {
		@Override
		protected int bits() {
			return Byte.SIZE;
		}
	},
	SHORT {
		@Override
		protected int bits() {
			return Short.SIZE;
		}
	},
	INT {
		@Override
		protected int bits() {
			return Integer.SIZE;
		}
	},
	LONG {
		@Override
		protected int bits() {
			return Long.SIZE;
		}
	},
	FLOAT {
		@Override
		protected int bits() {
			return Float.SIZE;
		}
	},
	DOUBLE {
		@Override
		protected int bits() {
			return Double.SIZE;
		}
	};

	public static DataType getDatatype(final Band band) {
		int dt = band.GetRasterDataType();
		return dt == org.gdal.gdalconst.gdalconstConstants.GDT_Byte ? DataType.BYTE :
			   dt == org.gdal.gdalconst.gdalconstConstants.GDT_Int16 ? DataType.SHORT :
			   dt == org.gdal.gdalconst.gdalconstConstants.GDT_UInt16 ? DataType.INT :
			   dt == org.gdal.gdalconst.gdalconstConstants.GDT_Int32 ?  DataType.INT :
			   dt == org.gdal.gdalconst.gdalconstConstants.GDT_UInt32 ? DataType.LONG :
			   dt == org.gdal.gdalconst.gdalconstConstants.GDT_Float32 ? DataType.FLOAT :
			   dt == org.gdal.gdalconst.gdalconstConstants.GDT_Float64 ? DataType.DOUBLE :
		null;

	}

	public static int toGDAL(DataType datatype) {
		switch(datatype) {
		case CHAR:
			break;
		case BYTE:
			return org.gdal.gdalconst.gdalconstConstants.GDT_Byte;
		case SHORT:
			return org.gdal.gdalconst.gdalconstConstants.GDT_Int16;
		case INT:
			return org.gdal.gdalconst.gdalconstConstants.GDT_Int32;
		case LONG:
			return org.gdal.gdalconst.gdalconstConstants.GDT_UInt32;
		case FLOAT:
			return org.gdal.gdalconst.gdalconstConstants.GDT_Float32;
		case DOUBLE:
			return org.gdal.gdalconst.gdalconstConstants.GDT_Float64;
		}
		throw new IllegalArgumentException("unsupported data type: " + datatype);
	}
	
//	public static Object getArrayAccordingToDatatype(final DataType dt, final ByteBuffer pBuffer){
//
//		Buffer b = null;
//		switch(dt) {
//		case CHAR:
//			b = pBuffer.asCharBuffer();
//		case BYTE:
//			b = pBuffer;
//		case SHORT:
//			b =  pBuffer.asShortBuffer();
//		case INT:
//			b =  pBuffer.asIntBuffer();
//		case LONG:
//			b =  pBuffer.asLongBuffer();
//		case FLOAT:
//			b =  pBuffer.asFloatBuffer();
//		case DOUBLE:
//			b =  pBuffer.asDoubleBuffer();
//		}
//
//		return  b.array();
//	}

	/**
	 * The size of the datatype in bytes.
	 */
	 public int size() {
		return bits() / 8;
	}

	/**
	 * The size of the datatype in bits.
	 */
	 protected abstract int bits();
}