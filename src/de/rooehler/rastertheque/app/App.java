package de.rooehler.rastertheque.app;

import org.mapsforge.map.android.graphics.AndroidGraphicFactory;

import android.app.Application;

public class App extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
		
		AndroidGraphicFactory.createInstance(this);
			

	}
}
