package de.rooehler.rastertheque.core;

import de.rooehler.rastertheque.core.model.NoData;

/**
 * A band or component of a raster dataset.
 */
public interface Band {

    /**
     * Enumeration for color interpretation.
     */
    static enum Color {
        UNDEFINED, GRAY, RED, GREEN, BLUE, OTHER;
    }

    /**
     * The name of the band.
     */
    String name();

    /**
     * Returns the numeric type of data stored in the band.
     */
    DataType datatype();
    
    /**
     * Returns the color interpretation of this band.
     * @return
     */
    Color color();
    
    /**
     * The nodata value of the band.
     *
     * @return The value or <tt>null</tt> if the band has no nodata value.
     */
    NoData nodata();

}
