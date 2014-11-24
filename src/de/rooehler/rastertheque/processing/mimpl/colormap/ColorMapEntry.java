package de.rooehler.rastertheque.processing.mimpl.colormap;

public class ColorMapEntry{
	
	
	private int color;
	private double opacity;
	private double quantity;
	private String label;
	
	public ColorMapEntry(final int pColor,  final double pQuantity,final double pOpacity, final String pLabel){
		
		this.color = pColor;
		this.opacity = pOpacity;
		this.quantity = pQuantity;
		this.label = pLabel;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public int getColor() {
		return color;
	}

	public void setColor(int color) {
		this.color = color;
	}

	public double getOpacity() {
		return opacity;
	}

	public void setOpacity(double opacity) {
		this.opacity = opacity;
	}

	public double getQuantity() {
		return quantity;
	}

	public void setQuantity(double quantity) {
		this.quantity = quantity;
	}
	
}
