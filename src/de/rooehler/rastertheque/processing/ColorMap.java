package de.rooehler.rastertheque.processing;

import java.util.ArrayList;

import android.util.Log;
import android.util.Pair;


/**
 * class which defines ranges of values and maps colors to them
 * for fast access on arbitrary values it uses a NavigableMap 
 * As improvement the nodata value is handled separately
 * @author robertoehler
 *
 */
public class ColorMap {
	
	private Pair<Double,Integer> mNoData;
	private ArrayList<ColorMapEntry> mEntries;
	private double mMinValue;
	
	public ColorMap(ArrayList<ColorMapEntry> pEntries,final double pMin, final Pair<Double,Integer> pNoData){
		
		this.mEntries = pEntries;
		
		this.mNoData = pNoData;
		
		this.mMinValue = pMin;
	}

	public ArrayList<ColorMapEntry> getEntries() {
		
		return mEntries;
	}

	public void setEntries(ArrayList<ColorMapEntry> pEntries) {
		
		this.mEntries = pEntries;
	}
	
	public int getColorAccordingToValue(Double val){
		
		if(mNoData != null && val.equals(mNoData.first)){
			return mNoData.second;
		}
		
		int index = (int) (val - mMinValue);
		
		try{
			return  mEntries.get(index).getColor();
			
		}catch(IndexOutOfBoundsException e){
			Log.e("ColorMap", "IndexOutOfBoundsException");
			return mEntries.get(0).getColor();
		}

	}
}
