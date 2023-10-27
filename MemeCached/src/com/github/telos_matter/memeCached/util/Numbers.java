package com.github.telos_matter.memeCached.util;

public class Numbers {

    /**
     * @throws IllegalArgumentException if the number is negative
     */
    public static <T extends Number> T requireNonNegative (T number) {
        if (number.doubleValue() < 0) {
            throw new IllegalArgumentException("The passed number is negative: "+ number);
        }
        return number;
    }

}
