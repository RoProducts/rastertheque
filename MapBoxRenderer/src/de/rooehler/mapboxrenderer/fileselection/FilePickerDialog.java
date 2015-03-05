package de.rooehler.mapboxrenderer.fileselection;


import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import de.rooehler.mapboxrenderer.R;

/**
 * class which shows a dialog to let the user select a file
 * 
 * @author Robert Oehler 
 */
public class FilePickerDialog {
	
	protected static AlertDialog currentAlert;
	
	protected FilePickerListAdapter mAdapter;
	protected boolean mShowHiddenFiles = false;
	protected String[] acceptedFileExtensions;
	private Activity mActivity;
	private String mMessage;
	private FilePathPickCallback mFilePathPickCallback;
	
	/**
	 * Constructor which accepts an array of file extension to filter
	 * the results of the disk crawl
	 * @param activity
	 * @param message
	 * @param extensions
	 * @param filePickCallback
	 */
	public FilePickerDialog(final Activity activity,final String message,final String[] extensions, final FilePathPickCallback filePickCallback) {
		
		mActivity = activity;
		
		mMessage = message;
		
		mFilePathPickCallback = filePickCallback;
		
		// Initialize the extensions array to allow any file extensions
		acceptedFileExtensions = extensions;

	}
	/**
	 * Constructor which won't filter results and return all available files
	 * @param activity
	 * @param message
	 * @param filePickCallback
	 */
	public FilePickerDialog(final Activity activity,final String message, final FilePathPickCallback filePickCallback) {
		this(activity, message, new String[]{}, filePickCallback);
	}
	
	public void show(){
		
		refreshFilesList();
	}
	
	/**
	 * Updates the list view to the current directory
	 */
	protected void refreshFilesList() {
		// Clear the files ArrayList
		
		FetchFilesTask fft = new FetchFilesTask(mActivity,acceptedFileExtensions) {
			
			@Override
			public void publishResult(ArrayList<String> result) {
				
				// Set the ListAdapter
				mAdapter = new FilePickerListAdapter(mActivity.getBaseContext(), result);
				
				mAdapter.notifyDataSetChanged();
				
				showAlert();
			}
		};
		fft.execute();

	}
	public void showAlert(){
		
		
		LayoutInflater inflator = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final View view = inflator.inflate(R.layout.filepick_selection, null);
		final ListView lv = (ListView) view.findViewById(R.id.waypoint_listview);

		
		final TextView message_tv = (TextView) view.findViewById(R.id.message_tv);
		message_tv.setText(mMessage);

		final View emptyView = inflator.inflate(R.layout.file_picker_empty, null);
		lv.setEmptyView(emptyView);
		lv.setAdapter(mAdapter);
		lv.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				
				String newPath  = (String) parent.getItemAtPosition(position);

				mFilePathPickCallback.filePathPicked(newPath);						
				currentAlert.dismiss();

			}
		});
		
		final Button cancelButton = (Button) view.findViewById(R.id.cancelButton);
		cancelButton.setOnClickListener(new View.OnClickListener(){

			@Override
			public void onClick(View v) {
				currentAlert.dismiss();
			}
			
		});
		
		
		final AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
		builder.setIcon(R.drawable.ic_launcher)
		.setTitle(mActivity.getString(R.string.app_name))
		.setMessage(mMessage)
		.setView(view);

		currentAlert = builder.create();
		currentAlert.show();
	}
	/**
	 * sets the properties of the view of a file in the listview
	 */
	private class FilePickerListAdapter extends ArrayAdapter<String> {

		private List<String> mObjects;

		public FilePickerListAdapter(Context context, List<String> objects) {
			super(context, R.layout.file_picker_list_item, android.R.id.text1, objects);
			mObjects = objects;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			View row = null;

			if(convertView == null) { 
				LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				row = inflater.inflate(R.layout.file_picker_list_item, parent, false);
			} else {
				row = convertView;
			}

			String path = mObjects.get(position);
			
			File file = new File(path);
			
			float fileSize = file.length() / 1048576f;

			//ImageView imageView = (ImageView)row.findViewById(R.id.file_picker_image);
			TextView textView = (TextView)row.findViewById(R.id.file_picker_text);
			TextView sizetextView = (TextView)row.findViewById(R.id.file_picker_size);
			sizetextView.setText(Float.toString(Math.round(fileSize * 100)/100f )+ " MB");
			// Set single line
			textView.setSingleLine(true);
			sizetextView.setSingleLine(true);

			final String fileName = path.substring(path.lastIndexOf("/") + 1);
			textView.setText(fileName);


			return row;
		}

	}
	
	public interface FilePathPickCallback {
		
		public void filePathPicked(final String filePath);

	}

}
