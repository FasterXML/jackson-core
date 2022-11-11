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
 * Provides static method for parsing a {@code double} from a
 * {@link CharSequence}, {@code char} array or {@code byte} array.
 * <p>
 * <b>Syntax</b>
 * <p>
 * Numeric values that cannot be represented in the grammar below (such
 * as Infinity and NaN) are not permitted.
 * <pre>
 * number = [ minus ] int [ frac ] [ exp ]
 *
 * minus  = %x2D                        ; -
 * int    = zero / ( digit1-9 *DIGIT )
 * frac   = decimal-point 1*DIGIT
 * exp    = e [ minus / plus ] 1*DIGIT
 *
 * decimal-point = %x2E                 ; .
 * digit1-9      = %x31-39              ; 1-9
 * e             = %x65 / %x45          ; e E
 * plus          = %x2B                 ; +
 * zero          = %x30                 ; 0
 * </pre>
 * <p>
 * References:
 * <dl>
 *     <dt>IETF RFC 8259. The JavaScript Object Notation (JSON) Data Interchange
 *     Format, Chapter 6. Numbers</dt>
 *     <dd><a href="https://www.ietf.org/rfc/rfc8259.txt">www.ietf.org</a></dd>
 * </dl>
 */
public class JsonDoubleParser {


    /**
     * Don't let anyone instantiate this class.
     */
    private JsonDoubleParser() {

    }

    /**
     * Convenience method for calling {@link #parseDouble(CharSequence, int, int)}.
     *
     * @param str the string to be parsed
     * @return the parsed double value
     * @throws NumberFormatException if the string can not be parsed
     */
    public static double parseDouble(CharSequence str) throws NumberFormatException {
        return parseDouble(str, 0, str.length());
    }

    /**
     * Parses a {@code FloatingPointLiteral} from a {@link CharSequence} and converts it
     * into a {@code double} value.
     *
     * @param str    the string to be parsed
     * @param offset the start offset of the {@code FloatingPointLiteral} in {@code str}
     * @param length the length of {@code FloatingPointLiteral} in {@code str}
     * @return the parsed double value
     * @throws NumberFormatException if the string can not be parsed
     */
    public static double parseDouble(CharSequence str, int offset, int length) throws NumberFormatException {
        long bitPattern = new JsonDoubleBitsFromCharSequence().parseNumber(str, offset, length);
        if (bitPattern == AbstractFloatValueParser.PARSE_ERROR) {
            throw new NumberFormatException("Illegal input");
        }
        return Double.longBitsToDouble(bitPattern);
    }

    /**
     * Convenience method for calling {@link #parseDouble(byte[], int, int)}.
     *
     * @param str the string to be parsed, a byte array with characters
     *            in ISO-8859-1, ASCII or UTF-8 encoding
     * @return the parsed double value
     * @throws NumberFormatException if the string can not be parsed
     */
    public static double parseDouble(byte[] str) throws NumberFormatException {
        return parseDouble(str, 0, str.length);
    }

    /**
     * Parses a {@code FloatingPointLiteral} from a {@code byte}-Array and converts it
     * into a {@code double} value.
     *
     * @param str    the string to be parsed, a byte array with characters
     *               in ISO-8859-1, ASCII or UTF-8 encoding
     * @param offset The index of the first byte to parse
     * @param length The number of bytes to parse
     * @return the parsed double value
     * @throws NumberFormatException if the string can not be parsed
     */
    public static double parseDouble(byte[] str, int offset, int length) throws NumberFormatException {
        long bitPattern = new JsonDoubleBitsFromByteArray().parseNumber(str, offset, length);
        if (bitPattern == AbstractFloatValueParser.PARSE_ERROR) {
            throw new NumberFormatException("Illegal input");
        }
        return Double.longBitsToDouble(bitPattern);
    }

    /**
     * Convenience method for calling {@link #parseDouble(char[], int, int)}.
     *
     * @param str the string to be parsed
     * @return the parsed double value
     * @throws NumberFormatException if the string can not be parsed
     */
    public static double parseDouble(char[] str) throws NumberFormatException {
        return parseDouble(str, 0, str.length);
    }

    /**
     * Parses a {@code FloatingPointLiteral} from a {@code byte}-Array and converts it
     * into a {@code double} value.
     * <p>
     * See {@link JsonDoubleParser} for the syntax of {@code FloatingPointLiteral}.
     *
     * @param str    the string to be parsed, a byte array with characters
     *               in ISO-8859-1, ASCII or UTF-8 encoding
     * @param offset The index of the first character to parse
     * @param length The number of characters to parse
     * @return the parsed double value
     * @throws NumberFormatException if the string can not be parsed
     */
    public static double parseDouble(char[] str, int offset, int length) throws NumberFormatException {
        long bitPattern = new JsonDoubleBitsFromCharArray().parseNumber(str, offset, length);
        if (bitPattern == AbstractFloatValueParser.PARSE_ERROR) {
            throw new NumberFormatException("Illegal input");
        }
        return Double.longBitsToDouble(bitPattern);
    }

    /**
     * Parses a {@code FloatingPointLiteral} from a {@link CharSequence} and converts it
     * into a bit pattern that encodes a {@code double} value.
     * <p>
     * Usage example:
     * <pre>
     *     long bitPattern = parseDoubleBits("3.14", 0, 4);
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
    public static long parseDoubleBits(CharSequence str, int offset, int length) {
        return new JsonDoubleBitsFromCharSequence().parseNumber(str, offset, length);
    }

    /**
     * Parses a {@code FloatingPointLiteral} from a {@code byte}-Array and converts it
     * into a bit pattern that encodes a {@code double} value.
     * <p>
     * See {@link #parseDoubleBits(CharSequence, int, int)} for a usage example.
     *
     * @param str    the string to be parsed, a byte array with characters
     *               in ISO-8859-1, ASCII or UTF-8 encoding
     * @param offset The index of the first byte to parse
     * @param length The number of bytes to parse
     * @return the bit pattern of the parsed value, if the input is legal;
     * otherwise, {@code -1L}.
     */
    public static long parseDoubleBits(byte[] str, int offset, int length) {
        return new JsonDoubleBitsFromByteArray().parseNumber(str, offset, length);
    }

    /**
     * Parses a {@code FloatingPointLiteral} from a {@code byte}-Array and converts it
     * into a bit pattern that encodes a {@code double} value.
     * <p>
     * See {@link #parseDoubleBits(CharSequence, int, int)} for a usage example.
     *
     * @param str    the string to be parsed, a byte array with characters
     *               in ISO-8859-1, ASCII or UTF-8 encoding
     * @param offset The index of the first character to parse
     * @param length The number of characters to parse
     * @return the bit pattern of the parsed value, if the input is legal;
     * otherwise, {@code -1L}.
     */
    public static long parseDoubleBits(char[] str, int offset, int length) {
        return new JsonDoubleBitsFromCharArray().parseNumber(str, offset, length);
    }
}