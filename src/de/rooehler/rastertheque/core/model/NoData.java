package de.rooehler.rastertheque.core.model;

public class NoData {
	
    /**
     * default precision for no data values.
     */
    public static final double DEFAULT_TOLERANCE = 1E-8;

    /**
     * No NoData, always returns input value.
     */
    public static final NoData NONE = new NoData(0, 0) {
        @Override
        public Double valueOrNull(Double val) {
            return val;
        }
    };

    /**
     * Creates a new NoData helper with the default matching tolerance.
     *
     * @see {@link #create(Double, Double)}
     */
    public static NoData create(Double value) {
        return create(value, DEFAULT_TOLERANCE);
    }

    /**
     * Creates a new NoData helper with the specified matching tolerance.
     *
     * @param value The no data value. If this argument is <tt>null</tt> or <tt>NaN</tt>
     *   this method will create an no-op instance that never matches any value.
     */
    public static NoData create(Double value, Double tol) {
        return value != null ? new NoData(value, tol) : new NoData(Double.NaN, Double.NaN) {
            @Override
            public Double valueOrNull(Double val) {
                return val;
            }
        };
    }

    final double value;
    final double tol;

    public NoData(double value, double tol) {
        this.value = value;
        this.tol = tol;
    }

    /**
     * Determines if the specified value matches the nodata value.
     * <p>
     * This method will return <tt>null</tt> if the value matches or the specified
     * value is <tt>null</tt>. Otherwise it will return the original value.
     * </p>
     */
    public Double valueOrNull(Double val) {
        return val == null || Math.abs(val - value) <= tol ? null : val;
    }

}
