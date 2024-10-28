package tools.jackson.core.util;

/**
 * Internal Use Only. Helper class used to contain some useful utility methods.
 *
 * @since 2.17.3 / 2.18.1
 */
public abstract class InternalJacksonUtil {
    /**
     * Internal Use Only.
     * <p>
     * Method that will add two non-negative integers, and if result overflows, return
     * {@link Integer#MAX_VALUE}. For performance reasons, does NOT check for
     * the result being less than {@link Integer#MIN_VALUE}, nor whether arguments
     * are actually non-negative.
     * This is usually used to implement overflow-safe bounds checking.
     */
    public static int addOverflowSafe(final int base, final int length) {
        int result = base + length;
        if (result < 0) {
             return Integer.MAX_VALUE;
        }
        return result;
    }
}
