package de.rooehler.rastertheque.processing;

import java.util.ArrayList;
import java.util.Iterator;

import de.rooehler.rastertheque.core.util.CustomServiceLoader;

public class RasterOps {
	
	@SuppressWarnings({"unchecked","rawtypes"})
	public static ArrayList<?> getRasterOps(final String pathToService, final Class clazz){

		ArrayList<Object> drivers = new ArrayList<>();

		Iterator<?> it = CustomServiceLoader.load(clazz, pathToService).iterator();

		while(it.hasNext()){
			drivers.add(it.next());
		}

		return drivers;
	}



}
