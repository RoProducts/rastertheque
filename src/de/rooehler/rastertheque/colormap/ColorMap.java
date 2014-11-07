package de.rooehler.rastertheque.colormap;

import java.util.NavigableMap;
import java.util.TreeMap;

import android.util.Pair;


/**
 * class which defines ranges of values and maps colors to them
 * for fast access on arbitrary values it uses a NavigableMap 
 * As improvement the nodata value is handled separately
 * @author robertoehler
 *
 */
public class ColorMap {
	
	private NavigableMap<Double,ColorMapEntry> mEntries;
	private Pair<Double,Integer> mNoData;
	
	public ColorMap(NavigableMap<Double,ColorMapEntry> pEntries, final Pair<Double,Integer> pNoData){
		
		this.mEntries = pEntries;
		
		this.mNoData = pNoData;
	}

	public NavigableMap<Double,ColorMapEntry> getEntries() {
		
		return mEntries;
	}

	public void setEntries(TreeMap<Double,ColorMapEntry> mEntries) {
		
		this.mEntries = mEntries;
	}
	
	public int getColorAccordingToValue(Double val){
		
		if(mNoData != null && val.equals(mNoData.first)){
			return mNoData.second;
		}
		
		return  mEntries.get(mEntries.floorKey(val)).getColor();

	}
}
