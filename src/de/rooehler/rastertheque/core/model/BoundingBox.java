package de.rooehler.rastertheque.core.model;

public class BoundingBox {
	
	private double minX;
	private double minY;
	private double maxX;
	private double maxY;
	
	
	public BoundingBox(double minX, double minY, double maxX, double maxY) {

		this.minX = minX;
		this.minY = minY;
		this.maxX = maxX;
		this.maxY = maxY;
	}
	
	/**
	 * @return the minX
	 */
	public double getMinX() {
		return minX;
	}
	/**
	 * @return the minY
	 */
	public double getMinY() {
		return minY;
	}
	/**
	 * @return the maxX
	 */
	public double getMaxX() {
		return maxX;
	}
	/**
	 * @return the maxY
	 */
	public double getMaxY() {
		return maxY;
	}
	
	public Coordinate getCenter(){
		
		return new Coordinate((minX + maxX) / 2,(minY + maxY) / 2);
		
	}

}
