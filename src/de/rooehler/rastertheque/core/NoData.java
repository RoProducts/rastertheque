package de.rooehler.rastertheque.core;

public class NoData {

	/**
	 * default precision for no data values.
	 */
	public static final double DEFAULT_TOLERANCE = 1E-8;

	/**
	 * No NoData, always returns input value.
	 */
	public static final NoData NONE = new NoData(0, 0);

	/**
	 * Creates a new NoData helper with the default matching tolerance.
	 */
	public static NoData create(Double value) {
		return new NoData(value, DEFAULT_TOLERANCE);
	}
	
	public static NoData noDataForDataType(DataType type){
		switch(type){
		case BYTE:
			return new NoData(Byte.MIN_VALUE, DEFAULT_TOLERANCE);
			
		case CHAR:
			return new NoData(Character.MIN_VALUE, DEFAULT_TOLERANCE);
			
		case DOUBLE:
			return new NoData(Double.MIN_VALUE, DEFAULT_TOLERANCE);
			
		case FLOAT:
			return new NoData(Float.MIN_VALUE, DEFAULT_TOLERANCE);
			
		case INT:
			return new NoData(Integer.MIN_VALUE, DEFAULT_TOLERANCE);
			
		case LONG:
			return new NoData(Long.MIN_VALUE, DEFAULT_TOLERANCE);
			
		case SHORT:
			return new NoData(Short.MIN_VALUE, DEFAULT_TOLERANCE);
			
		default:
			break;
			
		}
		return NoData.NONE;
	}

	final double value;
	final double tol;

	public NoData(double value, double tol) {
		this.value = value;
		this.tol = tol;
	}


	public double getValue(){
		return value;
	}

}
