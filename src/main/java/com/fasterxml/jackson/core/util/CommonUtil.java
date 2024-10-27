package com.fasterxml.jackson.core.util;

/**
 * Internal Use Only. Helper class used some useful utility methods.
 */
public class CommonUtil {

    private CommonUtil() {}

    /**
     * Internal Use Only.
     * <p>
     * Method that will add two integers, and if result overflows, return
     * {@link Integer#MAX_VALUE}. For performance reasons, does NOT check for
     * the result being less than {@link Integer#MIN_VALUE}.
     * </p>
     */
    public static int addWithOverflowDefault(final int a, final int b) {
        final long result = (long) a + (long) b;
        if (result > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) result;
    }
}
