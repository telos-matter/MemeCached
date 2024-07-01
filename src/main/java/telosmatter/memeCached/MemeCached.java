package telosmatter.memeCached;

import telosmatter.memeCached.core.Callback;
import telosmatter.memeCached.core.Value;
import telosmatter.memeCached.util.Numbers;

import java.util.*;

/**
 * A map that uses an underlying {@link HashMap} to stores
 * non-<code>null</code> values for a set
 * amount of time, before completely forgetting about them
 * afterwards, with the possibility to define a {@link Callback}
 * that will get called when the value expires.
 * <br>
 * It's important to understand how the underlying mechanics
 * of this map work to use it effectively; when you {@link #put(Object, Object)}
 * or {@link #cache(Object, Object, long, Callback)} a value in the map
 * it will be stored alongside how long it should be remembered for. No timer
 * or anything similar is set to go off when that time is up, instead only when you query
 * the map for that value (or check the map for its {@link #size()} for example),
 * does it check if it still should be alive or not, if not
 * it will remove it from the underlying HashMap and call the callback if it exists
 * and return <code>null</code> to indicate that there is no value for the
 * queried key.
 * <br>
 * And so the actual contract or guarantee is that
 * if you query the map after the specified amount of time has elapsed
 * the value will not be there. And not that it will be removed
 * and that the callback (if one exists) will be called once the time is up.
 * <br><br>
 * Although this is supposed to be a map, it does not
 * implement the {@link Map} interface yet. May do so
 * in the future. Or feel free to do so and
 * contribute to the
 * <a href="https://github.com/telos-matter/MemeCached">GitHub repo.</a>
 * @see Callback
 */
public class MemeCached <K, V> {


    /**
     * 1 minute in seconds
     */
    public static final long _1_MINUTE = 60;
    /**
     * 5 minutes in seconds
     */
    public static final long _5_MINUTES = _1_MINUTE * 5;
    /**
     * 10 minutes in seconds
     */
    public static final long _10_MINUTES = _5_MINUTES * 2;
    /**
     * 15 minutes in seconds
     */
    public static final long _15_MINUTES = _5_MINUTES * 3;
    /**
     * 30 minutes in seconds
     */
    public static final long _30_MINUTES = _15_MINUTES * 2;
    /**
     * 1 hour in seconds
     */
    public static final long _1_HOUR = _30_MINUTES * 2;
    /**
     * 1 day in seconds
     */
    public static final long _1_DAY = _1_HOUR * 24;
    /**
     * 1 week in seconds
     */
    public static final long _1_WEEK = _1_DAY * 7;

    /**
     * The default lifespan to give new values.
     * Stored in seconds.
     */
    private long defaultLifeSpan;
    /**
     * The default callback to give new values.
     */
    private Callback<V> defaultCallback;
    /**
     * Is access to this map and should the actions
     * preformed on it be synchronous?
     * <hr>
     * The way synchronisation is implemented
     * is that any method that can be called
     * by the user first checks if this flag
     * is set, if so it locks on <code>this</code>
     * and calls
     * the actual method that does
     * the job. Otherwise, it simply calls it.
     * <br>
     * And so most methods that can be called by
     * the user have two versions, the first one which
     * is the one exposed to the user checks
     * and performs the synchronous-ness, and
     * the second one which is hidden actually
     * implements the method.
     */
    private final boolean synchronous;
    /**
     * The actual underlying map
     */
    private final Map<K, Value<V>> cache;

    /**
     * Creates an instance with this configuration:
     * <ul>
     *     <li>{@link #defaultLifeSpan} set to {@link #_15_MINUTES}</li>
     *     <li>{@link #defaultCallback} set to <code>null</code></li>
     *     <li>{@link #synchronous} set to <code>true</code></li>
     * </ul>
     * @see #MemeCached(long, Callback, boolean)
     */
    public MemeCached () {
        this(_15_MINUTES, null, true);
    }

    /**
     * Creates an instance with this configuration:
     * <ul>
     *     <li>{@link #defaultLifeSpan} set to {@link #_15_MINUTES}</li>
     *     <li>{@link #defaultCallback} set to <code>null</code></li>
     *     <li>{@link #synchronous} set to the given value</li>
     * </ul>
     * @see #MemeCached(long, Callback, boolean)
     */
    public MemeCached (boolean synchronous) {
        this(_15_MINUTES, null, synchronous);
    }

