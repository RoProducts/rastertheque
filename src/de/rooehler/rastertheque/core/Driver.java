/* Copyright 2013 The jeo project. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.rooehler.rastertheque.core;

import java.io.IOException;


public interface Driver {

    /**
     * Name identifying the driver.
     * <p>
     * This name should be no more than a few words (ideally one). It isn't meant to be a 
     * description but should be human readable. 
     * </p>
     */
    String getName();

    /**
     * Determines if this driver can open a connection to a given file
     * @param file the file to open
     * 
     * @return True if the driver can open the file, otherwise false.
     */
    boolean canOpen(String filePath);

    /**
     * Opens a connection to data described by the specified options.
     * 
     * @param opts Options describing the data to connect to.
     * 
     * @return The data.
     * 
     * @throws IOException In the event of a connection error such as a file system error or 
     *   database connection failure. 
     */
    Dataset open(String filePath) throws IOException;
}
