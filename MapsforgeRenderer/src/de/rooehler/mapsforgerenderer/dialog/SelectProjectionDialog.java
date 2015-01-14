package de.rooehler.mapsforgerenderer.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import de.rooehler.mapsforgerenderer.R;
import de.rooehler.rastertheque.util.Constants;

public class SelectProjectionDialog {
	
	public static void showProjectionSelectionDialog(final Activity act,final String current, final IProjectionSelected callback){
		

	    final CharSequence[] items = new CharSequence[]{"Current :",current,"Select projection :","EPSG 4326 (WGS84)","EPSG 3857(Web Mercator)"};
		
		final AlertDialog.Builder builder = new AlertDialog.Builder(act);
		builder.setIcon(R.drawable.ic_launcher);
		builder.setTitle("Projection");
		builder.setItems(items,  new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int item) {
		    
		    	switch (item) {
				case 3:
					callback.selected(Constants.EPSG_4326);
					break;
				case 4:
					callback.selected(Constants.EPSG_3857);
					break;
				}
		    	dialog.dismiss();
		    }
		});

		final AlertDialog currentAlert= builder.create();
		currentAlert.show();
	}

	public interface IProjectionSelected{
		
		public void selected(final String proj);
	}
}

