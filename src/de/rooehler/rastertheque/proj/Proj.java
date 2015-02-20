package de.rooehler.rastertheque.proj;


import java.io.IOException;
import java.io.InputStream;

import org.osgeo.proj4j.CRSFactory;
import org.osgeo.proj4j.CoordinateReferenceSystem;
import org.osgeo.proj4j.CoordinateTransform;
import org.osgeo.proj4j.CoordinateTransformFactory;
import org.osgeo.proj4j.ProjCoordinate;
import org.osgeo.proj4j.io.Proj4FileReader;

import android.annotation.SuppressLint;
import android.util.Log;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

@SuppressLint("DefaultLocale")
public class Proj {

	static CRSFactory csFactory = new CRSFactory();

	static CoordinateTransformFactory txFactory = new CoordinateTransformFactory();

	/**
	 * Google mercator
	 */
	public static final CoordinateReferenceSystem EPSG_900913 = Proj.crs(900913);



	public static CoordinateReferenceSystem crs(int epsg) {

		final String _epsg = "EPSG:" + epsg;

		if (_epsg.equalsIgnoreCase("epsg:900913")) {
			return EPSG_900913 != null ? EPSG_900913 : createFromExtra("epsg", "900913");
		}

		return csFactory.createFromName(_epsg);
	}
	
    /**
     * Creates a crs object from projection parameter definition.
     * 
     * @param projdef The projection / proj4 parameters.
     * 
     * @return The crs object.
     */
    public static CoordinateReferenceSystem crs(String... projdef) {
        if (projdef != null && projdef.length == 1) {
            return csFactory.createFromParameters(null, projdef[0]);
        }
        return csFactory.createFromParameters(null, projdef);
    }

	/**
	 * Reprojects an envelope between two coordinate reference systems.
	 * <p>
	 * In the event a transformation between the two crs objects can not be found this method throws
	 * {@link IllegalArgumentException}.
	 * 
	 * In the event the two specified coordinate reference systems are equal this method is a 
	 * no-op and returns the original envelope. 
	 * </p>
	 * @param e The envelope to reproject.
	 * @param from The source coordinate reference system.
	 * @param to The target coordinate reference system.
	 * 
	 * @return The reprojected envelope.
	 * 
	 * @throws IllegalArgumentException If no coordinate transform can be found.
	 */
	public static Envelope reproject(Envelope e, CoordinateReferenceSystem from, CoordinateReferenceSystem to) {

		CoordinateTransform tx = transform(from, to);

		Coordinate c1 = new Coordinate(e.getMinX(), e.getMinY());
		Coordinate c2 = new Coordinate(e.getMaxX(), e.getMaxY());
		
		ProjCoordinate p1 = new ProjCoordinate(c1.x, c1.y);
		ProjCoordinate p2 = new ProjCoordinate(c2.x, c2.y);
		
		tx.transform(p1, p1);
		tx.transform(p2, p2);
		
		c1.x = p1.x;
		c1.y = p1.y;
		
		c2.x = p2.x;
		c2.y = p2.y;

		return new Envelope(c1.x, c2.x, c1.y, c2.y);
	}

	public static CoordinateTransform transform(CoordinateReferenceSystem from, CoordinateReferenceSystem to) {


		CoordinateTransform tx = txFactory.createTransform(from, to);
		if (tx == null) {
			throw new IllegalArgumentException("Unable to find transform from " + from + " to " + to);
		}
		return tx;
	}

	private static CoordinateReferenceSystem createFromExtra(String auth, String code) {
		
		Proj4FileReader r = new Proj4FileReader();
		InputStream in = Proj.class.getResourceAsStream("other.extra");

		try {
			try {
				return csFactory.createFromParameters(auth+":"+code, r.readParameters(code, in));
			}
			finally {
				in.close();
			}
		}
		catch(IOException e) {
			Log.d(Proj.class.getSimpleName(),String.format("Failure creating crs %s:%s from extra", auth, code));
			return null;
		}
	}

}
