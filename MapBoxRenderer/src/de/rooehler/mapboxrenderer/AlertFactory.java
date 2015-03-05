package de.rooehler.mapboxrenderer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
/**
 * Factory for dialogs
 * 
 * @author Robert Oehler
 *
 */
public class AlertFactory {
	
	/**
	 * shows a simple message containing title and message
	 * has only one "ok" button
	 * 
	 * @param context the context to show in
	 * @param title the title of the dialog to show
	 * @param message the message of the dialog to show
	 */
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
