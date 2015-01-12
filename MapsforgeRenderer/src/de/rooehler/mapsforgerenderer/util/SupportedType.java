package de.rooehler.mapsforgerenderer.util;


public enum SupportedType {
	
	MAPSFORGE,
	MBTILES,
	RASTER;
	
	
	public static final String TAG = SupportedType.class.getSimpleName();
	
	
	public static String[] getExtensions(SupportedType type){
		
		switch (type) {
		case MAPSFORGE:		
			return new String[]{"map"};
		case MBTILES:
			return new String[]{"mbtiles"};
		case RASTER:
			return new String[]{"tif","tiff","dem","img","rsw","mtw","vrt","mem","mpr","mpl","n1","ers","dt0","dt1","dt2","doq"};

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
