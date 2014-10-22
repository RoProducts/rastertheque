package de.rooehler.rastertheque.util.mapsforge.raster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.mapsforge.core.model.Dimension;

import android.graphics.Rect;

public class RasterHelper {

	public static Rect getRect(Dataset dataset) {
		Dimension size = size(dataset);
		return new Rect(0, 0, size.width, size.height);
	}

	public static Dimension size(Dataset dataset) {
		return new Dimension(dataset.getRasterXSize(), dataset.getRasterYSize());
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

	public static List<Band> getBands(Dataset dataset) {
		int nbands = dataset.GetRasterCount();

		List<Band> bands = new ArrayList<Band>(nbands);
		for (int i = 1; i <= nbands; i++) {
			bands.add(dataset.GetRasterBand(i));
		}

		return bands;
	}
	
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
}
