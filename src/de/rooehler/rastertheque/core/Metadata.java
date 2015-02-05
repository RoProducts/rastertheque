package de.rooehler.rastertheque.core;

import java.util.HashMap;
import java.util.Map;

public class Metadata {
	
	
	private Map<?,?> mMetaData = new HashMap<>();
	
	public Metadata(final Map<?,?> pMetaData){
		
		
		this.setMetaData(pMetaData);
	}

	public Map<?,?> getMetaData() {
		return mMetaData;
	}

	public void setMetaData(Map<?,?> mMetaData) {
		this.mMetaData = mMetaData;
	}

}
