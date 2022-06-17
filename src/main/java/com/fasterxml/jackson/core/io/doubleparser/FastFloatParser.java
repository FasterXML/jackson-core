/**
 * References:
 * <dl>
 *     <dt>This class has been derived from "FastDoubleParser".</dt>
 *     <dd>Copyright (c) Werner Randelshofer. Apache 2.0 License.
 *         <a href="https://github.com/wrandelshofer/FastDoubleParser">github.com</a>.</dd>
 * </dl>
 */

package com.fasterxml.jackson.core.io.doubleparser;

/**
 * Provides static method for parsing a {@code float} from a
 * {@link CharSequence} or {@code char} array.
 */
public class FastFloatParser {

    /**
     * Don't let anyone instantiate this class.
     */
    private FastFloatParser() {

    }

    /**
     * Convenience method for calling {@link #parseFloat(CharSequence, int, int)}.
     *
     * @param str the string to be parsed
     * @return the parsed float value
     * @throws NumberFormatException if the string can not be parsed
     */
    public static float parseFloat(CharSequence str) throws NumberFormatException {
        return parseFloat(str, 0, str.length());
    }

    /**
     * Parses a {@code FloatValue} from a {@link CharSequence} and converts it
     * into a {@code float} value.
     * <p>
     * See {@link com.fasterxml.jackson.core.io.doubleparser} for the syntax of {@code FloatValue}.
     *
     * @param str    the string to be parsed
     * @param offset the start offset of the {@code FloatValue} in {@code str}
     * @param length the length of {@code FloatValue} in {@code str}
     * @return the parsed float value
     * @throws NumberFormatException if the string can not be parsed
     */
    public static float parseFloat(CharSequence str, int offset, int length) throws NumberFormatException {
        return FloatFromCharSequence.INSTANCE.parseFloat(str, offset, length);
    }

}
