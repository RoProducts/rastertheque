package de.rooehler.rastertheque.colormap;

import java.util.NavigableMap;
import java.util.TreeMap;

import android.util.Log;

public class ColorMap {
	
	private NavigableMap<Double,ColorMapEntry> mEntries;
	
	public ColorMap(NavigableMap<Double,ColorMapEntry> pEntries){
		this.mEntries = pEntries;
	}

	public NavigableMap<Double,ColorMapEntry> getmEntries() {
		return mEntries;
	}

	public void setmEntries(TreeMap<Double,ColorMapEntry> mEntries) {
		this.mEntries = mEntries;
	}
	
	public int getColorAccordingToValue(Double val){
//				Log.d("ColorMap", "Requesting color for "+val+" result = "+mEntries.get(mEntries.floorKey(val)).getColor());
		return mEntries.get(mEntries.floorKey(val)).getColor();
		
//		for(int i = 0; i < mEntries.size() - 1; i++){
//			if(value >= mEntries.get(i).getQuantity()){
//				//it is smaller than the next but larger as last ?
//				if(value <= mEntries.get(i + 1).getQuantity()){
//					
//					return mEntries.get(i).getColor();
//				}
//			}
//		}
//		
//		throw new IllegalArgumentException("invalid color" + value);
	}
	


}
