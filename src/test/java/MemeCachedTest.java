import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import telosmatter.memeCached.MemeCached;
import telosmatter.memeCached.core.Callback;

import static org.junit.jupiter.api.Assertions.*;

public class MemeCachedTest {

    @Test
    public void example_1 () throws InterruptedException {
        // A simple usage example

        // Create the map
        var map = new MemeCached<Integer, String>();

        // Cache a value for 1 second
        map.cache(5, "five", 1);
        // Size will be 1
        assertEquals(1, map.size());
        // Value exists
        assertEquals("five", map.get(5));
        // The default value will not be returned
        assertNotEquals("default five", map.get(5, "default five"));

        // We wait for a second to elapse
        Thread.sleep(1000);

        // Size should be now 0
        assertEquals(0, map.size());
        // Value should no longer be there
        assertNull(map.get(5));
        // We can get a default value
        assertEquals("default five", map.get(5, "default five"));
    }

    private static boolean EXAMPLE_2_FLAG = false;
    @Test
    public void example_2 () throws InterruptedException {
        // Use case with a Callback

        // Create the map
        var map = new MemeCached<Integer, String>();

        // Create a callback for Strings
        var callback = new Callback<String>(){
            @Override
            public void callback(String value, MemeCached<?, String> memeCached, long totalLifeTime, Callback<String> thisCallback) {
                // We will set the flag to assert that the callback was called
                MemeCachedTest.EXAMPLE_2_FLAG = true;
            }
        };

        // Flag should be false
        assertFalse(EXAMPLE_2_FLAG);

        // Cache value for 1 second
        map.cache(7, "seven", 1, callback);

        // Wait for second to elapse
        Thread.sleep(1000);

        // Right now, even though a second has elapsed, the callback
        // is still not called! Check the documentation on MemeCached
        // to better understand how it works
        assertFalse(EXAMPLE_2_FLAG);

        // If we "ping" the map, now the value should be forgotten about
        // and the Callback should be called
        assertEquals(0, map.size());
        assertTrue(EXAMPLE_2_FLAG);
    }

    // That's it for the examples.
    // The rest are actual tests.

    @Test
    public void constructorsTest () {
        // No exception no nothing
        var map = new MemeCached<>();

        // Config check
        map = new MemeCached<>(true);
        assertTrue(map.isSynchronous());

        // Config check
        var callback = new Callback<>(
        ) {
            @Override
            public void callback(Object value, MemeCached<?, Object> memeCached, long totalLifeTime, Callback<Object> thisCallback) {
                return;
            }
        };
        map = new MemeCached<>(MemeCached._1_WEEK, callback, true);
        assertEquals(MemeCached._1_WEEK, map.getDefaultLifeSpan());
        assertSame(callback, map.getDefaultCallback());
        assertTrue(map.isSynchronous());
    }

    @Test
    public void settersAndGettersTest () {
        var map = new MemeCached<>();

        // DefaultLifeSpan
        var newValue = map.getDefaultLifeSpan() +1;
        map.setDefaultLifeSpan(newValue);
        assertEquals(newValue, map.getDefaultLifeSpan());

        // DefaultCallback
        var newCallback = new Callback<>(
        ) {
            @Override
            public void callback(Object value, MemeCached<?, Object> memeCached, long totalLifeTime, Callback<Object> thisCallback) {}
        };
        map.setDefaultCallback(newCallback);
        assertSame(newCallback, map.getDefaultCallback());
    }

    @Test
    public void basicTests () {
        // Nothing ATM. The examples pretty much test
        // the essentials
    }

    @BeforeAll
    public static void beforeAll () {
        System.out.println("The tests will take a couple of seconds..");
    }

}