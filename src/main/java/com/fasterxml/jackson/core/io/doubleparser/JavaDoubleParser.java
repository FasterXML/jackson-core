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
 * Leading and trailing whitespace characters in {@code str} are ignored.
 * Whitespace is removed as if by the {@link String#trim()} method;
 * that is, characters in the range [U+0000,U+0020].
 * <p>
 * The rest of {@code str} should constitute a Java {@code FloatingPointLiteral}
 * as described by the lexical syntax rules shown below:
 * <blockquote>
 * <dl>
 * <dt><i>FloatingPointLiteral:</i></dt>
 * <dd><i>[Sign]</i> {@code NaN}</dd>
 * <dd><i>[Sign]</i> {@code Infinity}</dd>
 * <dd><i>[Sign] DecimalFloatingPointLiteral</i></dd>
 * <dd><i>[Sign] HexFloatingPointLiteral</i></dd>
 * <dd><i>SignedInteger</i></dd>
 * </dl>
 *
 * <dl>
 * <dt><i>HexFloatingPointLiteral</i>:
 * <dd><i>HexSignificand BinaryExponent [FloatTypeSuffix]</i>
 * </dl>
 *
 * <dl>
 * <dt><i>HexSignificand:</i>
 * <dd><i>HexNumeral</i>
 * <dd><i>HexNumeral</i> {@code .}
 * <dd>{@code 0x} <i>[HexDigits]</i> {@code .} <i>HexDigits</i>
 * <dd>{@code 0X} <i>[HexDigits]</i> {@code .} <i>HexDigits</i>
 * </dl>
 *
 * <dl>
 * <dt><i>BinaryExponent:</i>
 * <dd><i>BinaryExponentIndicator SignedInteger</i>
 * </dl>
 *
 * <dl>
 * <dt><i>BinaryExponentIndicator:</i>
 * <dd>{@code p}
 * <dd>{@code P}
 * </dl>
 *
 * <dl>
 * <dt><i>DecimalFloatingPointLiteral:</i>
 * <dd><i>DecSignificand [DecExponent] [FloatTypeSuffix]</i>
 * </dl>
 *
 * <dl>
 * <dt><i>DecSignificand:</i>
 * <dd><i>IntegerPart {@code .} [FractionPart]</i>
 * <dd><i>{@code .} FractionPart</i>
 * <dd><i>IntegerPart</i>
 * </dl>
 *
 * <dl>
 * <dt><i>IntegerPart:</i>
 * <dd><i>Digits</i>
 * </dl>
 *
 * <dl>
 * <dt><i>FractionPart:</i>
 * <dd><i>Digits</i>
 * </dl>
 *
 * <dl>
 * <dt><i>DecExponent:</i>
 * <dd><i>ExponentIndicator SignedInteger</i>
 * </dl>
 *
 * <dl>
 * <dt><i>ExponentIndicator:</i>
 * <dd><i>e</i>
 * <dd><i>E</i>
 * </dl>
 *
 * <dl>
 * <dt><i>SignedInteger:</i>
 * <dd><i>[Sign] Digits</i>
 * </dl>
 *
 * <dl>
 * <dt><i>Sign:</i>
 * <dd><i>+</i>
 * <dd><i>-</i>
 * </dl>
 *
 * <dl>
 * <dt><i>Digits:</i>
 * <dd><i>Digit {Digit}</i>
 * </dl>
 *
 * <dl>
 * <dt><i>Digit:</i>
 * <dd><i>(one of)</i>
 * <dd>{@code 0 1 2 3 4 5 6 7 8 9}
 * </dl>
 *
 * <dl>
 * <dt><i>HexNumeral:</i>
 * <dd>{@code 0} {@code x} <i>HexDigits</i>
 * <dd>{@code 0} {@code X} <i>HexDigits</i>
 * </dl>
 *
 * <dl>
 * <dt><i>HexDigits:</i>
 * <dd><i>HexDigit {HexDigit}</i>
 * </dl>
 *
 * <dl>
 * <dt><i>HexDigit:</i>
 * <dd><i>(one of)</i>
 * <dd>{@code 0 1 2 3 4 5 6 7 8 9 a b c d e f A B C D E F}
 * </dl>
 *
 * <dl>
 * <dt><i>FloatTypeSuffix:</i>
 * <dd><i>(one of)</i>
 * <dd>{@code f F d D}
 * </dl>
 * </blockquote>
 * <p>
 * Expected character lengths for values produced by {@link Double#toString}:
 * <ul>
 *     <li>{@code DecSignificand} ({@code IntegerPart} + {@code FractionPart}):
 *     1 to 17 digits</li>
 *     <li>{@code IntegerPart}: 1 to 7 digits</li>
 *     <li>{@code FractionPart}: 1 to 16 digits</li>
 *     <li>{@code SignedInteger} in exponent: 1 to 3 digits</li>
 *     <li>{@code FloatingPointLiteral}: 1 to 24 characters, e.g. "-1.2345678901234568E-300"</li>
 * </ul>
 *
 * Expected character lengths for values produced by {@link Float#toString}:
 * <ul>
 *     <li>{@code DecSignificand} ({@code IntegerPart} + {@code FractionPart}):
 *     1 to 8 digits</li>
 *     <li>{@code IntegerPart}: 1 to 7 digits</li>
 *     <li>{@code FractionPart}: 1 to 7 digits</li>
 *     <li>{@code SignedInteger} in exponent: 1 to 2 digits</li>
 *     <li>{@code FloatingPointLiteral}: 1 to 14 characters, e.g. "-1.2345678E-38"</li>
 * </ul>
 *
 * References:
 * <dl>
 *     <dt>The Java® Language Specification, Java SE 18 Edition, Chapter 3. Lexical Structure, 3.10.2. Floating-Point Literals </dt>
 *     <dd><a href="https://docs.oracle.com/javase/specs/jls/se18/html/jls-3.html#jls-3.10.2">docs.oracle.com</a></dd>
 * </dl>
 */
public class JavaDoubleParser {


    /**
     * Don't let anyone instantiate this class.
     */
    private JavaDoubleParser() {

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
        long bitPattern = new JavaDoubleBitsFromCharSequence().parseFloatingPointLiteral(str, offset, length);
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
     * See {@link JavaDoubleParser} for the syntax of {@code FloatingPointLiteral}.
     *
     * @param str    the string to be parsed, a byte array with characters
     *               in ISO-8859-1, ASCII or UTF-8 encoding
     * @param offset The index of the first character to parse
     * @param length The number of characters to parse
     * @return the parsed double value
     * @throws NumberFormatException if the string can not be parsed
     */
    public static double parseDouble(char[] str, int offset, int length) throws NumberFormatException {
        long bitPattern = new JavaDoubleBitsFromCharArray().parseFloatingPointLiteral(str, offset, length);
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
        return new JavaDoubleBitsFromCharSequence().parseFloatingPointLiteral(str, offset, length);
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
        return new JavaDoubleBitsFromCharArray().parseFloatingPointLiteral(str, offset, length);
    }
}