package de.rooehler.rasterapp.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import de.rooehler.rasterapp.R;
import de.rooehler.rastertheque.util.Constants;

public class SelectProjectionDialog {
	
	public static void showProjectionSelectionDialog(final Activity act, final IProjectionSelected callback){
		

	    final CharSequence[] items = new CharSequence[]{"EPSG 4326","EPSG 3857"};

		
		final AlertDialog.Builder builder = new AlertDialog.Builder(act);
		builder.setIcon(R.drawable.ic_launcher);
		builder.setTitle("Select the desired projection");
		builder.setItems(items,  new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int item) {
		    
		    	switch (item) {
				case 0:
					callback.selected(Constants.EPSG_4326);
					break;
				case 1:
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

