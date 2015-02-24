package de.rooehler.rastertheque.processing.rendering;

import java.io.File;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.graphics.Color;
import android.util.Log;
import android.util.Pair;
/**
 * class to parse a SLD file to a colormap object
 * 
 * @author Robert Oehler
 *
 */
public class SLDColorMapParser {

	private final static String TAG = SLDColorMapParser.class.getSimpleName();

	/**
	 * parses a sld file to a colormap object
	 * @param file the sld colormap file
	 * @return a colormap containing the color values in ascending order like in the file
	 */
	public static ColorMap parseRawColorMapEntries(final File file){
		
		String name = file.getName();
		int lastIndexOf = name.lastIndexOf(".");
		if (lastIndexOf == -1 || (!name.substring(lastIndexOf + 1).equals("sld"))) {
		      Log.e(TAG, "provided file is not a sld file");
		      return null;
		}

		ArrayList<ColorMapEntry> colors = new ArrayList<ColorMapEntry>();

		Pair<Double,Integer> noData = null;
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		try {

			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(file);

			//read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
			doc.getDocumentElement().normalize();

			NodeList nList = doc.getElementsByTagName("ColorMapEntry");
			if(nList.getLength() == 0){
				nList = doc.getElementsByTagName("sld:ColorMapEntry");
			}

			for (int i = 0; i < nList.getLength(); i++) {

				Node node = nList.item(i);

				if (!node.hasAttributes()) {
					throw new IllegalArgumentException("no color attributes found");
				}

				// get attributes names and values
				NamedNodeMap nodeMap = node.getAttributes();

				int color = Color.parseColor(nodeMap.getNamedItem("color").getNodeValue());
				double value = Double.parseDouble(nodeMap.getNamedItem("quantity").getNodeValue());
				double opacity = 1.0d;

				try{
					opacity = Double.parseDouble(nodeMap.getNamedItem("opacity").getNodeValue());
				}catch(NumberFormatException | NullPointerException e){		}

				String label = null;

				try{
					label = nodeMap.getNamedItem("label").getNodeValue();
					if(label.equals("nodata")){

						noData = new Pair<Double, Integer>(value, color);
						continue;
					}
				}catch( NullPointerException e){ }

				if(value < min){
					min = value;
				}
				if(value > max){
					max = value;
				}

				colors.add(new ColorMapEntry(color, value, opacity, label));	
			}	

		} catch (Exception e) {
			Log.e(TAG,"error parsing", e);
		}

		return new ColorMap(colors, min, max, noData, false);
	}
	/**
	 * interpolates the colormap values to map each raster value
	 * to a color
	 * @param map the original colormap
	 * @return a colormap containing a color for each raster value
	 */
	public static ColorMap applyInterpolation(final ColorMap map){

		ArrayList<ColorMapEntry> newColors = new ArrayList<ColorMapEntry>();
		
		//takes two values, hence starts with n+1
		for (int i = 1; i < map.size(); i++) {

			ColorMapEntry lowerEntry = map.getEntries().get(i - 1); 

			ColorMapEntry higherEntry = map.getEntries().get(i); 

			int lowerColor = lowerEntry.getColor();
			int higherColor = higherEntry.getColor();

			double lowerValue = lowerEntry.getValue();
			double higherValue = higherEntry.getValue();

			final int lR = (lowerColor >> 16) & 0x000000FF;
			final int lG = (lowerColor >> 8 ) & 0x000000FF;
			final int lB = (lowerColor)       & 0x000000FF;

			final int hR = (higherColor >> 16) & 0x000000FF;
			final int hG = (higherColor >> 8 ) & 0x000000FF;
			final int hB = (higherColor)       & 0x000000FF;

			final int rMin = Math.min(hR, lR);
			final int gMin = Math.min(lG, hG);
			final int bMin = Math.min(lB, hB);

			final int rMax = Math.max(hR, lR);
			final int gMax = Math.max(lG, hG);
			final int bMax = Math.max(lB, hB);

			//can be negative
			final int slope = Math.abs(( int ) (higherValue - lowerValue)) - 1;

			final float redSlope =   (float) (rMax - rMin) / slope;
			final float greenSlope = (float) (gMax - gMin) / slope;
			final float blueSlope =  (float) (bMax - bMin) / slope;

			newColors.add(new ColorMapEntry(lowerColor, lowerValue, lowerEntry.getOpacity(), lowerEntry.getLabel()));	
			
			//calculates and sets color for the values between lower and higer value
			for(int j = 1; j < slope; j++){
				//TODO apply opacity
				int newColor =  0xff000000 |
						((((int) (rMax - (j * redSlope))) << 16) & 0xff0000) |
						((((int) (gMax - (j * greenSlope))) << 8) & 0xff00) |
						((int)   (bMax - (j * blueSlope)));

				newColors.add(new ColorMapEntry(newColor, lowerValue + j, lowerEntry.getOpacity(), lowerEntry.getLabel()));	

			}
			newColors.add(new ColorMapEntry(higherColor, higherValue, higherEntry.getOpacity(), higherEntry.getLabel()));

		}	


		return new ColorMap(newColors,map.getMinValue(),map.getMaxValue(),map.getNoData(), true);
	}
}
