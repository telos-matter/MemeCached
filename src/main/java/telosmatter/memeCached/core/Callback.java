package telosmatter.memeCached.core;

import telosmatter.memeCached.MemeCached;

/**
 * A Functional Interface to define a callback function
 * that will get called when a <code>V</code> type value dies.
 */
@FunctionalInterface
public interface Callback <V> {

    /**
     * The callback function that will get called when
     * a <code>V</code> type value dies.
     * The arguments are supplied by the value that died. You
     * just define the function..
     *
     * @param value the value that died.
     * @param memeCached the {@link MemeCached} the value belonged to.
     * @param totalLifeTime how long the value actually lived for, rounded to the closest second.
     * @param thisCallback a reference to this callback itself.
     */
    public abstract void callback (V value, MemeCached <?, V> memeCached, long totalLifeTime, Callback <V> thisCallback);

}
