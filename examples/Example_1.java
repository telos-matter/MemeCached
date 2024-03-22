import memeCached.MemeCached;

// This example won't work as is, because the jar
// should be included with the build of this file
public class Example_1 {

    public static void main(String[] args) throws InterruptedException {
        // Create an instance of the map
        MemeCached <String, Integer> map = new MemeCached<>();

        // Cache some values
        map.cache("first", 1, 1, null); // Save a value for 1 second
        map.cache("second", 2, 2, null); // Save another value but for 2 seconds

        System.out.println(map.size()); // 2, all values are still alive
        Thread.sleep(1000);
        System.out.println(map.size()); // 1, it forgot about the first
        Thread.sleep(1000);
        System.out.println(map.size()); // 0, it forgot about the second
    }
}
