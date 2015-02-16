package de.rooehler.rastertheque.core;

import org.gdal.gdal.Band;

/**
 * Numeric data type enumeration.
 */
public enum DataType {

	BYTE {
		@Override
		protected int bits() {
			return Byte.SIZE;
		}
	},
	CHAR {
		@Override
		protected int bits() {
			return Character.SIZE;
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
		
		//source for conversion : http://grass.osgeo.org/grass64/manuals/r.out.gdal.html
		
		return dt == org.gdal.gdalconst.gdalconstConstants.GDT_Byte ? DataType.BYTE :
			   dt == org.gdal.gdalconst.gdalconstConstants.GDT_Int16 ? DataType.CHAR :
			   dt == org.gdal.gdalconst.gdalconstConstants.GDT_CInt16 ? DataType.SHORT:
			   dt == org.gdal.gdalconst.gdalconstConstants.GDT_UInt16 ? DataType.SHORT :
			   dt == org.gdal.gdalconst.gdalconstConstants.GDT_Int32 ?  DataType.INT :
			   dt == org.gdal.gdalconst.gdalconstConstants.GDT_CInt32 ?  DataType.INT :
			   dt == org.gdal.gdalconst.gdalconstConstants.GDT_UInt32 ? DataType.LONG :
			   dt == org.gdal.gdalconst.gdalconstConstants.GDT_Float32 ? DataType.FLOAT :
			   dt == org.gdal.gdalconst.gdalconstConstants.GDT_CFloat32 ? DataType.FLOAT :
			   dt == org.gdal.gdalconst.gdalconstConstants.GDT_Float64 ? DataType.DOUBLE :
			   dt == org.gdal.gdalconst.gdalconstConstants.GDT_CFloat64 ? DataType.DOUBLE :
		null;

	}

	public static int toGDAL(DataType datatype) {
		switch(datatype) {
		case CHAR:
			return org.gdal.gdalconst.gdalconstConstants.GDT_Int16;
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