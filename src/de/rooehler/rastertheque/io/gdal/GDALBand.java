package de.rooehler.rastertheque.io.gdal;

import de.rooehler.rastertheque.core.Band;
import de.rooehler.rastertheque.core.DataType;
import de.rooehler.rastertheque.core.NoData;

public class GDALBand implements Band{
	
	org.gdal.gdal.Band band;

    public GDALBand(org.gdal.gdal.Band band) {
        this.band = band;
    }

	@Override
	public String name() {
		
		return band.GetDescription();
	}

	@Override
	public DataType datatype() {
		
		DataType datatype = DataType.BYTE;
		
		DataType dt = DataType.getDatatype(band);
		if (dt.compareTo(datatype) > 0) {
			datatype = dt;
		}
		
		return datatype;
	}
	
	 @Override
     public Color color() {
         int ci = band.GetColorInterpretation();
         return ci == org.gdal.gdalconst.gdalconstConstants.GCI_Undefined ? Color.UNDEFINED :
                ci == org.gdal.gdalconst.gdalconstConstants.GCI_GrayIndex ? Color.GRAY :
                ci == org.gdal.gdalconst.gdalconstConstants.GCI_RedBand ? Color.RED :
                ci == org.gdal.gdalconst.gdalconstConstants.GCI_GreenBand ? Color.GREEN :
                ci == org.gdal.gdalconst.gdalconstConstants.GCI_BlueBand ? Color.BLUE : Color.GRAY;
     }

	@Override
	public NoData nodata() {
		
		Double[] nodata = new Double[]{null};
        band.GetNoDataValue(nodata);
        
        if(nodata[0] != null){
        	return NoData.create(nodata[0]);
        }
        
        return NoData.NONE;
	}
	
	public org.gdal.gdal.Band getBand(){
		return this.band;
	}

}
