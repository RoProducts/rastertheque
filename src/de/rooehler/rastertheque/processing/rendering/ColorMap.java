package de.rooehler.rastertheque.processing.rendering;

import java.util.ArrayList;

import android.util.Log;
import android.util.Pair;


/**
 * class which defines ranges of values and maps colors to them
 * The nodata value is handled separately
 * It uses internally an array where every (pixel) value that is covered must be a member of the mEntries list
 * To be able to handle negative values, a min Value is set by which the query is reduced when getting the value
 * which will result e.g. for a range {-100,100} with minValue - 100 in the index -100 - -100 --> 0
 * @author Robert Oehler
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
			if(mNoData == null){
				mNoData = new Pair<Double, Integer>(val, mEntries.get(0).getColor());
			}else{				
				Log.e("ColorMap", "IndexOutOfBoundsException");
			}
			return mEntries.get(0).getColor();
		}

	}
}
