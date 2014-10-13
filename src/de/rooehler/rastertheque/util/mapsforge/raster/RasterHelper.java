package de.rooehler.rastertheque.util.mapsforge.raster;

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

}
