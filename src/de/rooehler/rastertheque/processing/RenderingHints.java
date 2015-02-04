/*
 * Copyright (c) 1998, 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
package de.rooehler.rastertheque.processing;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The {@code RenderingHints} class defines and manages collections of
 * keys and associated values which allow an application to provide input
 * into the choice of algorithms used by other classes which perform
 * rendering and image manipulation services.
 * 
 * This is an extract of the original RenderingHints class
 * 
 * java.awt.RenderingHints
 * 
 */

public class RenderingHints implements Map<Object,Object>, Cloneable{


	private static final int INT_KEY_INTERPOLATION = 5;

	private static final int VAL_INTERPOLATION_NEAREST_NEIGHBOR = 0;

	private static final int VAL_INTERPOLATION_BILINEAR = 1;

	private static final int VAL_INTERPOLATION_BICUBIC = 2;

	private static final int INT_KEY_SYMBOLIZATION = 8;
	
	private static final int VAL_COLORMAP = 12;
	
	private static final int VAL_AMPLITUDE_RESCALING = 13;
	
	
	/**
	 * Interpolation hint key.
	 * The {@code INTERPOLATION} hint controls how image pixels are
	 * filtered or resampled during an image rendering operation.
	 * <p>
	 * Implicitly images are defined to provide color samples at
	 * integer coordinate locations.
	 * When images are rendered upright with no scaling onto a
	 * destination, the choice of which image pixels map to which
	 * device pixels is obvious and the samples at the integer
	 * coordinate locations in the image are transfered to the
	 * pixels at the corresponding integer locations on the device
	 * pixel grid one for one.
	 * When images are rendered in a scaled, rotated, or otherwise
	 * transformed coordinate system, then the mapping of device
	 * pixel coordinates back to the image can raise the question
	 * of what color sample to use for the continuous coordinates
	 * that lie between the integer locations of the provided image
	 * samples.
	 * Interpolation algorithms define functions which provide a
	 * color sample for any continuous coordinate in an image based
	 * on the color samples at the surrounding integer coordinates.
	 * <p>
	 * The allowable values for this hint are
	 * <ul>
	 * <li>{@link #VALUE_INTERPOLATION_NEAREST_NEIGHBOR}
	 * <li>{@link #VALUE_INTERPOLATION_BILINEAR}
	 * <li>{@link #VALUE_INTERPOLATION_BICUBIC}
	 * </ul>
	 */
	public static final Key KEY_INTERPOLATION = new RenderingHints.Key(INT_KEY_INTERPOLATION){
		
		@Override
		public boolean isCompatibleValue(Object val) {
			return val.equals(VALUE_INTERPOLATION_BICUBIC) ||
					val.equals(VALUE_INTERPOLATION_BILINEAR) ||
					val.equals(VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		}
		
	};
	
	public static final Key KEY_SYMBOLIZATION = new RenderingHints.Key(INT_KEY_SYMBOLIZATION){

		@Override
		public boolean isCompatibleValue(Object val) {
			 return val.equals(VALUE_AMPLITUDE_RESCALING) ||
					val.equals(VALUE_COLORMAP);
		}
		
	};


	/**
	 * Interpolation hint value -- the color sample of the nearest
	 * neighboring integer coordinate sample in the image is used.
	 * Conceptually the image is viewed as a grid of unit-sized
	 * square regions of color centered around the center of each
	 * image pixel.
	 * <p>
	 * As the image is scaled up, it will look correspondingly blocky.
	 * As the image is scaled down, the colors for source pixels will
	 * be either used unmodified, or skipped entirely in the output
	 * representation.
	 *
	 */
	public static final Object VALUE_INTERPOLATION_NEAREST_NEIGHBOR =
			VAL_INTERPOLATION_NEAREST_NEIGHBOR;

	/**
	 * Interpolation hint value -- the color samples of the 4 nearest
	 * neighboring integer coordinate samples in the image are
	 * interpolated linearly to produce a color sample.
	 * Conceptually the image is viewed as a set of infinitely small
	 * point color samples which have value only at the centers of
	 * integer coordinate pixels and the space between those pixel
	 * centers is filled with linear ramps of colors that connect
	 * adjacent discrete samples in a straight line.
	 * <p>
	 * As the image is scaled up, there are no blocky edges between
	 * the colors in the image as there are with
	 * {@link #VALUE_INTERPOLATION_NEAREST_NEIGHBOR NEAREST_NEIGHBOR},
	 * but the blending may show some subtle discontinuities along the
	 * horizontal and vertical edges that line up with the samples
	 * caused by a sudden change in the slope of the interpolation
	 * from one side of a sample to the other.
	 * As the image is scaled down, more image pixels have their
	 * color samples represented in the resulting output since each
	 * output pixel recieves color information from up to 4 image
	 * pixels.
	 *
	 */
	public static final Object VALUE_INTERPOLATION_BILINEAR =
			VAL_INTERPOLATION_BILINEAR;

	/**
	 * Interpolation hint value -- the color samples of 9 nearby
	 * integer coordinate samples in the image are interpolated using
	 * a cubic function in both {@code X} and {@code Y} to produce
	 * a color sample.
	 * Conceptually the view of the image is very similar to the view
	 * used in the {@link #VALUE_INTERPOLATION_BILINEAR BILINEAR}
	 * algorithm except that the ramps of colors that connect between
	 * the samples are curved and have better continuity of slope
	 * as they cross over between sample boundaries.
	 * <p>
	 * As the image is scaled up, there are no blocky edges and the
	 * interpolation should appear smoother and with better depictions
	 * of any edges in the original image than with {@code BILINEAR}.
	 * As the image is scaled down, even more of the original color
	 * samples from the original image will have their color information
	 * carried through and represented.
	 *
	 */
	public static final Object VALUE_INTERPOLATION_BICUBIC =
			VAL_INTERPOLATION_BICUBIC;
	
	/**
	 * Symbolization hint value -- the raster data values are analyzed
	 * of their min / max value and amplitude rescaling is applied
	 * i.e. minimum value will get black pixels, maximum values white pixels
	 * values inbetween are linearly interpolated
	 *
	 */
	public static final Object VALUE_AMPLITUDE_RESCALING =	VAL_AMPLITUDE_RESCALING;
	
	/**
	 * Symbolization hint value -- the raster data is symbolized
	 * using a provided colormap and according to the properties
	 * set within in
	 *
	 */
	public static final Object VALUE_COLORMAP =	VAL_COLORMAP;

	HashMap hintmap = new HashMap(7);

	/**
	 * Constructs a new object with keys and values initialized
	 * from the specified Map object which may be null.
	 * @param init a map of key/value pairs to initialize the hints
	 *          or null if the object should be empty
	 */
	public RenderingHints(Map<Key,?> init) {
		if (init != null) {
			hintmap.putAll(init);
		}
	}

	/**
	 * Constructs a new object with the specified key/value pair.
	 * @param key the key of the particular hint property
	 * @param value the value of the hint property specified with
	 * <code>key</code>
	 */
	public RenderingHints(Key key, Object value) {
		hintmap.put(key, value);
	}


	/**
	 * Returns <code>true</code> if this <code>RenderingHints</code>
	 *  contains a mapping for the specified key.
	 *
	 * @param key key whose presence in this
	 * <code>RenderingHints</code> is to be tested.
	 * @return <code>true</code> if this <code>RenderingHints</code>
	 *          contains a mapping for the specified key.
	 * @exception <code>ClassCastException</code> if the key can not
	 *            be cast to <code>RenderingHints.Key</code>
	 */
	public boolean containsKey(Object key) {
		return hintmap.containsKey((Key) key);
	}

	/**
	 * Returns true if this RenderingHints maps one or more keys to the
	 * specified value.
	 * More formally, returns <code>true</code> if and only
	 * if this <code>RenderingHints</code>
	 * contains at least one mapping to a value <code>v</code> such that
	 * <pre>
	 * (value==null ? v==null : value.equals(v))
	 * </pre>.
	 * This operation will probably require time linear in the
	 * <code>RenderingHints</code> size for most implementations
	 * of <code>RenderingHints</code>.
	 *
	 * @param value value whose presence in this
	 *          <code>RenderingHints</code> is to be tested.
	 * @return <code>true</code> if this <code>RenderingHints</code>
	 *           maps one or more keys to the specified value.
	 */
	public boolean containsValue(Object value) {
		return hintmap.containsValue(value);
	}

	/**
	 * Returns a <code>Set</code> view of the mappings contained
	 * in this <code>RenderingHints</code>.  Each element in the
	 * returned <code>Set</code> is a <code>Map.Entry</code>.
	 * The <code>Set</code> is backed by the <code>RenderingHints</code>,
	 * so changes to the <code>RenderingHints</code> are reflected
	 * in the <code>Set</code>, and vice-versa.  If the
	 * <code>RenderingHints</code> is modified while
	 * while an iteration over the <code>Set</code> is in progress,
	 * the results of the iteration are undefined.
	 * <p>
	 * The entrySet returned from a <code>RenderingHints</code> object
	 * is not modifiable.
	 *
	 * @return a <code>Set</code> view of the mappings contained in
	 * this <code>RenderingHints</code>.
	 */
	public Set<Map.Entry<Object,Object>> entrySet() {
		return Collections.unmodifiableMap(hintmap).entrySet();
	}


	/**
	 * Returns <code>true</code> if this
	 * <code>RenderingHints</code> contains no key-value mappings.
	 *
	 * @return <code>true</code> if this
	 * <code>RenderingHints</code> contains no key-value mappings.
	 */
	public boolean isEmpty() {
		return hintmap.isEmpty();
	}

	/**
	 * Returns a <code>Set</code> view of the Keys contained in this
	 * <code>RenderingHints</code>.  The Set is backed by the
	 * <code>RenderingHints</code>, so changes to the
	 * <code>RenderingHints</code> are reflected in the <code>Set</code>,
	 * and vice-versa.  If the <code>RenderingHints</code> is modified
	 * while an iteration over the <code>Set</code> is in progress,
	 * the results of the iteration are undefined.  The <code>Set</code>
	 * supports element removal, which removes the corresponding
	 * mapping from the <code>RenderingHints</code>, via the
	 * <code>Iterator.remove</code>, <code>Set.remove</code>,
	 * <code>removeAll</code> <code>retainAll</code>, and
	 * <code>clear</code> operations.  It does not support
	 * the <code>add</code> or <code>addAll</code> operations.
	 *
	 * @return a <code>Set</code> view of the keys contained
	 * in this <code>RenderingHints</code>.
	 */
	public Set<Object> keySet() {
		return hintmap.keySet();
	}

	/**
	 * Returns a <code>Collection</code> view of the values
	 * contained in this <code>RenderinHints</code>.
	 * The <code>Collection</code> is backed by the
	 * <code>RenderingHints</code>, so changes to
	 * the <code>RenderingHints</code> are reflected in
	 * the <code>Collection</code>, and vice-versa.
	 * If the <code>RenderingHints</code> is modified while
	 * an iteration over the <code>Collection</code> is
	 * in progress, the results of the iteration are undefined.
	 * The <code>Collection</code> supports element removal,
	 * which removes the corresponding mapping from the
	 * <code>RenderingHints</code>, via the
	 * <code>Iterator.remove</code>,
	 * <code>Collection.remove</code>, <code>removeAll</code>,
	 * <code>retainAll</code> and <code>clear</code> operations.
	 * It does not support the <code>add</code> or
	 * <code>addAll</code> operations.
	 *
	 * @return a <code>Collection</code> view of the values
	 *          contained in this <code>RenderingHints</code>.
	 */
	public Collection<Object> values() {
		return hintmap.values();
	}

	/**
	 * Returns the value to which the specified key is mapped.
	 * @param   key   a rendering hint key
	 * @return  the value to which the key is mapped in this object or
	 *          <code>null</code> if the key is not mapped to any value in
	 *          this object.
	 * @exception <code>ClassCastException</code> if the key can not
	 *            be cast to <code>RenderingHints.Key</code>
	 * @see     #put(Object, Object)
	 */
	public Object get(Object key) {
		return hintmap.get((Key) key);
	}

	/**
	 * Maps the specified <code>key</code> to the specified
	 * <code>value</code> in this <code>RenderingHints</code> object.
	 * Neither the key nor the value can be <code>null</code>.
	 * The value can be retrieved by calling the <code>get</code> method
	 * with a key that is equal to the original key.
	 * @param      key     the rendering hint key.
	 * @param      value   the rendering hint value.
	 * @return     the previous value of the specified key in this object
	 *             or <code>null</code> if it did not have one.
	 * @exception <code>NullPointerException</code> if the key is
	 *            <code>null</code>.
	 * @exception <code>ClassCastException</code> if the key can not
	 *            be cast to <code>RenderingHints.Key</code>
	 * @exception <code>IllegalArgumentException</code> if the
	 *            {@link Key#isCompatibleValue(java.lang.Object)
	 *                   Key.isCompatibleValue()}
	 *            method of the specified key returns false for the
	 *            specified value
	 * @see     #get(Object)
	 */
	public Object put(Object key, Object value) {
		if (!((Key) key).isCompatibleValue(value)) {
			throw new IllegalArgumentException(value+
					" incompatible with "+
					key);
		}
		return hintmap.put((Key) key, value);
	}

	/**
	 * Copies all of the mappings from the specified <code>Map</code>
	 * to this <code>RenderingHints</code>.  These mappings replace
	 * any mappings that this <code>RenderingHints</code> had for any
	 * of the keys currently in the specified <code>Map</code>.
	 * @param m the specified <code>Map</code>
	 * @exception <code>ClassCastException</code> class of a key or value
	 *          in the specified <code>Map</code> prevents it from being
	 *          stored in this <code>RenderingHints</code>.
	 * @exception <code>IllegalArgumentException</code> some aspect
	 *          of a key or value in the specified <code>Map</code>
	 *           prevents it from being stored in
	 *            this <code>RenderingHints</code>.
	 */
	public void putAll(Map<?,?> m) {
		// ## javac bug?
		//if (m instanceof RenderingHints) {
		if (RenderingHints.class.isInstance(m)) {
			//hintmap.putAll(((RenderingHints) m).hintmap);
			for (Map.Entry<?,?> entry : m.entrySet())
				hintmap.put(entry.getKey(), entry.getValue());
		} else {
			// Funnel each key/value pair through our protected put method
			for (Map.Entry<?,?> entry : m.entrySet())
				put(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Clears this <code>RenderingHints</code> object of all key/value
	 * pairs.
	 */
	public void clear() {
		hintmap.clear();
	}

	/**
	 * Removes the key and its corresponding value from this
	 * <code>RenderingHints</code> object. This method does nothing if the
	 * key is not in this <code>RenderingHints</code> object.
	 * @param   key   the rendering hints key that needs to be removed
	 * @exception <code>ClassCastException</code> if the key can not
	 *            be cast to <code>RenderingHints.Key</code>
	 * @return  the value to which the key had previously been mapped in this
	 *          <code>RenderingHints</code> object, or <code>null</code>
	 *          if the key did not have a mapping.
	 */
	public Object remove(Object key) {
		return hintmap.remove((Key) key);
	}

	/**
	 * Returns the number of key-value mappings in this
	 * <code>RenderingHints</code>.
	 *
	 * @return the number of key-value mappings in this
	 * <code>RenderingHints</code>.
	 */
	public int size() {
		return hintmap.size();
	}


	/**
	 * Defines the base type of all keys used along with the
	 * {@link RenderingHints} class to control various
	 * algorithm choices in the rendering and imaging pipelines.
	 * Instances of this class are immutable and unique which
	 * means that tests for matches can be made using the
	 * {@code ==} operator instead of the more expensive
	 * {@code equals()} method.
	 */
	public abstract static class Key {
		private static HashMap identitymap = new HashMap(17);

		private String getIdentity() {
			// Note that the identity string is dependent on 3 variables:
			//     - the name of the subclass of Key
			//     - the identityHashCode of the subclass of Key
			//     - the integer key of the Key
			// It is theoretically possible for 2 distinct keys to collide
			// along all 3 of those attributes in the context of multiple
			// class loaders, but that occurence will be extremely rare and
			// we account for that possibility below in the recordIdentity
			// method by slightly relaxing our uniqueness guarantees if we
			// end up in that situation.
			return getClass().getName()+"@"+
			Integer.toHexString(System.identityHashCode(getClass()))+":"+
			Integer.toHexString(privatekey);
		}

		private synchronized static void recordIdentity(Key k) {
			Object identity = k.getIdentity();
			Object otherref = identitymap.get(identity);
			if (otherref != null) {
				Key otherkey = (Key) ((WeakReference) otherref).get();
				if (otherkey != null && otherkey.getClass() == k.getClass()) {
					throw new IllegalArgumentException(identity+
							" already registered");
				}
				// Note that this system can fail in a mostly harmless
				// way.  If we end up generating the same identity
				// String for 2 different classes (a very rare case)
				// then we correctly avoid throwing the exception above,
				// but we are about to drop through to a statement that
				// will replace the entry for the old Key subclass with
				// an entry for the new Key subclass.  At that time the
				// old subclass will be vulnerable to someone generating
				// a duplicate Key instance for it.  We could bail out
				// of the method here and let the old identity keep its
				// record in the map, but we are more likely to see a
				// duplicate key go by for the new class than the old
				// one since the new one is probably still in the
				// initialization stage.  In either case, the probability
				// of loading 2 classes in the same VM with the same name
				// and identityHashCode should be nearly impossible.
			}
			// Note: Use a weak reference to avoid holding on to extra
			// objects and classes after they should be unloaded.
			identitymap.put(identity, new WeakReference(k));
		}

		private int privatekey;

		/**
		 * Construct a key using the indicated private key.  Each
		 * subclass of Key maintains its own unique domain of integer
		 * keys.  No two objects with the same integer key and of the
		 * same specific subclass can be constructed.  An exception
		 * will be thrown if an attempt is made to construct another
		 * object of a given class with the same integer key as a
		 * pre-existing instance of that subclass of Key.
		 * @param privatekey the specified key
		 */
		protected Key(int privatekey) {
			this.privatekey = privatekey;
			recordIdentity(this);
		}

		/**
		 * Returns true if the specified object is a valid value
		 * for this Key.
		 * @param val the <code>Object</code> to test for validity
		 * @return <code>true</code> if <code>val</code> is valid;
		 *         <code>false</code> otherwise.
		 */
		public abstract boolean isCompatibleValue(Object val);

		/**
		 * Returns the private integer key that the subclass
		 * instantiated this Key with.
		 * @return the private integer key that the subclass
		 * instantiated this Key with.
		 */
		protected final int intKey() {
			return privatekey;
		}

		/**
		 * The hash code for all Key objects will be the same as the
		 * system identity code of the object as defined by the
		 * System.identityHashCode() method.
		 */
		public final int hashCode() {
			return super.hashCode();
		}

		/**
		 * The equals method for all Key objects will return the same
		 * result as the equality operator '=='.
		 */
		public final boolean equals(Object o) {
			return this == o;
		}
	}

}
