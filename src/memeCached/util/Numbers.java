package memeCached.util;

/**
 * A utility class for Numbers
 */
public class Numbers {

    /**
     * @throws IllegalArgumentException if the number is negative
     * @return the given number
     */
    public static <T extends Number> T requireNonNegative (T number) {
        if (number.doubleValue() < 0) {
            throw new IllegalArgumentException("The given number is negative: "+ number);
        }
        return number;
    }

}
