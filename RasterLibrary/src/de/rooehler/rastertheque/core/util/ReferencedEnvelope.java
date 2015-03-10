package de.rooehler.rastertheque.core.util;

import org.osgeo.proj4j.CoordinateReferenceSystem;
import org.osgeo.proj4j.CoordinateTransform;
import org.osgeo.proj4j.CoordinateTransformFactory;
import org.osgeo.proj4j.ProjCoordinate;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.util.AffineTransformation;
import com.vividsolutions.jts.geom.util.GeometryEditor.CoordinateOperation;

import de.rooehler.rastertheque.proj.Proj;
/**
 * ReferencedEnvelope wraps an envelope and a crs
 * 
 * it provides the possibility to transform an envelope
 * to a target crs applying an expansion of the envelope
 * 
 * @author Robert Oehler
 *
 */
public class ReferencedEnvelope {
	
	Envelope envelope;
	
	CoordinateReferenceSystem crs;
	
	public ReferencedEnvelope(final Envelope pEnv, final CoordinateReferenceSystem pCRS){
		
		this.envelope = pEnv;
		
		this.crs = pCRS;
	}
	
	public ReferencedEnvelope(final double x1, final double x2, final double y1, final double y2,
	        final CoordinateReferenceSystem crs){
		
		this(new Envelope(x1, x2, y1, y2),crs);
		
	}
	 /**
     * Returns the number of dimensions.
     */
    public int getDimension() {
        return 2;
    }
	
    /**
     * Transforms the referenced envelope to the specified coordinate reference system
     * using the specified amount of points.
     * <p>
     * This method can handle the case where the envelope contains the North or South pole,
     * or when it cross the +180° longitude.
     *
     * @param targetCRS The target coordinate reference system.
     * @param lenient   {@code true} if datum shift should be applied even if there is
     *                  insuffisient information. Otherwise (if {@code false}), an
     *                  exception is thrown in such case.
     * @param numPointsForTransformation The number of points to use for sampling the envelope.
     * @return The transformed envelope.
     * @throws FactoryException if the math transform can't be determined.
     * @throws TransformException if at least one coordinate can't be transformed.
     *
     * @see CRS#transform(CoordinateOperation, org.opengis.geometry.Envelope)
     *
     * @since 2.3
     */
    public ReferencedEnvelope transform(final CoordinateReferenceSystem targetCRS, final int numPointsForTransformation){
        if( this.crs == null ){

             // really this is a the code that created this ReferencedEnvelope
             throw new NullPointerException("Unable to transform referenced envelope, crs has not yet been provided."); 
        }
        /*
         * Gets a first estimation using an algorithm capable to take singularity in account
         * (North pole, South pole, 180° longitude). We will expand this initial box later.
         */
        CoordinateTransformFactory txFactory = new CoordinateTransformFactory();
        CoordinateTransform tx = txFactory.createTransform(crs, targetCRS);
        Envelope transformed = Proj.reproject(envelope, crs, targetCRS);
        
        /*
         * Now expands the box using the usual utility methods.
         */
        //JTS.transform(this, target, transform, numPointsForTransformation);
        // -->
        Envelope expanded = transform(this.envelope,transformed, tx, numPointsForTransformation);
        
        
        return new ReferencedEnvelope(expanded, targetCRS);
    }

    /**
     * transforms the @param sourceEnvelope according to transformation @param tx
     * the transformation is applied to @param targetEnvelope
     * which is returned
     * 
     * the envelope is expanded by @param nPoints 
     * 
     * @param sourceEnvelope
     * @param targetEnvelope
     * @param tx
     * @param npoints
     * @return the transformed envelope
     */
    public Envelope transform(final Envelope sourceEnvelope, Envelope targetEnvelope,
            final CoordinateTransform tx, int npoints)  {
    	
        final double[] coordinates = new double[(4 * npoints) * 2];
        final double xmin = sourceEnvelope.getMinX();
        final double xmax = sourceEnvelope.getMaxX();
        final double ymin = sourceEnvelope.getMinY();
        final double ymax = sourceEnvelope.getMaxY();
        final double scaleX = (xmax - xmin) / npoints;
        final double scaleY = (ymax - ymin) / npoints;

        int offset = 0;

        for (int t = 0; t < npoints; t++) {
            final double dx = scaleX * t;
            final double dy = scaleY * t;
            coordinates[offset++] = xmin; // Left side, increasing toward top.
            coordinates[offset++] = ymin + dy;
            coordinates[offset++] = xmin + dx; // Top side, increasing toward right.
            coordinates[offset++] = ymax;
            coordinates[offset++] = xmax; // Right side, increasing toward bottom.
            coordinates[offset++] = ymax - dy;
            coordinates[offset++] = xmax - dx; // Bottom side, increasing toward left.
            coordinates[offset++] = ymin;
        }

        xform(tx, coordinates, coordinates);

        // Now find the min/max of the result
        if (targetEnvelope == null) {
            targetEnvelope = new Envelope();
        }

        for (int t = 0; t < offset;) {
            targetEnvelope.expandToInclude(coordinates[t++], coordinates[t++]);
        }

        return targetEnvelope;
    	
    }
    
    /**
     * Like a transform but eXtreme!
     * 
     * Transforms an array of coordinates using the provided math transform. Each coordinate is
     * transformed separately. In case of a transform exception then the new value of the coordinate
     * is the last coordinate correctly transformed.
     * 
     * @param transform
     *            The math transform to apply.
     * @param src
     *            The source coordinates.
     * @param dest
     *            The destination array for transformed coordinates.
     * @throws TransformException
     *             if this method failed to transform any of the points.
     */
    public void xform(final CoordinateTransform tx, final double[] src, final double[] dest) {

        final int sourceDim = 2;
        final int targetDim = 2;

        boolean startPointTransformed = false;
        ProjCoordinate c = null;

        for (int i = 0; i < src.length; i += sourceDim) {

        	double x = src[i];
        	double y = src[i + 1];

        	c = new ProjCoordinate(x, y);
        	tx.transform(c, c);

        	dest[i] = c.x;
        	dest[i + 1] = c.y;

        	if (!startPointTransformed) {
        		startPointTransformed = true;

        		for (int j = 0; j < i; j++) {
        			System.arraycopy(dest, j, dest, i, targetDim);
        		}
        	}

        }
    }
    
    public Envelope getEnvelope() {
		return envelope;
	}
    public CoordinateReferenceSystem getCrs() {
		return crs;
	}
}
