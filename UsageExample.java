import com.github.telos_matter.memeCached.MemeCached;

// This example won't work as is, because the jar should be included with the build
public class UsageExample {

    public static void main(String[] args) throws InterruptedException {
        MemeCached <String, Integer> cache = new MemeCached<>();

        cache.cache("first", 1, 1);
        cache.cache("second", 2, 2);

        System.out.println(cache.size()); // 2, all values are still alive
        Thread.sleep(1000);
        System.out.println(cache.size()); // 1, it forgot about the first
        Thread.sleep(1000);
        System.out.println(cache.size()); // 0, it forgot about the second
    }
}
