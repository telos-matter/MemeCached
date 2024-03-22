package memeCached.core;

import memeCached.util.Numbers;

/**
 * Groups information about a value of type <code>V</code>, and
 * eases the work for {@link memeCached.MemeCached} a bit.
 * <br>
 * The end-user is never going to interact directly with this
 * class. It is managed by MemeCached.
 */
public class Value <V> {

    /**
     * The actual value. Can't be <code>null</code>
     */
    private V value;
    /**
     * How long should this value be left alive for?
     * Stored in nanoseconds.
     */
    private long lifeSpan;
    /**
     * The function to be called when this value dies.
     * Can be <code>null</code>.
     */
    private Callback <V> callback;
    /**
     * When was this value born?
     * Acquired from {@link System#nanoTime()}.
     */
    private final long birth;
    /**
     * Has this value been terminated by
     * the memeCached it belongs to?
     * <br>
     * Any call to a terminated value results
     * in an {@link AssertionError}
     */
    private boolean terminated;


    /**
     * Creates a value.
     *
     * @param value the value to manage. Can <b>not</b> be <code>null</code>.
     * @param lifeSpan in seconds
     * @param callback the callback for when this values dies. Can be <code>null</code>
     */
    public Value (V value, long lifeSpan, Callback <V> callback) {
        // Set the birth
        this.birth = System.nanoTime();
        // Make sure value is not null
        if (value == null) {
            throw new AssertionError("Value is null!");
        }
        this.value = value;
        // Make sure the lifeSpan is not negative
        if (lifeSpan < 0) {
            throw new AssertionError("LifeSpan is negative");
        }
        // Convert it to nanoseconds
        this.lifeSpan = lifeSpan * 1_000_000_000L;
        // Set the callback
        this.callback = callback;
        // Set terminated value
        this.terminated = false;
    }

    /**
     * Asserts that this value has not been terminated
     */
    private void assertNotTerminated () {
        if (this.terminated) {
            throw new AssertionError("Value already terminated!");
        }
    }

    /**
     * @return this value's current age rounded to the closest second
     */
    public long age () {
        assertNotTerminated();
        // Get the age in nanoseconds
        long age = System.nanoTime() - birth;
        // Return rounded version
        return Math.round(age / 1_000_000_000D);
    }

    /**
     * @return whether this value should be dead by now or not
     */
    public boolean expired () {
        assertNotTerminated();
        // Get the age
        long age = System.nanoTime() - birth;
        // Return whether it's greater than the lifeSpan
        return age > lifeSpan;
    }

    /**
     * @return the actual value
     */
    public V get() {
        assertNotTerminated();
        return value;
    }

    /**
     * Commit Seppuku.
     */
    public void kys () {
        value = null;
        callback = null;
        lifeSpan = -1;
        terminated = true;
    }

    /**
     * Updates the value
     */
    public void update (V newValue) {
        assertNotTerminated();
        if (newValue == null) {
            throw new AssertionError("New value is null!");
        }
        this.value = newValue;
    }

    /**
     * Extend or shorten the lifespan.
     * @param duration to change lifespan with in seconds. <b>Can</b> be negative.
     */
    public void extend (long duration) {
        assertNotTerminated();
        lifeSpan += duration * 1_000_000_000L;
    }

    /**
     * Update the lifespan by extending it or shorting it
     * so that this value is can only be alive for how much ever
     * is specified in <code>newLifeSpan</code>
     * @param newLifeSpan the "new" lifespan in seconds. Can <b>not</b> be negative
     */
    public void makeRemainingLifeSpan (long newLifeSpan) {
        assertNotTerminated();
        // Assert newLifeSpan is not negative
        if (newLifeSpan < 0) {
            throw new AssertionError("New lifespan is negative!");
        }
        // Convert to nanoseconds
        newLifeSpan = newLifeSpan * 1_000_000_000L;
        // Get the current age
        long age = System.nanoTime() - birth;
        // Set the new lifespan
        this.lifeSpan = age + newLifeSpan;
    }

    /**
     * @return how long can this value still be alive for, in seconds, rounded down
     */
    public long stillAliveFor () {
        assertNotTerminated();
        return (birth +lifeSpan -System.nanoTime())/1_000_000_000L;
    }

    /**
     * @return the callback
     */
    public Callback<V> getCallback () {
        return this.callback;
    }

}
