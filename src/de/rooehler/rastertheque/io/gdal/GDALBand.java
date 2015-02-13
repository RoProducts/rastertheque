package de.rooehler.rastertheque.io.gdal;

import java.io.File;
import java.util.ArrayList;

import org.gdal.gdal.ColorTable;

import de.rooehler.rastertheque.core.Band;
import de.rooehler.rastertheque.core.DataType;
import de.rooehler.rastertheque.core.NoData;
import de.rooehler.rastertheque.processing.rendering.ColorMap;
import de.rooehler.rastertheque.processing.rendering.ColorMapEntry;
import de.rooehler.rastertheque.processing.rendering.SLDColorMapParser;
import de.rooehler.rastertheque.util.Constants;

public class GDALBand implements Band{
	
	org.gdal.gdal.Band band;
	
	private static ColorMap mColorMap;
	
	public static void clearColorMap(){
		
		mColorMap = null;
	}
	
	public static void applySLDColorMap(final String filePath){
		
		if(mColorMap == null ){
			if(filePath != null){
				final String colorMapFilePath = filePath.substring(0, filePath.lastIndexOf(".") + 1) + "sld";

				File file = new File(colorMapFilePath);

				if(file.exists()){

					ColorMap rawColorMap = SLDColorMapParser.parseRawColorMapEntries(file);
					
					//check if to use an interpolated color map which maps to every raster value a color
					if(rawColorMap.getRange() < Constants.COLORMAP_ENTRY_THRESHOLD){
						
						mColorMap = SLDColorMapParser.applyInterpolation(rawColorMap);
					}else{
						mColorMap = rawColorMap;
					}

				}
			}
		}
	}


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

	@Override
	public ColorMap colorMap() {
		
		if(mColorMap == null){
			if(this.band.GetColorTable() != null){
				mColorMap = convertGDALColorTable(this.band.GetColorTable());
			}
		}
		return mColorMap;
	}

	private ColorMap convertGDALColorTable(ColorTable colorTable) {
	
		ArrayList<ColorMapEntry> entries = new ArrayList<>();
		
		for(int i = 0 ; i < colorTable.GetCount(); i++){
			
			int color = colorTable.GetColorEntry(i);
			
		}
 		
		return null;
	}
}
