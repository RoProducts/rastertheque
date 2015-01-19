package de.rooehler.rastertheque.util;

public class Constants {
	
	
	public static final int EARTH_RADIUS = 6378137;
	
	public static final int DEGREE_IN_METERS_AT_EQUATOR = 111195;
	
	public static final String EPSG_3857 = "PROJCS[\"WGS 84 / Pseudo-Mercator\","+
			  "GEOGCS[\"WGS 84\","+
			      "DATUM[\"WGS_1984\","+
			          "SPHEROID[\"WGS 84\",6378137,298.257223563,"+
			              "AUTHORITY[\"EPSG\",\"7030\"]],"+
			          "AUTHORITY[\"EPSG\",\"6326\"]],"+
			      "PRIMEM[\"Greenwich\",0,"+
			          "AUTHORITY[\"EPSG\",\"8901\"]],"+
			      "UNIT[\"degree\",0.0174532925199433,"+
			          "AUTHORITY[\"EPSG\",\"9122\"]],"+
			      "AUTHORITY[\"EPSG\",\"4326\"]],"+
			  "PROJECTION[\"Mercator_1SP\"],"+
			  "PARAMETER[\"central_meridian\",0],"+
			  "PARAMETER[\"scale_factor\",1],"+
			  "PARAMETER[\"false_easting\",0],"+
			  "PARAMETER[\"false_northing\",0],"+
			  "UNIT[\"metre\",1,"+
			      "AUTHORITY[\"EPSG\",\"9001\"]],"+
			  "AXIS[\"X\",EAST],"+
			  "AXIS[\"Y\",NORTH],"+
			  "EXTENSION[\"PROJ4\",\"+proj=merc +a=6378137 +b=6378137 +lat_ts=0.0 +lon_0=0.0 +x_0=0.0 +y_0=0 +k=1.0 +units=m +nadgrids=@null +wktext  +no_defs\"],"+
			  "AUTHORITY[\"EPSG\",\"3857\"]]";
				
	public static final String EPSG_4326 = "GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\",6378137,298.257223563,AUTHORITY[\"EPSG\",\"7030\"]],AUTHORITY[\"EPSG\",\"6326\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\",0.01745329251994328,AUTHORITY[\"EPSG\",\"9122\"]],AUTHORITY[\"EPSG\",\"4326\"]]";
	

	public static double getResolutionInMetersPerPixelForZoomLevel(int zoomLevel){
		
		if(zoomLevel < 0){
			throw new IllegalArgumentException("negative zoomlevel "+ zoomLevel);
		}
		
		switch(zoomLevel){
		case 0 : return 156543.03392;
		case 1 : return 78271.51696;
		case 2 : return 39135.75848;
		case 3 : return 19567.87924;
		case 4 : return 9783.93962;
		case 5 : return 4891.96981;
		case 6 : return 2445.98490;
		case 7 : return 1222.99245;
		case 8 : return 611.49622;
		case 9 : return 305.748115;
		case 10 : return 152.87405;
		case 11 : return 76.43702;
		case 12 : return 38.21851;
		case 13 : return 19.10925;
		case 14 : return 9.55462;
		case 15 : return 4.77731;
		case 16 : return 2.38865;
		case 17 : return 1.19432;
		case 18 : return 0.59716;
		case 19 : return 0.29858;
		
		default:
			return 156543.03392 / Math.pow(2, zoomLevel);
		}
		
		/*
		 * OR
		 * res = π R / (256 * 2^(z-1))
		 *  π = 3.1415926536, R = 6378137, z = zoom
		 *  source : http://multiplans.net/en_Importation_SphericalMercator.htm
		 *  
		 */
		
	}
}
