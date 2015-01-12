package de.rooehler.rastertheque.io.mbtiles;

import java.io.IOException;

import android.content.Context;
import de.rooehler.rastertheque.core.Driver;

public class MBTilesDriver implements Driver<MBTilesDataset>  {
	
	private Context mContext;
	
	public MBTilesDriver(final Context context){
		
		this.mContext = context;	
		
	}

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
			
			final MBTilesDatabase db = new MBTilesDatabase(mContext, path);
			
			db.openDataBase();
			
			db.close();
			
		}catch(Exception e){
			return false;
		}
		
		return true;
	}

	@Override
	public MBTilesDataset open(String filePath) throws IOException {
		
		return new MBTilesDataset(mContext, filePath);
	}

}
