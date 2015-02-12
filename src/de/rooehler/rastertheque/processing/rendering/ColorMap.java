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
	private double mMaxValue;
	private boolean mapsColorsToValues;
	
	private int mCount;
	
	public ColorMap(ArrayList<ColorMapEntry> pEntries,final double pMin, final double pMax, final Pair<Double,Integer> pNoData, boolean hasInterpolatedColorMap){
		
		this.mEntries = pEntries;
		
		this.mNoData = pNoData;
		
		this.mMinValue = pMin;
		
		this.mMaxValue = pMax;
		
		this.mapsColorsToValues = hasInterpolatedColorMap;
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
		
		if(mapsColorsToValues){
			//there should exist a color value according to the raster value
			int index = (int) (val - mMinValue);

			if(index < 0){
				if(mNoData != null){				
					return mNoData.second;
				}else{
					return mEntries.get(0).getColor();
				}
			}
			try{
				return  mEntries.get(index).getColor();

			}catch(IndexOutOfBoundsException e){
				if(mNoData == null){
					mNoData = new Pair<Double, Integer>(val, mEntries.get(0).getColor());
				}else{				
					Log.e("ColorMap", "IndexOutOfBoundsException for index "+index +", size : "+mEntries.size()+", noData "+ mNoData.first);
				}
				return mEntries.get(0).getColor();
			}

		}else{
			//needs to search for the nearest color map entry

			int min = 0;
			int max = mEntries.size() - 1;
			mCount = 0;


			while(max >= min) {
				int middle = (max + min) / 2;

				if(middle <= 0){
					return mEntries.get(middle).getColor();
				}else if(middle >= mEntries.size() - 1){
					return mEntries.get(mEntries.size() - 1).getColor();				            	   
				}else{

					if(mEntries.get(middle - 1).getQuantity() <= val && 
							mEntries.get(middle ).getQuantity() >= val) {
						return mEntries.get(middle - 1).getColor();
					}else if(mEntries.get(middle).getQuantity() <= val && 
							mEntries.get(middle + 1).getQuantity() >= val){
						return mEntries.get(middle).getColor();
					}else  if(mEntries.get(middle).getQuantity() <= val) {
						min = middle + 1;
					}else  if(mEntries.get(middle).getQuantity() >= val) {
						max = middle - 1;
					}
				}
				mCount++;
			}
		}
		return mEntries.get(0).getColor();

	}
	public double getMaxValue() {
		return mMaxValue;
	}
	
	public double getMinValue() {
		return mMinValue;
	}
	public Pair<Double,Integer> getNoData(){
		
		return mNoData;
	}
	
	public double getRange(){
		
		return this.mMaxValue - this.mMinValue;
	}
	
	public boolean hasInterpolatedColorMap(){
		
		return mapsColorsToValues;
	}
	
	public int size(){
		return mEntries.size();
	}
}
