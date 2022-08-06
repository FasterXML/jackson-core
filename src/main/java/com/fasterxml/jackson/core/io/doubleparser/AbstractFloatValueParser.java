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
 * Abstract base class for parsers that parse a {@code FloatingPointLiteral} from a
 * character sequence ({@code str}).
 * <p>
 * This is a C++ to Java port of Daniel Lemire's fast_double_parser.
 * <p>
 * References:
 * <dl>
 *     <dt>Daniel Lemire, fast_double_parser, 4x faster than strtod.
 *     Apache License 2.0 or Boost Software License.</dt>
 *     <dd><a href="https://github.com/lemire/fast_double_parser">github.com</a></dd>
 *
 *     <dt>Daniel Lemire, fast_float number parsing library: 4x faster than strtod.
 *     Apache License 2.0.</dt>
 *     <dd><a href="https://github.com/fastfloat/fast_float">github.com</a></dd>
 *
 *     <dt>Daniel Lemire, Number Parsing at a Gigabyte per Second,
 *     Software: Practice and Experience 51 (8), 2021.
 *     arXiv.2101.11408v3 [cs.DS] 24 Feb 2021</dt>
 *     <dd><a href="https://arxiv.org/pdf/2101.11408.pdf">arxiv.org</a></dd>
 * </dl>
 */
abstract class AbstractFloatValueParser {
    /**
     * This return value indicates that the input value is not a legal
     * production of the syntax rule for a float value.
     * <p>
     * The value is {@code -1L}.
     * <p>
     * Converting this value to a floating point number using
     * {@code Double.longBitsToDouble(-1L)}, or
     * {@code Float.intBitsToFloat((int) -1L)} yields a {@code NaN}.
     * <p>
     * Although this number yields {@code NaN}, the bit pattern of this value is
     * intentionally different from {@link Double#NaN}, and {@link Float#NaN}.
     */
    final static long PARSE_ERROR = -1L;
    final static long MINIMAL_NINETEEN_DIGIT_INTEGER = 1000_00000_00000_00000L;
    /**
     * The decimal exponent of a double has a range of -324 to +308.
     * The hexadecimal exponent of a double has a range of -1022 to +1023.
     */
    final static int MAX_EXPONENT_NUMBER = 1024;
    /**
     * Special value in {@link #CHAR_TO_HEX_MAP} for
     * the decimal point character.
     */
    static final byte DECIMAL_POINT_CLASS = -4;
    /**
     * Special value in {@link #CHAR_TO_HEX_MAP} for
     * characters that are neither a hex digit nor
     * a decimal point character..
     */
    static final byte OTHER_CLASS = -1;
    /**
     * Includes all non-negative values of a {@code byte}, so that we only have
     * to check for byte values {@literal <} 0 before accessing this array.
     */
    static final byte[] CHAR_TO_HEX_MAP = new byte[128];

    static {
        for (char ch = 0; ch < AbstractFloatValueParser.CHAR_TO_HEX_MAP.length; ch++) {
            AbstractFloatValueParser.CHAR_TO_HEX_MAP[ch] = AbstractFloatValueParser.OTHER_CLASS;
        }
        for (char ch = '0'; ch <= '9'; ch++) {
            AbstractFloatValueParser.CHAR_TO_HEX_MAP[ch] = (byte) (ch - '0');
        }
        for (char ch = 'A'; ch <= 'F'; ch++) {
            AbstractFloatValueParser.CHAR_TO_HEX_MAP[ch] = (byte) (ch - 'A' + 10);
        }
        for (char ch = 'a'; ch <= 'f'; ch++) {
            AbstractFloatValueParser.CHAR_TO_HEX_MAP[ch] = (byte) (ch - 'a' + 10);
        }
        for (char ch = '.'; ch <= '.'; ch++) {
            AbstractFloatValueParser.CHAR_TO_HEX_MAP[ch] = AbstractFloatValueParser.DECIMAL_POINT_CLASS;
        }
    }
}
