package de.rooehler.rasterapp.test.testImpl;

import java.io.IOException;

import de.rooehler.rastertheque.core.Driver;
import de.rooehler.rastertheque.io.gdal.GDALDataset;
/**
 * Test Driver Implementation to test the retrieval of Driver implementations during runtime
 * 
 * @author Robert Oehler
 *
 */
public class TestDriverImpl implements Driver {

	@Override
	public String getName() {
		return TestDriverImpl.class.getSimpleName();
	}

	@Override
	public boolean canOpen(String filePath) {

		return false;
	}

	@Override
	public GDALDataset open(String filePath) throws IOException {

		return null;
	}

}
