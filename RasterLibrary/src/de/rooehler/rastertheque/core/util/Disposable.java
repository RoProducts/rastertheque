package de.rooehler.rastertheque.core.util;

import java.io.Closeable;

/**
 * Interface for objects that should be closed after usage.
 *
 */
public interface Disposable extends Closeable {

    /**
     * Disposes the object. 
     */
    void close();
}
