package de.rooehler.rastertheque.colormap;

import java.util.ArrayList;

import android.util.Log;

public class ColorMap {
	
	private ArrayList<ColorMapEntry> mEntries;
	
	public ColorMap(ArrayList<ColorMapEntry> pEntries){
		this.mEntries = pEntries;
	}

	public ArrayList<ColorMapEntry> getmEntries() {
		return mEntries;
	}

	public void setmEntries(ArrayList<ColorMapEntry> mEntries) {
		this.mEntries = mEntries;
	}
	
	public int getColorAccordingToValue(int value){
		
		for(int i = 0; i < mEntries.size() - 1; i++){
			if(value >= mEntries.get(i).getQuantity()){
				//it is smaller than the next but larger as last ?
				if(value <= mEntries.get(i + 1).getQuantity()){
					
					return mEntries.get(i).getColor();
				}
			}
		}
		
		throw new IllegalArgumentException("invalid color" + value);
	}
	


}
