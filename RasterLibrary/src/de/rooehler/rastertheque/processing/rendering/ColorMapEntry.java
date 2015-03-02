package de.rooehler.rastertheque.processing.rendering;

/**
 * A ColormapEntry contains color, value, opacity and optionally a label
 * for a color which is associated to a raster value
 * 
 * @author Robert Oehler
 */
public class ColorMapEntry{
	
	
	private int color;
	private double opacity;
	private double value;
	private String label;
	
	public ColorMapEntry(final int pColor, final double pValue, final double pOpacity, final String pLabel){
		
		this.color = pColor;
		this.opacity = pOpacity;
		this.value = pValue;
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

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}
	
}
