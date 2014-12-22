package de.rooehler.rastertheque.util;

public class Formulae {
	
	/**
	 * get the distance between from and to in meters
	 * using the haversine formula
	 * @param LatLong from 
	 * @param LatLong to
	 * @return the distance as double
	 */
	public static double distanceBetweenInMeters(double fromLat, double fromLon, double toLat, double toLon) {  

		double dLat = Math.toRadians(toLat - fromLat);  
		double dLon = Math.toRadians(toLon - fromLon);  

		double a = Math.sin(dLat/2) * Math.sin(dLat/2) + Math.cos(Math.toRadians(fromLat)) * Math.cos(Math.toRadians(toLat)) * Math.sin(dLon/2) * Math.sin(dLon/2);  
		double c = 2 * Math.asin(Math.sqrt(a));  
		return Constants.EARTH_RADIUS * c;  
	} 

}
