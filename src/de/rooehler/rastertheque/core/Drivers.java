package de.rooehler.rastertheque.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import de.rooehler.rastertheque.core.util.CustomServiceLoader;
import de.rooehler.rastertheque.util.Hints;

public class Drivers {

	private final static String PATH_TO_DRIVERS = "org/rastertheque/io/driver/";
	
	private static ArrayList<Driver> drivers;
	
	static{
		drivers = getDrivers(PATH_TO_DRIVERS);
	}
	
	/**
	 * opens a dataset by
	 * 1.if a driver is provided in the hints using it
	 * 2.getting all available drivers and trying to open the file using them
	 * @param filePath the path to the file
	 * @param hints (can be null) can contain a priorized driver to open the file
	 * @return the dataset which the driver opened
	 * @throws IOException that occurred during I/O operation
	 */
	public static Dataset open(String filePath, Hints hints) throws IOException{
		
		if(hints != null && hints.containsKey(Hints.KEY_DRIVER)){
			Driver driver = (Driver) hints.get(Hints.KEY_DRIVER);
			if(driver.canOpen(filePath)){
				return driver.open(filePath);
			}
		}
		
		for(Driver driver : drivers){
			if(driver.canOpen(filePath)){
				
				return driver.open(filePath);
				
			}
		}
		return null;
	}
	
	/**
	 * retrieve implementations of the Driver class
	 * 
	 * @param pathToservice, the fully qualified package name where implementations of the Driver class reside
	 * @return a list of the implementations 
	 */
	private static ArrayList<Driver> getDrivers(final String pathToService){

		ArrayList<Driver> drivers = new ArrayList<>();

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