    /**
     * Creates an instance with the given configuration
     * @param defaultLifeSpan to give to cached values. In seconds
     * @param defaultCallback to set to cached values
     * @param synchronous if actions on this map should be preformed synchronously
     * @throws IllegalArgumentException if <code>defaultLifeSpan</code> is negative
     */
    public MemeCached (long defaultLifeSpan, Callback <V> defaultCallback, boolean synchronous) {
        this.defaultLifeSpan = Numbers.requireNonNegative(defaultLifeSpan);
        this.defaultCallback = defaultCallback;
        this.synchronous = synchronous;
        this.cache = new HashMap<>();
    }

    /**
     * Used internally to terminate a value.
     * Calls the callback of the value
     * if it has one before it does so.
     * Does <b>not</b> remove it from the
     * {@link #cache}.
     */
    private void terminateValue (Value <V> value) {
        // Check if the callback exists
        Callback<V> callback = value.getCallback();
        if (callback != null) {
            // If so call it
            callback.callback(value.get(), this, value.age(), callback);
        }
        // And then terminate the value
        value.kys();
    }

    /**
     * Used internally to facilitate
     * the work of retrieving values.
     *
     * @return the value if it exists, and is alive,
     * otherwise <code>null</code>.
     */
    private Value<V> getValue (K key) {
        // Get the value
        Value<V> value = cache.get(key);
        // If it's null
        if (value == null) {
            // Then it doesn't exist, and so just return null
            return null;
        // Otherwise if it exists, check if it's expired
        } else if (value.expired()) {
            // If so:
            // - Remove it from the cache
            cache.remove(key);
            // - Terminate it
            terminateValue(value);
            // - And return null
            return null;
        // Otherwise
        } else {
            // Return the value
            return value;
        }
    }

    /**
     * Used internally to rechecks all the values
     * and see if any of them are expired in
     * order to remove them.
     *
     * @return number of alive values afterwards.
     */
    private int recheckAll () {
        // Initialize the count
        int count = 0;
        // Retrieve an iterator of the map
        Iterator<K> it = cache.keySet().iterator();
        // Iterate
        while (it.hasNext()) {
            // Get the value of the current key
            Value<V> value = cache.get(it.next());
            // Check if it still should be alive
            if (!value.expired()) {
                // If so, increment the count
                count++;
            } else {
                // Otherwise, if it should be removed
                // then do so
                it.remove();
                // And terminate it
                terminateValue(value);
            }
        }
        // At the end return the count
        return count;
    }

    /**
     * @return the number of mappings
     */
    public int size() {
        if (synchronous) {
            synchronized (this) {
                return size0();
            }
        } else {
            return size0();
        }
    }
    /**
     * Actual implementation of {@link #size()}
     */
    private int size0() {
        return recheckAll();
    }

    /**
     * @return whether the map is empty or not
     */
    public boolean isEmpty () {
        return size() == 0;
    }

    /**
     * Caches the given value with the default configuration
     * @see #cache(Object, Object, long, Callback)
     * @throws NullPointerException if <code>value</code> is <code>null</code>
     */
    public boolean put (K key, V value) {
        return cache(key, value, defaultLifeSpan, defaultCallback);
    }

    // TODO add another cache method with no call back
    // TODO add get or default

    /**
     * Cache the given value with the given options
     *
     * @param key for the value
     * @param value to cache
     * @param lifeSpan in seconds of how long should the value be remembered for
     * @param callback to call when the value dies. Can be <code>null</code>.
     * @return <code>true</code> if the <code>key</code>
     * is new, <code>false</code> if it has been overwritten
     * @throws NullPointerException if <code>value</code> is <code>null</code>
     * @throws IllegalArgumentException if <code>lifeSpan</code> is negative
     */
    public boolean cache (K key, V value, long lifeSpan, Callback <V> callback) {
        if (synchronous) {
            synchronized (this) {
                return cache0(key, value, lifeSpan, callback);
            }
        } else {
            return cache0(key, value, lifeSpan, callback);
        }
    }
    /**
     * Actual implementation of {@link #cache(Object, Object, long, Callback)}
     */
    private boolean cache0 (K key, V value, long lifeSpan, Callback <V> callback) {
        // Check arguments
        Objects.requireNonNull(value);
        Numbers.requireNonNegative(lifeSpan);
        // See if the value exists
        Value <V> exists = getValue(key);
        // Put the new value in
        cache.put(key, new Value<>(value, lifeSpan, callback));
        // Return whether it existed or not
        return exists == null;
    }

    /**
     * @return the value mapped with the given <code>key</code>
     * or <code>null</code> if it doesn't exist.
     */
    public V get (K key) {
        if (synchronous) {
            synchronized (this) {
                return get0(key);
            }
        } else {
            return get0(key);
        }
    }
    /**
     * The actual implementation of {@link #get(Object)}
     */
    private V get0 (K key) {
        // Get the value object
        Value<V> value = getValue(key);
        // If it's null
        if (value == null) {
            // Return null
            return null;
        } else {
            // Otherwise return the actual value
            return value.get();
        }
    }

