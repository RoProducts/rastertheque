package de.rooehler.rastertheque.core;

import java.util.ArrayList;
import java.util.Iterator;

import de.rooehler.rastertheque.core.util.CustomServiceLoader;

public class Drivers {

	
	/**
	 * retrieve implementations of the Driver class
	 * 
	 * @param pathToservice, the fully qualified package name where implementations of the Driver class reside
	 * @return a list of the implementations 
	 */
	@SuppressWarnings("rawtypes")
	public static ArrayList<Driver<?>> getDrivers(final String pathToService){

		ArrayList<Driver<?>> drivers = new ArrayList<>();

		/**
		 * CustomServiceLoader is an adaption for Android which does not allow to use the standard META-INF/services location
		 * as it gets deliberately excluded in the building process by APKBuilder
		 * see
		 * http://www.davidwong.com.au/blog/2011/07/using-a-custom-serviceloader-in-android/
		 */
		Iterator<Driver> it = CustomServiceLoader.load(Driver.class, pathToService).iterator();

		while(it.hasNext()){
			drivers.add(it.next());
		}

		return drivers;
	}


}
