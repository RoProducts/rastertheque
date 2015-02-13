package de.rooehler.rastertheque.util;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.vividsolutions.jts.geom.Envelope;

import de.rooehler.rastertheque.core.Driver;
import de.rooehler.rastertheque.processing.Interpolation;
import de.rooehler.rastertheque.processing.RasterOp;
import de.rooehler.rastertheque.processing.RasterOps;

/**
 * The {@code Hints} class defines and manages collections of
 * keys and associated values which allow an application to provide input
 * into the choice of algorithms used by other classes which perform
 * rendering and image manipulation services.
 * 
 */

public class Hints implements Map<Object,Object>, Cloneable{

	/***KEYS IDs*****/

	private static final int INT_KEY_INTERPOLATION = 1001;
	
	private static final int INT_KEY_DRIVER = 1002;
	
	private static final int INT_KEY_COLORMAP = 1003;
	
	private static final int INT_KEY_AMPLITUDE_RESCALING = 1004;	

	

	
	/*****KEYS*****/

	public static final Key KEY_INTERPOLATION = new Hints.Key(INT_KEY_INTERPOLATION){
		
		@Override
		public boolean isCompatibleValue(Object val) {
			return val != null && val instanceof Interpolation;
		}
		
	};
	
	public static final Key KEY_DRIVER = new Hints.Key(INT_KEY_DRIVER){
		
		@Override
		public boolean isCompatibleValue(Object val) {
			return val instanceof Driver;
		}
	};
	
	public static final Key KEY_COLORMAP = new Hints.Key(INT_KEY_COLORMAP){
		
		@Override
		public boolean isCompatibleValue(Object val) {
			return val instanceof RasterOp && ((RasterOp) val).getOperationName().equals(RasterOps.COLORMAP);
		}
		
	};
	public static final Key KEY_AMPLITUDE_RESCALING = new Hints.Key(INT_KEY_AMPLITUDE_RESCALING){
		
		@Override
		public boolean isCompatibleValue(Object val) {
			return val instanceof RasterOp && ((RasterOp) val).getOperationName().equals(RasterOps.AMPLITUDE_RESCALING);
		}
		
	};

	

	HashMap hintmap = new HashMap(7);

	/**
	 * Constructs a new object with keys and values initialized
	 * from the specified Map object which may be null.
	 * @param init a map of key/value pairs to initialize the hints
	 *          or null if the object should be empty
	 */
	public Hints(Map<Key,?> init) {
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
	public Hints(Key key, Object value) {
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
		if (Hints.class.isInstance(m)) {
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
	 * {@link Hints} class to control various
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