    /**
     * @return the key set
     */
    public Set<K> keySet() {
        if (synchronous) {
            synchronized (this) {
                return keySet0();
            }
        } else {
            return keySet0();
        }
    }
    /**
     * The actual implementation of {@link #keySet()}
     */
    private Set<K> keySet0() {
        // Recheck all to update
        recheckAll();
        // Simply return the key set from the underlying map
        return cache.keySet();
    }

    /**
     * @return the values
     */
    public Collection<V> values() {
        if (synchronous) {
            synchronized (this) {
                return values0();
            }
        } else {
            return values0();
        }
    }
    /**
     * The actual implementation of {@link #values()}
     */
    private Collection<V> values0 () {
        // Recheck all to update
        recheckAll();
        // Get the actual values from the values
        return cache.values()
                .stream()
                .map(Value::get)
                .toList();
    }

    /**
     * Extend or shorten the lifespan of the value
     * mapped to the given <code>key</code>.
     * @param duration in seconds. <b>Can</b> be negative
     * @return <code>true</code> if value exists and
     * has been extended, otherwise <code>false</code>
     */
    public boolean extend (K key, long duration) {
        if (synchronous) {
            synchronized (this) {
                return extend0(key, duration);
            }
        } else {
            return extend0(key, duration);
        }
    }
    /**
     * Actual implementation of {@link #extend(Object, long)}
     */
    private boolean extend0 (K key, long duration) {
        // Get the value
        Value<V> value = getValue(key);
        // If it doesn't exist
        if (value == null) {
            // Return false
            return false;
        } else {
            // Otherwise extend it
            value.extend(duration);
            // And return true
            return true;
        }
    }

    /**
     * Update the life span of value mapped to
     * the given <code>key</code>, either by extending it or shorting it
     * so that the value will only be alive for how much ever
     * is specified in <code>lifeSpan</code>
     * @param lifeSpan the "new" lifespan in seconds
     * @return <code>true</code> if the exists
     * and its life span has been updated. Otherwise <code>false</code>
     * @throws IllegalArgumentException if <code>lifeSpan</code> is negative
     */
    public boolean makeRemainingLifeSpan (K key, long lifeSpan) {
        if (synchronous) {
            synchronized (this) {
                return makeRemainingLifeSpan0(key, lifeSpan);
            }
        } else {
            return makeRemainingLifeSpan0(key, lifeSpan);
        }
    }
    /**
     * The actual implementation of {@link #makeRemainingLifeSpan(Object, long)}
     */
    private boolean makeRemainingLifeSpan0 (K key, long lifeSpan) {
        // Get the value
        Value<V> value = getValue(key);
        // Check if it exists
        if (value == null) {
            // If not, return false
            return false;
        } else {
            // Otherwise update the lifespan
            value.makeRemainingLifeSpan(lifeSpan);
            // And return true
            return true;
        }
    }

    /**
     * Alias for {@link #isAlive(Object)}
     *
     * @return whether the mapping exists or not
     */
    public boolean exists (K key) {
        return isAlive(key);
    }

    /**
     * @return whether the mapping is still alive or not
     */
    public boolean isAlive (K key) {
        if (synchronous) {
            synchronized (this) {
                return isAlive0(key);
            }
        } else {
            return isAlive0(key);
        }
    }
    /**
     * Actual implementation of {@link #isAlive(Object)}
     */
    private boolean isAlive0 (K key) {
        return getValue(key) != null;
    }

    /**
     * Alias for {@link #forgor(Object)}
     * 
     * @return the value mapped to the given key
     */
    public V remove (K key) {
        return forgor(key);
    }

    /**
     * Forgors 💀 a mapping.
     *
     * @return the value mapped to the <code>key</code>
     * before forgorring about it, or <code>null</code>
     * if it doesn't exist.
     */
    public V forgor (K key) {
        if (synchronous) {
            synchronized (this) {
                return forgor0(key);
            }
        } else {
            return forgor0(key);
        }
    }
    /**
     * The actual implementation of {@link #forgor(Object)}
     */
    private V forgor0 (K key) {
        // Retrieve the value
        Value<V> value = getValue(key);
        // If it doesn't exist
        if (value == null) {
            // Return null
            return null;
        // Otherwise
        } else {
            // Remove it
            cache.remove(key);
            // Retrieve the actual value before terminating it
            V actualValue = value.get();
            // Terminate the value
            terminateValue(value);
            // Return the actual value
            return actualValue;
        }
    }

