package de.rooehler.rastertheque.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import de.rooehler.rastertheque.core.util.CustomServiceLoader;
import de.rooehler.rastertheque.util.Hints;
/**
 * Facade class to access available drivers during runtime
 * and open files using them
 * 
 * @author Robert Oehler
 *
 */
public class Drivers {

	private final static String PATH_TO_DRIVERS = "org/rastertheque/io/driver/";
	
	private static ArrayList<Driver> drivers;
	
	static{
		drivers = getDrivers(PATH_TO_DRIVERS);
	}
	
	/**
	 * opens a file resulting in a dataset using
	 * 1. a driver provided in the hints
	 * 2.the first driver which is able to open the file
	 * which is available during runtime and was specified in a
	 * configuration file called "de.rooehler.rastertheque.driver"
	 * 
	 * @see https://docs.oracle.com/javase/6/docs/api/java/util/ServiceLoader.html for a description of the concept
	 * 
	 * @param filePath the path to the file
	 * @param hints (can be null) can contain a priorized driver to open the file
	 * @return the dataset which the driver opened or null if no driver was able to open the file
	 * @throws IOException that occurred during I/O operation on the file
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
