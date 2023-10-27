package com.github.telos_matter.memeCached.core;

import com.github.telos_matter.memeCached.util.Numbers;

import java.util.Objects;

/**
 * Holds a value of type <code>V</code> for
 * a given life span
 */
public class Value <V> {

    private V value;
    /**
     * Stored in nanoseconds
     */
    private long lifeSpan;
    private final long birth;

    /**
     * @param lifeSpan in seconds
     * @throws NullPointerException if <code>value</code> is <code>null</code>
     * @throws IllegalArgumentException if <code>lifeSpan</code> is negative
     */
    public Value (V value, long lifeSpan) {
        this.birth = System.nanoTime();
        this.value = Objects.requireNonNull(value);
        this.lifeSpan = Numbers.requireNonNegative(lifeSpan) * 1_000_000_000L;
    }

    /**
     * @return whether this value is alive or not
     */
    public boolean isAlive () {
        long age = System.nanoTime() - birth;
        if (age > lifeSpan) {
            kill();
            return false;
        } else {
            return true;
        }
    }

    /**
     * @return the value if it's still alive,
     * <code>null</code> if it's dead
     */
    public V get() {
        if (isAlive()) {
            return value;
        } else {
            return null;
        }
    }

    /**
     * @return <code>true</code> if this value is alive
     * and has been updated, otherwise <code>false</code>
     * @throws NullPointerException if the <code>newValue</code> is <code>null</code>
     */
    public boolean update (V newValue) {
        if (isAlive()) {
            this.value = Objects.requireNonNull(newValue);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Commits Seppuku
     * @return the value it holds before killing itself
     */
    public V kill () {
        V temp = value;
        value = null;
        lifeSpan = -1;
        return temp;
    }

    /**
     * @param duration to extend life span with in seconds
     * @return <code>true</code> if this value is alive
     * and has been extended, otherwise <code>false</code>
     */
    public boolean extend (long duration) {
        if (isAlive()) {
            lifeSpan += duration * 1_000_000_000L;
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return how long is this value still alive for in seconds
     * , or <code>-1</code> if its already dead
     */
    public long stillAliveFor () {
        if (isAlive()) {
            return (birth +lifeSpan -System.nanoTime())/1_000_000_000L;
        } else {
            return -1;
        }
    }

}
