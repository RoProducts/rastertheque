package de.rooehler.rastertheque.colormap;

public class ColorMapEntry{
	
	
	private int color;
	private float opacity;
	private float quantity;
	
	public ColorMapEntry(final int pColor, final float pOpacity, final float pQuantity){
		
		this.color = pColor;
		this.opacity = pOpacity;
		this.quantity = pQuantity;
	}

	public int getColor() {
		return color;
	}

	public void setColor(int color) {
		this.color = color;
	}

	public float getOpacity() {
		return opacity;
	}

	public void setOpacity(float opacity) {
		this.opacity = opacity;
	}

	public float getQuantity() {
		return quantity;
	}

	public void setQuantity(float quantity) {
		this.quantity = quantity;
	}
	
}
