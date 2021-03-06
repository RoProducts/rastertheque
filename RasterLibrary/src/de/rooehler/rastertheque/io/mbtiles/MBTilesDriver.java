package de.rooehler.rastertheque.io.mbtiles;

import java.io.IOException;

import com.vividsolutions.jts.geom.Envelope;

import de.rooehler.rastertheque.core.Driver;
/**
 * class which models the access to a MbTiles file
 * 
 * @author Robert Oehler
 *
 */
public class MBTilesDriver implements Driver  {
	

	@Override
	public String getName() {
		
		return "MBTiles Driver";
	}

	@Override
	public boolean canOpen(String path) {
		
		final String extension = path.substring(path.lastIndexOf(".") + 1, path.length());
		
		if(!extension.equals("mbtiles")){
			return false;
		}
		
		try{
			
			final MBTilesDataset dataset = new MBTilesDataset(path);
			@SuppressWarnings("unused")
			//try to access the envelope without raising an exception
			final Envelope en = dataset.getBoundingBox();
			
			dataset.close();
			
		}catch(Exception e){
			return false;
		}
		
		return true;
	}

	@Override
	public MBTilesDataset open(String filePath) throws IOException {
		
		return new MBTilesDataset(filePath);
	}

}
