package de.rooehler.mapboxrenderer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

public class AlertFactory {
	
	
	public static void showErrorAlert(final Context context, final String title, final String message){
		
		final AlertDialog.Builder builder = new AlertDialog.Builder(context)
		.setIcon(R.drawable.ic_launcher)
		.setTitle(title)
		.setMessage(message)
		.setPositiveButton(android.R.string.ok, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
				dialog.dismiss();
				
			}
		});
		final AlertDialog currentAlert= builder.create();
		currentAlert.show();
	}

}
