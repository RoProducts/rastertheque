package de.rooehler.mapsforgerenderer.dialog;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import de.rooehler.mapsforgerenderer.R;
/**
 * Asynctask which crawls the file system / queries the Android MediaStore database
 * to retrieve all files on the system with specified extensions @param mExtensions
 * 
 * @author Robert Oehler
 *
 */
public abstract class FetchFilesTask extends AsyncTask<Void, Void,ArrayList<String>>{
	
	private final static String TAG = FetchFilesTask.class.getSimpleName();
	
	private ProgressDialog pd = null; 
	private Activity activity;
	private String[] mExtensions;
	
	public abstract void publishResult(final ArrayList<String> result);
	
	public FetchFilesTask(Activity act, final String[] pExtensions){
		this.activity  = act;
		this.mExtensions = pExtensions;
	}
    
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		pd = new ProgressDialog(activity);
		pd.setTitle(activity.getString(R.string.searching));
		pd.setCancelable(false);
		pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		pd.setIcon(R.drawable.ic_launcher);
		pd.show();
	}

	@SuppressLint("NewApi")
	@Override
	protected ArrayList<String> doInBackground(Void... params) {
		ArrayList<String> tempResult = new ArrayList<String>();
		try{
			
			//crawl some folder
			tempResult.addAll(walkdir(new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/rastertheque/"), 2));
			
			//this would be more generic but may last a while
//			tempResult.addAll(walkdir(new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/"), 1));

			//query the MediaStore
			ContentResolver cr = activity.getContentResolver();
			Uri myUri = MediaStore.Files.getContentUri("external");
			String selection  = MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_NONE;
			String[] projection = new String[] { MediaStore.Files.FileColumns.DATA };

			Cursor cursor = cr.query(myUri, projection, selection, null, null);
			if((cursor.getCount() == 0) || !cursor.moveToFirst()){//empty)

			}else{
				do{			
					String path = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA));
					if(mExtensions != null && mExtensions.length > 0){
						String extension = path.substring(path.lastIndexOf(".") + 1, path.length());
						for(int i = 0; i < mExtensions.length;i++){
							final String target = mExtensions[i];
							if(extension.equals(target)){
								File file = new File(path);
								if(!tempResult.contains(path) && file.exists()){
									tempResult.add(path);
									Log.d(TAG, "adding path : "+path);
								}
							}
						}
					}else{
						tempResult.add(path);
					}
				}while(cursor.moveToNext());
			}
			if(cursor != null)
				cursor.close();
			
		}catch(Exception e){
			Log.e(TAG, "error onbackground",e);	
		}
		return tempResult;
	}

	@Override
	protected void onPostExecute(ArrayList<String> result) {
		super.onPostExecute(result);
		try{

			Collections.sort(result, new MyFileNameComparator());
			
			publishResult(result);

		}catch(Exception e){
			Log.e(TAG, "error onPostexecute",e);
		}
		try{
			if(pd != null && pd.isShowing())
				pd.dismiss();
		}catch(Exception e){
			Log.e(TAG, "error removing pd");
		}

	}
	/**
	 * compares fileName to sort and retrieve a list which is alphabetically sorted
	 */
	public class MyFileNameComparator implements Comparator<String>{


		@Override
	    public int compare(String s1, String s2) {
	    	
	    	final String fileName1 = s1.substring(s1.lastIndexOf("/") + 1, s1.length()).toLowerCase(Locale.getDefault());
	    	final String fileName2 = s2.substring(s2.lastIndexOf("/") + 1, s2.length()).toLowerCase(Locale.getDefault());
	        
	        return fileName1.compareTo(fileName2);
	    }
	}
	/**
	 * recursive method to walk through a file systems directory hierarchy
	 * @param dir the directory to start
	 * @param level the amount of steps to go up the hierarchy
	 * @return the list of files found according to mExtensions
	 */
	public ArrayList<String> walkdir(File dir, int level) {

		ArrayList<String> tempResult = new ArrayList<String>();
		
		if(level == 0)
			return tempResult;

		File listFile[] = dir.listFiles();

		if (listFile != null) {
			for (int i = 0; i < listFile.length; i++) {

				if (listFile[i].isDirectory()) {
					walkdir(listFile[i],--level);
				} else {
					if(mExtensions != null && mExtensions.length > 0){
						for(int j = 0; j < mExtensions.length;j++){
							final String target = mExtensions[j];
							if (listFile[i].getName().endsWith(target)){					
								tempResult.add(listFile[i].getPath());
							}
						}
					}else{
						tempResult.add(listFile[i].getPath());
					}
				}
			}
		} 
		return tempResult;
	}
 }
