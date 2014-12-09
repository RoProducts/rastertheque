package de.rooehler.raster_jai;
import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;


public class DisplayDEMApp extends JFrame implements MouseMotionListener
{
	private DisplayDEM dd; // An instance of the DisplayDEM component.
	private JLabel label; // Label to display information about the image.

	public DisplayDEMApp(PlanarImage image)
	{
		setTitle("Move the mouse over the image !");
		getContentPane().setLayout(new BorderLayout());
		dd = new DisplayDEM(image); // Create the component.
		getContentPane().add(new JScrollPane(dd),BorderLayout.CENTER);
		label = new JLabel("---"); // Create the label.
		getContentPane().add(label,BorderLayout.SOUTH);
		dd.addMouseMotionListener(this); // Register mouse events.
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(600,600);
		setVisible(true);
	}

	//This method is here just to satisfy the MouseMotionListener interface.
	public void mouseDragged(MouseEvent e) { }

	// This method will be executed when the mouse is moved over the
	// application.
	public void mouseMoved(MouseEvent e)
	{
		label.setText(dd.getPixelInfo()); // Update the label with the
		// DisplayDEM instance info.
	}

	public static void main(String[] args)
	{
		String imagePath = "/Users/robertoehler/Documents/Raster/GRAY_50M_SR_OB/GRAY_50M_SR_OB.tif";
		
		PlanarImage image = JAI.create("fileload", imagePath);
		new DisplayDEMApp(image);
	}
}

