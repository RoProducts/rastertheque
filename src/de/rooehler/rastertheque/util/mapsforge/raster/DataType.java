package de.rooehler.rastertheque.util.mapsforge.raster;

/**
 * Numeric data type enumeration.
 */
public enum DataType {

    CHAR {
        @Override
        protected int bits() {
            return Character.SIZE;
        }
    },
    BYTE {
        @Override
        protected int bits() {
            return Byte.SIZE;
        }
    },
    SHORT {
        @Override
        protected int bits() {
            return Short.SIZE;
        }
    },
    INT {
        @Override
        protected int bits() {
            return Integer.SIZE;
        }
    },
    LONG {
        @Override
        protected int bits() {
            return Long.SIZE;
        }
    },
    FLOAT {
        @Override
        protected int bits() {
            return Float.SIZE;
        }
    },
    DOUBLE {
        @Override
        protected int bits() {
            return Double.SIZE;
        }
    };

    /**
     * The size of the datatype in bytes.
     */
    public int size() {
        return bits() / 8;
    }

    /**
     * The size of the datatype in bits.
     */
    protected abstract int bits();
}