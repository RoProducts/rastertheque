package de.rooehler.mapboxrenderer.fileselection;

import com.mapbox.mapboxsdk.tileprovider.tilesource.ITileLayer;


public enum SupportedType {
	
	Satellite,
	Terrain,
	Street,
	Outdoors,
	Woodcut,
	Pencil,
	SpaceShip,
	MBTILES,
	RASTER;
	
	
	public static final String TAG = SupportedType.class.getSimpleName();
	
	
	public static String[] getExtensionForType(SupportedType type){
		
		switch (type) {
		case MBTILES:
			return new String[]{"mbtiles"};
		case RASTER:
			return new String[]{"tif","tiff","dem"};

		default:
			throw new IllegalArgumentException("unsupported type requested");
		}
	}
	public static String[] getTypes(){
		
		String[] titles = new String[SupportedType.values().length];
		
		for(int i = 0; i < SupportedType.values().length;i++){
			titles[i] = SupportedType.values()[i].name();
		}
		
		return titles;
	}

	
}
