//package de.rooehler.rastertheque.processing.rendering;
//
//import java.io.EOFException;
//import java.io.File;
//import java.io.IOException;
//import java.io.Serializable;
//import java.nio.ByteBuffer;
//import java.nio.ByteOrder;
//import java.util.Map;
//
//import android.opengl.GLSurfaceView.Renderer;
//import android.util.Log;
//import de.rooehler.rastertheque.core.DataType;
//import de.rooehler.rastertheque.core.Raster;
//import de.rooehler.rastertheque.core.util.ByteBufferReader;
//import de.rooehler.rastertheque.core.util.ByteBufferReaderUtil;
//import de.rooehler.rastertheque.util.Constants;
//import de.rooehler.rastertheque.util.Hints;
//import de.rooehler.rastertheque.util.Hints.Key;
//import de.rooehler.rastertheque.util.ProgressListener;
//
//public class MRenderer implements Serializable{
//	
//	private static final long serialVersionUID = 856937549115836638L;
//
//	
//	public MRenderer(final String pFilePath, final boolean useColorMapIfAvailable){
//		
//		
//	}
//	
//	/**
//	 * render the data contained in @param buffer
//	 * Currently this will, depending on the data
//	 * <ol>
//	 *   <li>if the raster contains 3 bands R, G and B use these bands to render</li>
//	 *   <li>if there is an according colormap to this raster file use this colormap to render</li>
//	 *   <li>if none of the before will interpolate a gray scale image</li>
//	 * </ol>  
//	 *   
//	 * @param raster, containing the data to render
//	 */
//
//	public int[] render(final Raster raster, Map <Key,Serializable> params, Hints hints, ProgressListener listener) {
//		
//		Object rendering = null;
//		if(hints != null && hints.containsKey(Hints.KEY_SYMBOLIZATION)){
//			
//			rendering = params.get(Hints.KEY_SYMBOLIZATION);
//		}
//		
//		boolean rgbBands = false;
//		
//		if(params != null && params.containsKey(Hints.KEY_RGB_BANDS)){
//			rgbBands = Boolean.valueOf( (String) params.get(Hints.KEY_RGB_BANDS));
//		}
//		
//		int[] pixels = null;
//		
//		if(rgbBands){
//			pixels = rgbBands(raster);
//		}	
//		
//		return pixels;
//	}
//
//	private int[] rgbBands(final Raster raster) {
//		
//		final ByteBufferReader reader = new ByteBufferReader(raster.getData().array(), ByteOrder.nativeOrder());
//		final int pixelAmount = (int) raster.getDimension().getWidth() *  (int) raster.getDimension().getHeight();
//		
//		int [] pixels = new int[pixelAmount];
//		
//		double[] pixelsR = new double[pixelAmount];
//		double[] pixelsG = new double[pixelAmount];
//		double[] pixelsB = new double[pixelAmount];
//           
//		for (int i = 0; i < pixelAmount; i++) {	
//			pixelsR[i] =  ByteBufferReaderUtil.getValue(reader, raster.getBands().get(0).datatype());
//		}
//		for (int j = 0; j < pixelAmount; j++) {	
//			pixelsG[j] =  ByteBufferReaderUtil.getValue(reader, raster.getBands().get(1).datatype());
//		}
//		for (int k = 0; k < pixelAmount; k++) {	
//			pixelsB[k] =  ByteBufferReaderUtil.getValue(reader, raster.getBands().get(2).datatype());
//		}
//		
//        for (int l = 0; l < pixelAmount; l++) {	
//        	
//        	double r = pixelsR[l];
//        	double g = pixelsG[l];
//        	double b = pixelsB[l];
//        	
//        	pixels[l] = 0xff000000 | ((((int) r) << 16) & 0xff0000) | ((((int) g) << 8) & 0xff00) | ((int) b);
//        }
//        
//		return pixels;
//	}
//
//
//}
