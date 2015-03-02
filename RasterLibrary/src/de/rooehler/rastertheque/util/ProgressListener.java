package de.rooehler.rastertheque.util;
/**
 * an interface which can be used to report the progress
 * of an operation
 * 
 * @author Robert Oehler
 *
 */
public interface ProgressListener {
	
	void onProgress(int percent);

}
