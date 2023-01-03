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
 * {@link CharSequence}, {@code char} array or {@code byte} array.
 */
public class FastFloatParser {

    private static final FloatBitsFromCharArray CHAR_ARRAY_PARSER = new FloatBitsFromCharArray();
    private static final FloatBitsFromCharSequence CHAR_SEQ_PARSER = new FloatBitsFromCharSequence();

    /**
     * Don't let anyone instantiate this class.
     */
    private FastFloatParser() {

    }

    /**
     * Convenience method for calling {@link #parseFloat(CharSequence, int, int)}.
     *
     * @param str the string to be parsed
     * @return the parsed value
     * @throws NumberFormatException if the string can not be parsed
     */
    public static float parseFloat(CharSequence str) throws NumberFormatException {
        return parseFloat(str, 0, str.length());
    }

    /**
     * Parses a {@code FloatingPointLiteral} from a {@link CharSequence} and converts it
     * into a {@code float} value.
     * <p>
     * See {@link com.fasterxml.jackson.core.io.doubleparser} for the syntax of {@code FloatingPointLiteral}.
     *
     * @param str    the string to be parsed
     * @param offset the start offset of the {@code FloatingPointLiteral} in {@code str}
     * @param length the length of {@code FloatingPointLiteral} in {@code str}
     * @return the parsed value
     * @throws NumberFormatException if the string can not be parsed
     */
    public static float parseFloat(CharSequence str, int offset, int length) throws NumberFormatException {
        long bitPattern = CHAR_SEQ_PARSER.parseFloatingPointLiteral(str, offset, length);
        if (bitPattern == AbstractFloatValueParser.PARSE_ERROR) {
            throw new NumberFormatException("Illegal input");
        }
        return Float.intBitsToFloat((int) bitPattern);
    }

    /**
     * Convenience method for calling {@link #parseFloat(char[], int, int)}.
     *
     * @param str the string to be parsed
     * @return the parsed value
     * @throws NumberFormatException if the string can not be parsed
     */
    public static float parseFloat(char[] str) throws NumberFormatException {
        return parseFloat(str, 0, str.length);
    }

    /**
     * Parses a {@code FloatingPointLiteral} from a {@code byte}-Array and converts it
     * into a {@code float} value.
     * <p>
     * See {@link com.fasterxml.jackson.core.io.doubleparser} for the syntax of {@code FloatingPointLiteral}.
     *
     * @param str    the string to be parsed, a byte array with characters
     *               in ISO-8859-1, ASCII or UTF-8 encoding
     * @param offset The index of the first character to parse
     * @param length The number of characters to parse
     * @return the parsed value
     * @throws NumberFormatException if the string can not be parsed
     */
    public static float parseFloat(char[] str, int offset, int length) throws NumberFormatException {
        long bitPattern = CHAR_ARRAY_PARSER.parseFloatingPointLiteral(str, offset, length);
        if (bitPattern == AbstractFloatValueParser.PARSE_ERROR) {
            throw new NumberFormatException("Illegal input");
        }
        return Float.intBitsToFloat((int) bitPattern);
    }

    /**
     * Parses a {@code FloatingPointLiteral} from a {@link CharSequence} and converts it
     * into a bit pattern that encodes a {@code float} value.
     * <p>
     * See {@link com.fasterxml.jackson.core.io.doubleparser} for the syntax of {@code FloatingPointLiteral}.
     * <p>
     * Usage example:
     * <pre>
     *     long bitPattern = parseFloatBits("3.14", 0, 4);
     *     if (bitPattern == -1L) {
     *         ...handle parse error...
     *     } else {
     *         double d = Double.longBitsToDouble(bitPattern);
     *     }
     * </pre>
     *
     * @param str    the string to be parsed
     * @param offset the start offset of the {@code FloatingPointLiteral} in {@code str}
     * @param length the length of {@code FloatingPointLiteral} in {@code str}
     * @return the bit pattern of the parsed value, if the input is legal;
     * otherwise, {@code -1L}.
     */
    public static long parseFloatBits(CharSequence str, int offset, int length) {
        return CHAR_SEQ_PARSER.parseFloatingPointLiteral(str, offset, length);
    }

    /**
     * Parses a {@code FloatingPointLiteral} from a {@code byte}-Array and converts it
     * into a bit pattern that encodes a {@code float} value.
     * <p>
     * See {@link com.fasterxml.jackson.core.io.doubleparser} for the syntax of {@code FloatingPointLiteral}.
     * <p>
     * See {@link #parseFloatBits(CharSequence, int, int)} for a usage example.
     *
     * @param str    the string to be parsed, a byte array with characters
     *               in ISO-8859-1, ASCII or UTF-8 encoding
     * @param offset The index of the first character to parse
     * @param length The number of characters to parse
     * @return the bit pattern of the parsed value, if the input is legal;
     * otherwise, {@code -1L}.
     */
    public static long parseFloatBits(char[] str, int offset, int length) {
        return CHAR_ARRAY_PARSER.parseFloatingPointLiteral(str, offset, length);
    }
}