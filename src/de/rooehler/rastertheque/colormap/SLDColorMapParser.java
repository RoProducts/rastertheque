package de.rooehler.rastertheque.colormap;

import java.io.File;
import java.util.NavigableMap;
import java.util.TreeMap;

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
		
		NavigableMap<Double,ColorMapEntry> colors = new TreeMap<Double,ColorMapEntry>();
		
		Pair<Double,Integer> noData = null;
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

			for (int temp = 0; temp < nList.getLength(); temp++) {

				Node nNode = nList.item(temp);

				if (nNode.hasAttributes()) {

					// get attributes names and values
					NamedNodeMap nodeMap = nNode.getAttributes();
					
					int color = Color.parseColor(nodeMap.getNamedItem("color").getNodeValue());
					double quantity = Double.parseDouble(nodeMap.getNamedItem("quantity").getNodeValue());
					double opacity = 1.0d;
					
					try{
						opacity = Double.parseDouble(nodeMap.getNamedItem("opacity").getNodeValue());
					}catch(NumberFormatException | NullPointerException e){		}

					String label = null;
					boolean add = true;
					try{
						label = nodeMap.getNamedItem("label").getNodeValue();
						if(label.equals("nodata")){
							add = false;
							noData = new Pair<Double, Integer>(quantity, color);
						}
					}catch( NullPointerException e){ }
					if(add){	
						Log.i(TAG, "adding entry with color " + nodeMap.getNamedItem("color").getNodeValue() + " value : "+quantity);
						colors.put(quantity, new ColorMapEntry(color, quantity, opacity, label));	
					}
				}
			}	

		} catch (Exception e) {
			Log.e(TAG,"error parsing", e);
		}
		
	    return new ColorMap(colors,noData);
	}
}
