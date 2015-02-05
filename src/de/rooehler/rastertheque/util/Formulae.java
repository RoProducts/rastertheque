package de.rooehler.rastertheque.util;

public class Formulae {
	
	/**
	 * calculates the distance between two coordinates in meters
	 * using the Haversine formula
	 * @param LatLong from 
	 * @param LatLong to
	 * @return the distance as double
	 */
	public static double distanceBetweenInMeters(double fromLat, double fromLon, double toLat, double toLon) {  

		double dLat = Math.toRadians(toLat - fromLat);  
		double dLon = Math.toRadians(toLon - fromLon);  

		double a = 
				Math.sin(dLat/2) *
				Math.sin(dLat/2) +
				Math.cos(Math.toRadians(fromLat)) *
				Math.cos(Math.toRadians(toLat)) *
				Math.sin(dLon/2) *
				Math.sin(dLon/2);  
		
		double c = 2 * Math.asin(Math.sqrt(a)); 
		
		return Constants.EARTH_RADIUS * c;  
	} 

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
