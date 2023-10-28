package com.github.telos_matter.memeCached;

import com.github.telos_matter.memeCached.core.Value;
import com.github.telos_matter.memeCached.util.Numbers;

import java.util.*;


/**
 * A {@link HashMap} that stores non-null values for a set
 * amount of time, before completely forgetting about them
 * afterwards. When their time comes
 * it just deletes their mapping as if it never existed before..
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
     * Stored in seconds
     */
    private long defaultLifeSpan;
    private boolean synchronous;
    private final Map<K, Value<V>> cache;

    /**
     * <p>{@link #synchronous} set to <code>true</code>
     * <p>{@link #defaultLifeSpan} set to {@link #_15_MINUTES}
     */
    public MemeCached () {
        this(true, _15_MINUTES);
    }

    /**
     * @param synchronous if the methods should be synchronized or not
     * @param defaultLifeSpan to give to cached value
     * @throws IllegalArgumentException if <code>defaultLifeSpan</code> is negative
     */
    public MemeCached (boolean synchronous, long defaultLifeSpan) {
        this.defaultLifeSpan = Numbers.requireNonNegative(defaultLifeSpan);
        this.synchronous = synchronous;
        this.cache = new HashMap<>();
    }

    /**
     * @return the value if it exists, and it's alive,
     * otherwise <code>null</code>
     */
    private Value<V> getValue (K key) {
        Value<V> value = cache.get(key);
        if (value == null) {
            return null;
        } else if (!value.isAlive()) {
            cache.remove(key);
            return null;
        } else {
            return value;
        }
    }

    /**
     * Rechecks all the values if any of them is dead in
     * order to remove them
     * @return number of alive values afterward
     */
    private int recheckAll () {
        int count = 0;
        Iterator<K> it = cache.keySet().iterator();
        while (it.hasNext()) {
            Value<V> value = cache.get(it.next());
            assert value != null: "Cannot be null!";
            if (value.isAlive()) {
                count++;
            } else {
                it.remove();
            }
        }
        return count;
    }

    /**
     * @return number of the mappings
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
     * @see #size()
     */
    private int size0() {
        return recheckAll();
    }

    /**
     * @return if the map is empty
     */
    public boolean isEmpty () {
        return size() == 0;
    }

    /**
     * Caches with the defaultLifeSpan
     * @see #cache(K, V, long)
     * @throws NullPointerException if <code>value</code> is <code>null</code>
     */
    public boolean put (K key, V value) {
        return cache(key, value, defaultLifeSpan);
    }

    /**
     * @param lifeSpan in seconds
     * @return <code>true</code> if the <code>key</code>
     * is new, <code>false</code> if it has been overwritten
     * @throws NullPointerException if <code>value</code> is <code>null</code>
     * @throws IllegalArgumentException if <code>lifeSpan</code> is negative
     */
    public boolean cache (K key, V value, long lifeSpan) {
        if (synchronous) {
            synchronized (this) {
                return cache0(key, value, lifeSpan);
            }
        } else {
            return cache0(key, value, lifeSpan);
        }
    }
    /**
     * @see #cache(K, V, long)
     */
    private boolean cache0 (K key, V value, long lifeSpan) {
        Value <V> exists = getValue(key);
        cache.put(key, new Value<>(value, lifeSpan));
        return exists == null;
    }

    /**
     * @return the value stored for that <code>key</code>
     * or <code>null</code> if it doesn't exist
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
     * @see #get(K)
     */
    private V get0 (K key) {
        Value<V> value = getValue(key);
        if (value == null) {
            return null;
        } else {
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
     * @see #keySet()
     */
    private Set<K> keySet0() {
        recheckAll();
        return cache.keySet();
    }

    /**
     * @return the values
     */
    public Collection<V> values() {
        return keySet()
                .stream()
                .map(key -> cache.get(key).get())
                .toList();
    }

    /**
     * @param duration in seconds
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
     * @see #extend(K, long)
     */
    public boolean extend0 (K key, long duration) {
        Value<V> value = getValue(key);
        if (value == null) {
            return false;
        } else {
            return value.extend(duration);
        }
    }

    /**
     * @return whether the value is still alive or not
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
     * @see #isAlive(K)
     */
    private boolean isAlive0 (K key) {
        return getValue(key) != null;
    }

    /**
     * @return the value held by the <code>key</code>
     * before forgetting about it, or <code>null</code>
     * if it doesn't exist
     */
    public V forget (K key) {
        if (synchronous) {
            synchronized (this) {
                return forget0(key);
            }
        } else {
            return forget0(key);
        }
    }
    /**
     * @see #forget(K)
     */
    private V forget0 (K key) {
        Value<V> value = getValue(key);
        if (value == null) {
            return null;
        } else {
            cache.remove(key);
            return value.kill();
        }
    }

    /**
     * @return <code>true</code> if the key exists
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
     * @see #update(K, V)
     */
    private boolean update0 (K key, V newValue) {
        Value<V> value = getValue(key);
        if (value == null) {
            return false;
        } else {
            return value.update(newValue);
        }
    }

    /**
     * @return how long is the value still going to be remembered for
     * in seconds
     * or <code>-1</code> if it doesn't exist
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
     * @see #stillAliveFor(K)
     */
    private long stillAliveFor0 (K key) {
        Value<V> value = getValue(key);
        if (value == null) {
            return -1;
        } else {
            return value.stillAliveFor();
        }
    }

    /**
     * Forgets all the values
     * @return how many values it held
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
     * @see #forgetAll()
     */
    private int forgetAll0 () {
        int count = 0;
        Iterator<K> it = cache.keySet().iterator();
        while (it.hasNext()) {
            Value<V> value = cache.get(it.next());
            assert value != null: "Cannot be null!";
            if (value.isAlive()) {
                value.kill();
                count++;
            }
            it.remove();
        }
        return count;
    }

    /**
     * @return if it contains the <code>key</code>
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
     * @see #containsKey(K)
     */
    private boolean containsKey0(K key) {
        return getValue(key) != null;
    }

    public synchronized void setSynchronous (boolean synchronous) {
        this.synchronous = synchronous;
    }

    public synchronized boolean getSynchronous () {
        return this.synchronous;
    }

    /**
     * @throws IllegalArgumentException if <code>defaultLifeSpan</code> is negative
     */
    public synchronized void setDefaultLifeSpan (long defaultLifeSpan) { // Eh, they are synchronized just like that
        this.defaultLifeSpan = Numbers.requireNonNegative(defaultLifeSpan);
    }

    public synchronized long getDefaultLifeSpan () {
        return this.defaultLifeSpan;
    }

}
