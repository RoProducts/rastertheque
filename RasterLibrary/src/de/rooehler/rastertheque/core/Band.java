package de.rooehler.rastertheque.core;

import de.rooehler.rastertheque.processing.rendering.ColorMap;


/**
 * A band is a component of a raster dataset.
 */
public interface Band {

    /**
     * Possible color interpretation of this band
     */
    static enum Color {
        UNDEFINED, GRAY, RED, GREEN, BLUE, OTHER;
    }

    /**
     * The name of the band
     */
    String name();

    /**
     * Returns the data type of this band
     */
    DataType datatype();
    
    /**
     * Returns the color interpretation of this band.
     * @return
     */
    Color color();
    
    /**
     * Returns the colorMap for this band if available
     * @return
     */
    ColorMap colorMap();
    
    /**
     * The nodata value of the band
     *
     * @return The value or NoData.None if the band has no nodata value yet
     */
    NoData nodata();

}
