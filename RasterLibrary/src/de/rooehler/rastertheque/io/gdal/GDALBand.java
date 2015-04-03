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
/**
 * wraps a org.gdal.gdal.Band
 * 
 * @author Robert Oehler
 *
 */
public class GDALBand implements Band{
	
	org.gdal.gdal.Band band;
	
	private ColorMap mColorMap;
	
	public void clearColorMap(){
		
		mColorMap = null;
	}
	
	/**
	 * applies a colormap to this band
	 * 
	 * if the according raster file is accompanied by a sld file of the same name
	 * 
	 * it is parsed and the resulting colormap use for this band
	 * if the raster range is below Constants.COLORMAP_ENTRY_THRESHOLD
	 * 
	 * the colormap is additionally interpolated to provide a color for every raster value
	 * 
	 * @param filePath
	 */
	public void applySLDColorMap(final String filePath){
		
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

	/**
	 * returns the data type of this band
	 */
	@Override
	public DataType datatype() {
		
		DataType datatype = DataType.BYTE;
		
		DataType dt = DataType.getDatatype(band);
		if (dt != null && dt.compareTo(datatype) > 0) {
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

	 /**
	  * returns the nodata for this band if available,
	  * 
	  * NoData.None otherwise
	  */
	@Override
	public NoData nodata() {
		
		Double[] nodata = new Double[]{null};
        band.GetNoDataValue(nodata);
        
        if(nodata[0] != null){
        	return NoData.create(nodata[0]);
        }
        
        return NoData.NONE;
	}
	
	/**
	 * returns the original gdal. Band
	 * @return
	 */
	public org.gdal.gdal.Band getBand(){
		return this.band;
	}
	
	/**
	 * calculates the min/max values of a band
	 * @param the band to use
	 * @return an array of format {min,max}
	 */
	public double[] getMinMax(){
		
		double[] min = new double[1];
        double[] max = new double[1];
		this.band.ComputeStatistics(true, min, max);
		
		return new double[]{min[0],max[0]};
	}

	/**
	 * gets the colormap of this band
	 * 
	 * if there is no sld file parsed
	 * the band is checked for a color table provided by GDAL
	 * if available, it is parsed and returned
	 * 
	 * otherwise this band has no colormap and null is returned
	 */
	@Override
	public ColorMap colorMap() {
		
		if(mColorMap == null){
			if(this.band.GetColorTable() != null){
				mColorMap = convertGDALColorTable(this.band.GetColorTable());
			}
		}
		return mColorMap;
	}

	/**
	 * converts the GDAL ColorTable to a ColorMap
	 * @param colorTable
	 * @return the colroMap
	 */
	private ColorMap convertGDALColorTable(ColorTable colorTable) {
	
		ArrayList<ColorMapEntry> entries = new ArrayList<>();
		
		for(int i = 0 ; i < colorTable.GetCount(); i++){
			
			int color = colorTable.GetColorEntry(i);
			
			entries.add(new ColorMapEntry(color, i, 1.0, null));
			
		}
		
		return new ColorMap(entries, 0, colorTable.GetCount() - 1, null, true);
 		
	}
}
