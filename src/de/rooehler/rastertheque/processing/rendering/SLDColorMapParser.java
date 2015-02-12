package de.rooehler.rastertheque.processing.rendering;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

	public static ColorMap parseRawColorMapEntries(final File file){


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
			//TODO parse independent from namespace ?!
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

	//	public static ColorMap parseColorMapFile(final File file){
	//		
	//		ArrayList<ColorMapEntry> colors = new ArrayList<ColorMapEntry>();
	//		
	//		Pair<Double,Integer> noData = null;
	//		double min = Double.MAX_VALUE;
	//		double max = Double.MIN_VALUE;
	//		try {
	//
	//			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	//			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	//			Document doc = dBuilder.parse(file);
	//
	//			//read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
	//			doc.getDocumentElement().normalize();
	//
	//			NodeList nList = doc.getElementsByTagName("ColorMapEntry");
	//			//TODO parse independent from namespace ?!
	//			if(nList.getLength() == 0){
	//				nList = doc.getElementsByTagName("sld:ColorMapEntry");
	//			}
	//
	//			for (int i = 1; i < nList.getLength(); i++) {
	//
	//				Node lowerNode = nList.item(i - 1);
	//
	//				if (!lowerNode.hasAttributes()) {
	//					throw new IllegalArgumentException("no color attributes found");
	//				}
	//
	//				// get attributes names and values
	//				NamedNodeMap lowerNodeMap = lowerNode.getAttributes();
	//
	//				int lowerColor = Color.parseColor(lowerNodeMap.getNamedItem("color").getNodeValue());
	//				double lowerValue = Double.parseDouble(lowerNodeMap.getNamedItem("quantity").getNodeValue());
	//				double lowerOpacity = 1.0d;
	//				
	//
	//				try{
	//					lowerOpacity = Double.parseDouble(lowerNodeMap.getNamedItem("opacity").getNodeValue());
	//				}catch(NumberFormatException | NullPointerException e){		}
	//
	//				String label = null;
	//
	//				try{
	//					label = lowerNodeMap.getNamedItem("label").getNodeValue();
	//					if(label.equals("nodata")){
	//
	//						noData = new Pair<Double, Integer>(lowerValue, lowerColor);
	//						continue;
	//					}
	//				}catch( NullPointerException e){ }
	//				
	//				if(lowerValue < min){
	//					min = lowerValue;
	//				}
	//				if(lowerValue > max){
	//					max = lowerValue;
	//				}
	//				
	//				Node higherNode = nList.item(i);
	//				NamedNodeMap higherNodeMap = higherNode.getAttributes();
	//				
	//				int higherColor = Color.parseColor(higherNodeMap.getNamedItem("color").getNodeValue());
	//				double higherValue = Double.parseDouble(higherNodeMap.getNamedItem("quantity").getNodeValue());
	//				double higherOpacity = 1.0d;
	//
	//				try{
	//					higherOpacity = Double.parseDouble(higherNodeMap.getNamedItem("opacity").getNodeValue());
	//				}catch(NumberFormatException | NullPointerException e){		}
	//				
	//				if(higherValue < min){
	//					min = higherValue;
	//				}
	//				if(higherValue > max){
	//					max = higherValue;
	//				}
	//				
	//				final int lR = (lowerColor >> 16) & 0x000000FF;
	//				final int lG = (lowerColor >> 8 ) & 0x000000FF;
	//				final int lB = (lowerColor)       & 0x000000FF;
	//				
	//				final int hR = (higherColor >> 16) & 0x000000FF;
	//				final int hG = (higherColor >> 8 ) & 0x000000FF;
	//				final int hB = (higherColor)       & 0x000000FF;
	//	    		
	//				final int rMin = Math.min(hR, lR);
	//				final int gMin = Math.min(lG, hG);
	//				final int bMin = Math.min(lB, hB);
	//				
	//				final int rMax = Math.max(hR, lR);
	//				final int gMax = Math.max(lG, hG);
	//				final int bMax = Math.max(lB, hB);
	//	    		
	//	    		//can be negative
	//				final int slope = Math.abs(( int ) (higherValue - lowerValue)) - 1;
	//	    		
	//				final float redSlope =   (float) (rMax - rMin) / slope;
	//				final float greenSlope = (float) (gMax - gMin) / slope;
	//				final float blueSlope =  (float) (bMax - bMin) / slope;
	//
	//	    		colors.add(new ColorMapEntry(lowerColor, lowerValue, lowerOpacity, label));	
	//	    		
	//	    		for(int j = 1; j < slope; j++){
	//	    			//TODO apply opacity
	//	    			int newColor =  0xff000000 |
	//	    					((((int) (rMax - (j * redSlope))) << 16) & 0xff0000) |
	//	    					((((int) (gMax - (j * greenSlope))) << 8) & 0xff00) |
	//	    					((int)   (bMax - (j * blueSlope)));
	//	    			
	//	    			colors.add(new ColorMapEntry(newColor, lowerValue + j, lowerOpacity, label));	
	//	    			
	//	    		}
	//	    		colors.add(new ColorMapEntry(higherColor, higherValue, higherOpacity, label));	
	//
	////				Log.i(TAG, "adding entry with color " + lowerNodeMap.getNamedItem("color").getNodeValue() + " value : "+lowerValue);
	//
	//
	//			}	
	//
	//		} catch (Exception e) {
	//			Log.e(TAG,"error parsing", e);
	//		}
	//		
	//	    return new ColorMap(colors,min,max,noData, true);
	//	}

	public static ColorMap applyInterpolation(final ColorMap map){

		ArrayList<ColorMapEntry> newColors = new ArrayList<ColorMapEntry>();
		try {


			for (int i = 1; i < map.size(); i++) {

				ColorMapEntry lowerEntry = map.getEntries().get(i - 1); 

				ColorMapEntry higherEntry = map.getEntries().get(i); 

				int lowerColor = lowerEntry.getColor();
				int higherColor = higherEntry.getColor();

				double lowerValue = lowerEntry.getQuantity();
				double higherValue = higherEntry.getQuantity();

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

		} catch (Exception e) {
			Log.e(TAG,"error parsing", e);
		}

		return new ColorMap(newColors,map.getMinValue(),map.getMaxValue(),map.getNoData(), true);
	}
}