    /**
     * Updates a mapping
     * @return <code>true</code> if the value exists
     * and has been updated, <code>false</code> otherwise
     * @throws NullPointerException if <code>newValue</code> is <code>null</code>
     */
    public boolean update (K key, V newValue) {
        if (synchronous) {
            synchronized (this) {
                return update0(key, newValue);
            }
        } else {
            return update0(key, newValue);
        }
    }
    /**
     * The actual implementation of {@link #update(K, V)}
     */
    private boolean update0 (K key, V newValue) {
        // Check for null
        Objects.requireNonNull(newValue);
        // Get the value
        Value<V> value = getValue(key);
        // Check if it exists
        if (value == null) {
            // If not return false
            return false;
        } else {
            // Otherwise update
            value.update(newValue);
            // And return true
            return true;
        }
    }

    /**
     * @return how long is the value still going to be remembered for
     * in seconds, rounded down.
     * Or <code>-1</code> if it doesn't exist
     */
    public long stillAliveFor (K key) {
        if (synchronous) {
            synchronized (this) {
                return stillAliveFor0(key);
            }
        } else {
            return stillAliveFor0(key);
        }
    }
    /**
     * The actual implementation of {@link #stillAliveFor(Object)}
     * @see #stillAliveFor(K)
     */
    private long stillAliveFor0 (K key) {
        // Get the value
        Value<V> value = getValue(key);
        // Check if it exists
        if (value == null) {
            // If not, return -1
            return -1;
        } else {
            // Else return the value
            return value.stillAliveFor();
        }
    }

    /**
     * Clears the map. Just an alias for
     * {@link #forgetAll()}
     * @return how many mappings it had
     */
    public int clear () {
        return forgetAll();
    }

    /**
     * Forgets all the values.
     * @return how many mapping it held
     */
    public int forgetAll () {
        if (synchronous) {
            synchronized (this) {
                return forgetAll0();
            }
        } else {
            return forgetAll0();
        }
    }
    /**
     * The actual implementation of {@link #forgetAll()}
     */
    private int forgetAll0 () {
        // Initialize the count
        int count = 0;
        // Get an iterator
        Iterator<K> it = cache.keySet().iterator();
        // Iterate
        while (it.hasNext()) {
            // Get the value
            Value<V> value = cache.get(it.next());
            // Check if the value is still alive
            if (!value.expired()) {
                // If so increment the count
                count++;
            }
            // Then remove it from the cache
            it.remove();
            // And terminate it
            terminateValue(value);
        }
        // Return the count at the end
        return count;
    }

    /**
     * @return whether it contains the <code>key</code>
     */
    public boolean containsKey(K key) {
        if (synchronous) {
            synchronized (this) {
                return containsKey0(key);
            }
        } else {
            return containsKey0(key);
        }
    }
    /**
     * The actual implementation of {@link #containsKey(Object)}
     */
    private boolean containsKey0(K key) {
        // Simply check if the value exists
        return getValue(key) != null;
    }

    /**
     * @throws IllegalArgumentException if <code>defaultLifeSpan</code> is negative
     */
    public void setDefaultLifeSpan (long defaultLifeSpan) {
        if (synchronous) {
            synchronized (this) {
                setDefaultLifeSpan0(defaultLifeSpan);
            }
        } else {
            setDefaultLifeSpan0(defaultLifeSpan);
        }
    }
    /**
     * The actual implementation of {@link #setDefaultLifeSpan(long)}
     */
    private void setDefaultLifeSpan0(long defaultLifeSpan) {
        this.defaultLifeSpan = Numbers.requireNonNegative(defaultLifeSpan);
    }

    public long getDefaultLifeSpan() {
        if (synchronous) {
            synchronized (this) {
                return getDefaultLifeSpan0();
            }
        } else {
            return getDefaultLifeSpan0();
        }
    }

    /**
     * The actual implementation of {@link #getDefaultLifeSpan()}
     */
    private long getDefaultLifeSpan0() {
        return this.defaultLifeSpan;
    }

    public void setDefaultCallback (Callback <V> defaultCallback) {
        if (synchronous) {
            synchronized (this) {
                setDefaultCallback0(defaultCallback);
            }
        } else {
            setDefaultCallback0(defaultCallback);
        }
    }
    /**
     * The actual implementation of {@link #setDefaultCallback(Callback)} ()}
     */
    private void setDefaultCallback0(Callback <V> defaultCallback) {
        this.defaultCallback = defaultCallback;
    }

    public Callback <V> getDefaultCallback () {
        if (synchronous) {
            synchronized (this) {
                return getDefaultCallback0();
            }
        } else {
            return getDefaultCallback0();
        }
    }
    /**
     * The actual implementation of {@link #getDefaultCallback()} ()}
     */
    private Callback <V> getDefaultCallback0() {
        return this.defaultCallback;
    }

}
