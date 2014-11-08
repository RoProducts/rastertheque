package de.rooehler.rastertheque.colormap;

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

public class SLDColorMapParser {
	
	private final static String TAG = SLDColorMapParser.class.getSimpleName();
	
	
	public static ColorMap parseColorMapFile(final File file){
		
		ArrayList<ColorMapEntry> colors = new ArrayList<ColorMapEntry>();
		
		Pair<Double,Integer> noData = null;
		double min = Double.MAX_VALUE;
		try {

			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(file);

			//read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
			doc.getDocumentElement().normalize();

			NodeList nList = doc.getElementsByTagName("ColorMapEntry");
			//TODO parse independent from namespace ?!
			if(nList.getLength() == 0){
				nList = doc.getElementsByTagName("sld:ColorMapEntry");
			}

			for (int i = 1; i < nList.getLength(); i++) {

				Node lowerNode = nList.item(i - 1);

				if (!lowerNode.hasAttributes()) {
					throw new IllegalArgumentException("no color attributes found");
				}

				// get attributes names and values
				NamedNodeMap lowerNodeMap = lowerNode.getAttributes();

				int lowerColor = Color.parseColor(lowerNodeMap.getNamedItem("color").getNodeValue());
				double lowerValue = Double.parseDouble(lowerNodeMap.getNamedItem("quantity").getNodeValue());
				double lowerOpacity = 1.0d;
				

				try{
					lowerOpacity = Double.parseDouble(lowerNodeMap.getNamedItem("opacity").getNodeValue());
				}catch(NumberFormatException | NullPointerException e){		}

				String label = null;

				try{
					label = lowerNodeMap.getNamedItem("label").getNodeValue();
					if(label.equals("nodata")){

						noData = new Pair<Double, Integer>(lowerValue, lowerColor);
						continue;
					}
				}catch( NullPointerException e){ }
				
				if(min == Double.MAX_VALUE){
					min = lowerValue;
				}
				
				Node higherNode = nList.item(i);
				NamedNodeMap higherNodeMap = higherNode.getAttributes();
				
				int higherColor = Color.parseColor(higherNodeMap.getNamedItem("color").getNodeValue());
				double higherValue = Double.parseDouble(higherNodeMap.getNamedItem("quantity").getNodeValue());
				double higherOpacity = 1.0d;

				try{
					higherOpacity = Double.parseDouble(higherNodeMap.getNamedItem("opacity").getNodeValue());
				}catch(NumberFormatException | NullPointerException e){		}
				
				int lR = (lowerColor >> 16) & 0x000000FF;
				int lG = (lowerColor >> 8 ) & 0x000000FF;
				int lB = (lowerColor)       & 0x000000FF;
				
				int hR = (higherColor >> 16) & 0x000000FF;
	    		int hG = (higherColor >> 8 ) & 0x000000FF;
	    		int hB = (higherColor)       & 0x000000FF;
	    		
	    		
	    		//can be negative
	    		int slope = Math.abs(( int ) (higherValue - lowerValue)) - 1;
	    		
	    		float redSlope = (float) (Math.max(hR,lR) - Math.min(hR, lR)) / slope;
	    		float greenSlope = (float) (Math.max(lG,hG) - Math.min(lG, hG)) / slope;
	    		float blueSlope = (float) (Math.max(lB,hB) - Math.min(lB, hB)) / slope;

	    		colors.add(new ColorMapEntry(lowerColor, lowerValue, lowerOpacity, label));	
	    		
	    		for(int j = 1; j < slope; j++){
	    			
	    			int newColor =  0xff000000 |
	    					((((int) (lR - (j * redSlope))) << 16) & 0xff0000) |
	    					((((int) (lG - (j * greenSlope))) << 8) & 0xff00) |
	    					((int)   (lB - (j * blueSlope)));
	    			
	    			colors.add(new ColorMapEntry(newColor, lowerValue + j, lowerOpacity, label));	
	    			
	    		}
	    		colors.add(new ColorMapEntry(higherColor, higherValue, higherOpacity, label));	

//				Log.i(TAG, "adding entry with color " + lowerNodeMap.getNamedItem("color").getNodeValue() + " value : "+lowerValue);


			}	

		} catch (Exception e) {
			Log.e(TAG,"error parsing", e);
		}
		
	    return new ColorMap(colors,min,noData);
	}
}
