package de.rooehler.rastertheque.core;

public class Dimension {
	
	private int width;
	private int height;

	public Dimension(int width, int height) {
		
		this.width = width;
		this.height = height;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}
	
	public int getSize(){
		return width * height;
	}

}
