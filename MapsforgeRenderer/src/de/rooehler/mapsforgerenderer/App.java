package de.rooehler.mapsforgerenderer;

import org.mapsforge.map.android.graphics.AndroidGraphicFactory;

import android.app.Application;

public class App extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
		//this call is mandatory to use the Mapsforge library
		AndroidGraphicFactory.createInstance(this);
			

	}
}
