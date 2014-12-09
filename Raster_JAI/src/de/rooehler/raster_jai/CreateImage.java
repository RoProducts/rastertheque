package de.rooehler.raster_jai;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Point;
import java.awt.Transparency;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.io.File;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.TiledImage;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import com.sun.media.jai.widget.DisplayJAI;


public class CreateImage {
	
	public static String JAI_PROJECT = "JAI";
	
	public static void main(String[] args)
	 {
		String image = createGrayImage();

		imageInfo(image);
		
		displayImage(image); 
	 }

	
	public static  String createGrayImage(){
		 int width = 1024; int height = 1024; // Dimensions of the image.

		 float[] imageData = new float[width*height]; // Image data array.

		 int count = 0; // Auxiliary counter.

		 for(int w=0;w<width;w++){ // Fill the array with a degradé pattern.

			 for(int h=0;h<height;h++){

				 imageData[count++] = (float)(Math.sqrt(w+h));

			 }
		 }
		 // Create a DataBuffer from the values on the image array.
		 javax.media.jai.DataBufferFloat dbuffer = new javax.media.jai.DataBufferFloat(imageData,width*height);

		 // Create a float data sample model.
		 SampleModel sampleModel = RasterFactory.createBandedSampleModel(DataBuffer.TYPE_FLOAT, width,height,1);

		 // Create a compatible ColorModel.
		 ColorModel colorModel = PlanarImage.createColorModel(sampleModel);

		 // Create a WritableRaster.
		 Raster raster = RasterFactory.createWritableRaster(sampleModel,dbuffer, new Point(0,0));

		 // Create a TiledImage using the float SampleModel.
		 TiledImage tiledImage = new TiledImage(0,0,width,height,0,0,sampleModel,colorModel);

		 // Set the data of the tiled image to be the raster.
		 tiledImage.setData(raster);

		 String fileName = "floatpattern.tif";
		 // Save the image on a file.
		 JAI.create("filestore",tiledImage,"floatpattern.tif","TIFF");
		 
		 return fileName;
	}
	public static String createRGBImage(){

		int width = 1024; int height = 1024; // Dimensions of the image
		byte[] data = new byte[width*height*3]; // Image data array.
		int count = 0; // Temporary counter.
		
		for(int w=0;w<width;w++){ // Fill the array with a pattern.
			for(int h=0;h<height;h++)
			{
				data[count+0] = (count % 2 == 0) ? (byte)255: (byte) 0;
				data[count+1] = 0;
				data[count+2] = (count % 2 == 0) ? (byte) 0: (byte)255;
				count += 3;
			}
		}
		// Create a Data Buffer from the values on the single image array.
		DataBufferByte dbuffer = new DataBufferByte(data,width*height*3);
		// Create an pixel interleaved data sample model.
		SampleModel sampleModel = RasterFactory.createPixelInterleavedSampleModel(DataBuffer.TYPE_BYTE,	width,height,3);
		// Create a compatible ColorModel.
		ColorModel colorModel = PlanarImage.createColorModel(sampleModel);
		// Create a WritableRaster.
		Raster raster = RasterFactory.createWritableRaster(sampleModel,dbuffer,	new Point(0,0));
		// Create a TiledImage using the SampleModel.
		TiledImage tiledImage = new TiledImage(0,0,width,height,0,0,sampleModel,colorModel);
		// Set the data of the tiled image to be the raster.
		tiledImage.setData(raster);
		// Save the image on a file.
		
		String fileName = "rgbPattern.tif";
		
		JAI.create("filestore",tiledImage,fileName,"TIFF");

		return fileName;

	}
	
	public static void imageInfo(String imagePath){
		// Open the image (using the name passed as a command line parameter)
		PlanarImage pi = JAI.create("fileload", imagePath);
		// Get the image file size (non-JAI related).
		File image = new File(imagePath);
		System.out.println("Image file size: "+image.length()+" bytes.");
		// Show the image dimensions and coordinates.
		System.out.print("Dimensions: ");
		System.out.print(pi.getWidth()+"x"+pi.getHeight()+" pixels");
		// Remember getMaxX and getMaxY return the coordinate of the next point!
		System.out.println(" (from "+pi.getMinX()+","+pi.getMinY()+" to " +
				(pi.getMaxX()-1)+","+(pi.getMaxY()-1)+")");
		if ((pi.getNumXTiles() != 1)||(pi.getNumYTiles() != 1)) // Is it tiled?

		{
			// Tiles number, dimensions and coordinates.
			System.out.print("Tiles: ");
			System.out.print(pi.getTileWidth()+"x"+pi.getTileHeight()+" pixels"+
					" ("+pi.getNumXTiles()+"x"+pi.getNumYTiles()+" tiles)");
			System.out.print(" (from "+pi.getMinTileX()+","+pi.getMinTileY()+
					" to "+pi.getMaxTileX()+","+pi.getMaxTileY()+")");
			System.out.println(" offset: "+pi.getTileGridXOffset()+","+
					pi.getTileGridXOffset());
		}
		// Display info about the SampleModel of the image.
		SampleModel sm = pi.getSampleModel();
		System.out.println("Number of bands: "+sm.getNumBands());
		System.out.print("Data type: ");
		switch(sm.getDataType())
		{
		case DataBuffer.TYPE_BYTE: System.out.println("byte"); break;
		case DataBuffer.TYPE_SHORT: System.out.println("short"); break;
		case DataBuffer.TYPE_USHORT: System.out.println("ushort"); break;
		case DataBuffer.TYPE_INT: System.out.println("int"); break;
		case DataBuffer.TYPE_FLOAT: System.out.println("float"); break;
		case DataBuffer.TYPE_DOUBLE: System.out.println("double"); break;
		case DataBuffer.TYPE_UNDEFINED:System.out.println("undefined"); break;
		}
		// Display info about the ColorModel of the image.
		ColorModel cm = pi.getColorModel();
		if (cm != null)
		{
			System.out.println("Number of color components: "+
					cm.getNumComponents());
			System.out.println("Bits per pixel: "+cm.getPixelSize());
			System.out.print("Transparency: ");
			switch(cm.getTransparency())
			{
			case Transparency.OPAQUE: System.out.println("opaque"); break;
			case Transparency.BITMASK: System.out.println("bitmask"); break;
			case Transparency.TRANSLUCENT:
				System.out.println("translucent"); break;
			}
		}else{
			System.out.println("No color model.");
		}
	}

	public static void displayImage(String fileName){
		// Load the image which file name was passed as the first argument to
		 // the application.
		 PlanarImage image = JAI.create("fileload",fileName);
		 // Get some information about the image
		String imageInfo =
		 "Dimensions: "+image.getWidth()+"x"+image.getHeight()+
		 " Bands:"+image.getNumBands();
		 // Create a frame for display.
		 JFrame frame = new JFrame();
		 frame.setTitle("DisplayJAI: "+fileName);
		 // Get the JFrame’s ContentPane.
		 Container contentPane = frame.getContentPane();
		 contentPane.setLayout(new BorderLayout());
		 // Create an instance of DisplayJAI.
		 DisplayJAI dj = new DisplayJAI(image);
		 // Add to the JFrame’s ContentPane an instance of JScrollPane
		 // containing the DisplayJAI instance.
		 contentPane.add(new JScrollPane(dj),BorderLayout.CENTER);
		 // Add a text label with the image information.
		 contentPane.add(new JLabel(imageInfo),BorderLayout.SOUTH);
		 // Set the closing operation so the application is finished.
		 frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		 frame.setSize(1000,1000); // adjust the frame size.
		 frame.setVisible(true); // show the frame.

	}
}
