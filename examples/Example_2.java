import memeCached.MemeCached;
import memeCached.core.Callback;

// This example won't work as is, because the jar
// should be included with the build of this file
public class Example_2 {

    // Same example as before, but this one shows the use of callbacks
    public static void main(String[] args) throws InterruptedException {
        // Create an instance of the map
        MemeCached <String, Integer> map = new MemeCached<>();

        // Define a callback
        Callback <Integer> callback = (value, _map, age, _callback) -> {
            System.out.println("This value just died: " + value);
        };

        // Cache some values with the callback this time
        map.cache("first", 1, 1, callback);
        map.cache("second", 2, 2, callback);

        System.out.println(map.size()); // 2, all values are still alive
        Thread.sleep(1000);
        System.out.println(map.size()); // 1, it forgot about the first, and it's callback was called
        Thread.sleep(1000);
        System.out.println(map.size()); // 0, it forgot about the second, and the callback was called
    }
}
