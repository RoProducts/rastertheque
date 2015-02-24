package de.rooehler.rastertheque.core;

import java.io.IOException;

public interface Driver {

    /**
     * Name identifying the driver.
     */
    String getName();

    /**
     * Determines if this driver can open a connection to a file
     * @param filePath to the file to open
     * 
     * @return true if the driver can open the file, otherwise false.
     */
    boolean canOpen(String filePath);

    /**
     * Opens a connection to a file 
     * 
     * @return a dataset
     * 
     * @throws IOException a file system error or database connection failure. 
     */
    Dataset open(String filePath) throws IOException;
}
