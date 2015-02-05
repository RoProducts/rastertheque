package de.rooehler.rasterapp.test.testImpl;

import java.io.IOException;

import de.rooehler.rastertheque.core.Driver;
import de.rooehler.rastertheque.io.gdal.GDALDataset;

public class TestPluggedDriver implements Driver<GDALDataset> {

	@Override
	public String getName() {
		return TestPluggedDriver.class.getSimpleName();
	}

	@Override
	public boolean canOpen(String filePath) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public GDALDataset open(String filePath) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
