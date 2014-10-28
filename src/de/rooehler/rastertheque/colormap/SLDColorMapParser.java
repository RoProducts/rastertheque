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

public class SLDColorMapParser {
	
	private final static String TAG = SLDColorMapParser.class.getSimpleName();
	
	
	public static ColorMap parseColorMapFile(final File file){
		
		ArrayList<ColorMapEntry> colors = new ArrayList<ColorMapEntry>();
		
		try {

			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(file);

			//read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
			doc.getDocumentElement().normalize();

			NodeList nList = doc.getElementsByTagName("sld:ColorMapEntry");

			for (int temp = 0; temp < nList.getLength(); temp++) {

				Node nNode = nList.item(temp);

				if (nNode.hasAttributes()) {

					// get attributes names and values
					NamedNodeMap nodeMap = nNode.getAttributes();

					colors.add(new ColorMapEntry(Color.parseColor(
							nodeMap.item(0).getNodeValue()),
							Float.parseFloat(nodeMap.item(1).getNodeValue()),
							Float.parseFloat(nodeMap.item(2).getNodeValue())));	
				}
			}	

		} catch (Exception e) {
			Log.e(TAG,"error parsing", e);
		}
		
	    return new ColorMap(colors);
	}
}
