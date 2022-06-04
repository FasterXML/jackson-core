/*
 * @(#)AbstractFloatValueFromCharSequenceParser.java
 * Copyright © 2022. Werner Randelshofer, Switzerland. MIT License.
 */

package com.fasterxml.jackson.core.io.doubleparser;

/**
 * Parses a {@code FloatValue} from a {@link CharSequence}.
 * <p>
 * See {@link com.fasterxml.jackson.core.io.doubleparser} for the grammar of
 * {@code FloatValue}.
 */
abstract class AbstractFloatValueFromCharSequence extends AbstractFloatValueParser {

    private boolean isDigit(char c) {
        return '0' <= c && c <= '9';
    }

    abstract long nan();

    abstract long negativeInfinity();

    /**
     * Creates a new {@link NumberFormatException} for the provided string.
     *
     * @param str        a string containing a {@code FloatValue} production
     * @param startIndex start index (inclusive) of the {@code FloatValue} production in str
     * @param endIndex   end index (exclusive) of the {@code FloatValue} production in str
     * @return a new  {@link NumberFormatException}
     */
    private NumberFormatException newNumberFormatException(CharSequence str, int startIndex, int endIndex) {
        if (endIndex - startIndex > 1024) {
            // str can be up to Integer.MAX_VALUE characters long
            return new NumberFormatException("For input string of length " + (endIndex - startIndex));
        } else {
            return new NumberFormatException("For input string: \"" + str.subSequence(startIndex, endIndex) + "\"");
        }
    }

    /**
     * Parses a {@code DecimalFloatingPointLiteral} production with optional
     * trailing white space until the end of the text.
     * Given that we have already consumed the optional leading zero of
     * the {@code DecSignificand}.
     * <blockquote>
     * <dl>
     * <dt><i>DecimalFloatingPointLiteralWithWhiteSpace:</i></dt>
     * <dd><i>DecimalFloatingPointLiteral [WhiteSpace] EOT</i></dd>
     * </dl>
     * </blockquote>
     * See {@link com.fasterxml.jackson.core.io.doubleparser} for the grammar of
     * {@code DecimalFloatingPointLiteral} and {@code DecSignificand}.
     *
     * @param str            a string
     * @param index          start index inclusive of the {@code DecimalFloatingPointLiteralWithWhiteSpace}
     * @param endIndex       end index (exclusive)
     * @param isNegative     true if the float value is negative
     * @param hasLeadingZero true if we have consumed the optional leading zero
     * @return a float value
     * @throws NumberFormatException on parsing failure
     */
    private long parseDecimalFloatLiteral(CharSequence str, int index, int startIndex, int endIndex, boolean isNegative, boolean hasLeadingZero) {
        // Parse significand
        // -----------------
        // Note: a multiplication by a constant is cheaper than an
        //       arbitrary integer multiplication.
        long significand = 0;// significand is treated as an unsigned long
        final int significandStartIndex = index;
        int virtualIndexOfPoint = -1;
        boolean illegal = false;
        char ch = 0;
        for (; index < endIndex; index++) {
            ch = str.charAt(index);
            if (isDigit(ch)) {
                // This might overflow, we deal with it later.
                significand = 10 * significand + ch - '0';
            } else if (ch == '.') {
                illegal |= virtualIndexOfPoint >= 0;
                virtualIndexOfPoint = index;
                for (;index < endIndex - 8;index += 8) {
                    int eightDigits = tryToParseEightDigits(str, index + 1);
                    if (eightDigits < 0) {
                        break;
                    }
                    // This might overflow, we deal with it later.
                    significand = 100_000_000L * significand + eightDigits;
                }
            } else {
                break;
            }
        }
        final int digitCount;
        final int significandEndIndex = index;
        int exponent;
        if (virtualIndexOfPoint < 0) {
            digitCount = index - significandStartIndex;
            virtualIndexOfPoint = index;
            exponent = 0;
        } else {
            digitCount = index - significandStartIndex - 1;
            exponent = virtualIndexOfPoint - index + 1;
        }

        // Parse exponent number
        // ---------------------
        int expNumber = 0;
        if (ch == 'e' || ch == 'E') {
            ch = ++index < endIndex ? str.charAt(index) : 0;
            boolean neg_exp = ch == '-';
            if (neg_exp || ch == '+') {
                ch = ++index < endIndex ? str.charAt(index) : 0;
            }
            illegal |= !isDigit(ch);
            do {
                // Guard against overflow
                if (expNumber < AbstractFloatValueParser.MAX_EXPONENT_NUMBER) {
                    expNumber = 10 * expNumber + ch - '0';
                }
                ch = ++index < endIndex ? str.charAt(index) : 0;
            } while (isDigit(ch));
            if (neg_exp) {
                expNumber = -expNumber;
            }
            exponent += expNumber;
        }

        // Skip trailing whitespace and check if FloatValue is complete
        // ------------------------
        index = skipWhitespace(str, index, endIndex);
        if (illegal || index < endIndex
                || !hasLeadingZero && digitCount == 0) {
            throw newNumberFormatException(str, startIndex, endIndex);
        }

        // Re-parse significand in case of a potential overflow
        // -----------------------------------------------
        final boolean isSignificandTruncated;
        int skipCountInTruncatedDigits = 0;//counts +1 if we skipped over the decimal point
        int exponentOfTruncatedSignificand;
        if (digitCount > 19) {
            significand = 0;
            for (index = significandStartIndex; index < significandEndIndex; index++) {
                ch = str.charAt(index);
                if (ch == '.') {
                    skipCountInTruncatedDigits++;
                } else {
                    if (Long.compareUnsigned(significand, AbstractFloatValueParser.MINIMAL_NINETEEN_DIGIT_INTEGER) < 0) {
                        significand = 10 * significand + ch - '0';
                    } else {
                        break;
                    }
                }
            }
            isSignificandTruncated = (index < significandEndIndex);
            exponentOfTruncatedSignificand = virtualIndexOfPoint - index + skipCountInTruncatedDigits + expNumber;
        } else {
            isSignificandTruncated = false;
            exponentOfTruncatedSignificand = 0;
        }
        return valueOfFloatLiteral(str, startIndex, endIndex, isNegative, significand, exponent, isSignificandTruncated,
                exponentOfTruncatedSignificand);
    }

    /**
     * Parses a {@code FloatValue} production with optional leading and trailing
     * white space.
     * <blockquote>
     * <dl>
     * <dt><i>FloatValueWithWhiteSpace:</i></dt>
     * <dd><i>[WhiteSpace] FloatValue [WhiteSpace]</i></dd>
     * </dl>
     * </blockquote>
     * See {@link com.fasterxml.jackson.core.io.doubleparser} for the grammar of
     * {@code FloatValue}.
     *
     * @param str    a string containing a {@code FloatValueWithWhiteSpace}
     * @param offset start offset of {@code FloatValueWithWhiteSpace} in {@code str}
     * @param length length of {@code FloatValueWithWhiteSpace} in {@code str}
     * @return the parsed value
     * @throws NumberFormatException on parsing failure
     */
    long parseFloatValue(CharSequence str, int offset, int length) throws NumberFormatException {
        final int endIndex = offset + length;

        // Skip leading whitespace
        // -------------------
        int index = skipWhitespace(str, offset, endIndex);
        if (index == endIndex) {
            throw new NumberFormatException("empty String");
        }
        char ch = str.charAt(index);

        // Parse optional sign
        // -------------------
        final boolean isNegative = ch == '-';
        if (isNegative || ch == '+') {
            ch = ++index < endIndex ? str.charAt(index) : 0;
            if (ch == 0) {
                throw newNumberFormatException(str, offset, endIndex);
            }
        }

        // Parse NaN or Infinity
        // ---------------------
        if (ch >= 'I') {
            return ch == 'N'
                    ? parseNaN(str, index, endIndex)
                    : parseInfinity(str, index, endIndex, isNegative);
        }

        // Parse optional leading zero
        // ---------------------------
        final boolean hasLeadingZero = ch == '0';
        if (hasLeadingZero) {
            ch = ++index < endIndex ? str.charAt(index) : 0;
            if (ch == 'x' || ch == 'X') {
                return parseHexFloatingPointLiteral(str, index + 1, offset, endIndex, isNegative);
            }
        }

        return parseDecimalFloatLiteral(str, index, offset, endIndex, isNegative, hasLeadingZero);
    }

    /**
     * Parses the following rules
     * (more rules are defined in {@link AbstractFloatValueParser}):
     * <dl>
     * <dt><i>RestOfHexFloatingPointLiteral</i>:
     * <dd><i>RestOfHexSignificand BinaryExponent</i>
     * </dl>
     *
     * <dl>
     * <dt><i>RestOfHexSignificand:</i>
     * <dd><i>HexDigits</i>
     * <dd><i>HexDigits</i> {@code .}
     * <dd><i>[HexDigits]</i> {@code .} <i>HexDigits</i>
     * </dl>
     *
     * @param str        the input string
     * @param index      index to the first character of RestOfHexFloatingPointLiteral
     * @param startIndex the start index of the string
     * @param endIndex   the end index of the string
     * @param isNegative if the resulting number is negative
     * @return a double representation
     */
    private long parseHexFloatingPointLiteral(
            CharSequence str, int index, int startIndex, int endIndex, boolean isNegative) {

        // Parse HexSignificand
        // ------------
        long significand = 0;// digits is treated as an unsigned long
        int exponent = 0;
        final int significandStartIndex = index;
        int virtualIndexOfPoint = -1;
        final int digitCount;
        boolean illegal = false;
        char ch = 0;
        for (; index < endIndex; index++) {
            ch = str.charAt(index);
            // Table look up is faster than a sequence of if-else-branches.
            int hexValue = ch > 127 ? AbstractFloatValueParser.OTHER_CLASS : AbstractFloatValueParser.CHAR_TO_HEX_MAP[ch];
            if (hexValue >= 0) {
                significand = (significand << 4) | hexValue;// This might overflow, we deal with it later.
            } else if (hexValue == AbstractFloatValueParser.DECIMAL_POINT_CLASS) {
                illegal |= virtualIndexOfPoint >= 0;
                virtualIndexOfPoint = index;
                /*
                for (;index < endIndex - 8; index += 8) {
                    long parsed = tryToParseEightHexDigits(str, index + 1);
                    if (parsed >= 0) {
                        // This might overflow, we deal with it later.
                        digits = (digits << 32) + parsed;
                    } else {
                        break;
                    }
                }
                */
            } else {
                break;
            }
        }
        final int significandEndIndex = index;
        if (virtualIndexOfPoint < 0) {
            digitCount = significandEndIndex - significandStartIndex;
            virtualIndexOfPoint = significandEndIndex;
        } else {
            digitCount = significandEndIndex - significandStartIndex - 1;
            exponent = Math.min(virtualIndexOfPoint - index + 1, AbstractFloatValueParser.MAX_EXPONENT_NUMBER) * 4;
        }

        // Parse exponent
        // --------------
        int expNumber = 0;
        final boolean hasExponent = (ch == 'p') || (ch == 'P');
        if (hasExponent) {
            ch = ++index < endIndex ? str.charAt(index) : 0;
            boolean neg_exp = ch == '-';
            if (neg_exp || ch == '+') {
                ch = ++index < endIndex ? str.charAt(index) : 0;
            }
            illegal |= !isDigit(ch);
            do {
                // Guard against overflow
                if (expNumber < AbstractFloatValueParser.MAX_EXPONENT_NUMBER) {
                    expNumber = 10 * expNumber + ch - '0';
                }
                ch = ++index < endIndex ? str.charAt(index) : 0;
            } while (isDigit(ch));
            if (neg_exp) {
                expNumber = -expNumber;
            }
            exponent += expNumber;
        }

        // Skip trailing whitespace and check if FloatValue is complete
        // ------------------------
        index = skipWhitespace(str, index, endIndex);
        if (illegal || index < endIndex
                || digitCount == 0 && str.charAt(virtualIndexOfPoint) != '.'
                || !hasExponent) {
            throw newNumberFormatException(str, startIndex, endIndex);
        }

        // Re-parse significand in case of a potential overflow
        // -----------------------------------------------
        final boolean isSignificandTruncated;
        int skipCountInTruncatedDigits = 0;//counts +1 if we skipped over the decimal point
        if (digitCount > 16) {
            significand = 0;
            for (index = significandStartIndex; index < significandEndIndex; index++) {
                ch = str.charAt(index);
                // Table look up is faster than a sequence of if-else-branches.
                int hexValue = ch > 127 ? AbstractFloatValueParser.OTHER_CLASS : AbstractFloatValueParser.CHAR_TO_HEX_MAP[ch];
                if (hexValue >= 0) {
                    if (Long.compareUnsigned(significand, AbstractFloatValueParser.MINIMAL_NINETEEN_DIGIT_INTEGER) < 0) {
                        significand = (significand << 4) | hexValue;
                    } else {
                        break;
                    }
                } else {
                    skipCountInTruncatedDigits++;
                }
            }
            isSignificandTruncated = (index < significandEndIndex);
        } else {
            isSignificandTruncated = false;
        }

        return valueOfHexLiteral(str, startIndex, endIndex, isNegative, significand, exponent, isSignificandTruncated,
                virtualIndexOfPoint - index + skipCountInTruncatedDigits + expNumber);
    }

    /**
     * Parses a {@code Infinity} production with optional trailing white space
     * until the end of the text.
     * <blockquote>
     * <dl>
     * <dt><i>InfinityWithWhiteSpace:</i></dt>
     * <dd>{@code Infinity} <i>[WhiteSpace] EOT</i></dd>
     * </dl>
     * </blockquote>
     *
     * @param str      a string
     * @param index    index of the "I" character
     * @param endIndex end index (exclusive)
     * @return a positive or negative infinity value
     * @throws NumberFormatException on parsing failure
     */
    private long parseInfinity(CharSequence str, int index, int endIndex, boolean negative) {
        if (index + 7 < endIndex
                && str.charAt(index) == 'I'
                && str.charAt(index + 1) == 'n'
                && str.charAt(index + 2) == 'f'
                && str.charAt(index + 3) == 'i'
                && str.charAt(index + 4) == 'n'
                && str.charAt(index + 5) == 'i'
                && str.charAt(index + 6) == 't'
                && str.charAt(index + 7) == 'y'
        ) {
            index = skipWhitespace(str, index + 8, endIndex);
            if (index == endIndex) {
                return negative ? negativeInfinity() : positiveInfinity();
            }
        }
        throw newNumberFormatException(str, index, endIndex);
    }

    /**
     * Parses a {@code Nan} production with optional trailing white space
     * until the end of the text.
     * Given that the String contains a 'N' character at the current
     * {@code index}.
     * <blockquote>
     * <dl>
     * <dt><i>NanWithWhiteSpace:</i></dt>
     * <dd>{@code NaN} <i>[WhiteSpace] EOT</i></dd>
     * </dl>
     * </blockquote>
     *
     * @param str      a string that contains a "N" character at {@code index}
     * @param index    index of the "N" character
     * @param endIndex end index (exclusive)
     * @return a NaN value
     * @throws NumberFormatException on parsing failure
     */
    private long parseNaN(CharSequence str, int index, int endIndex) {
        if (index + 2 < endIndex
                // && str.charAt(index) == 'N'
                && str.charAt(index + 1) == 'a'
                && str.charAt(index + 2) == 'N') {

            index = skipWhitespace(str, index + 3, endIndex);
            if (index == endIndex) {
                return nan();
            }
        }
        throw newNumberFormatException(str, index, endIndex);
    }

    abstract long positiveInfinity();

    /**
     * Skips optional white space in the provided string
     *
     * @param str      a string
     * @param index    start index (inclusive) of the optional white space
     * @param endIndex end index (exclusive) of the optional white space
     * @return index after the optional white space
     */
    private int skipWhitespace(CharSequence str, int index, int endIndex) {
        for (; index < endIndex; index++) {
            if (str.charAt(index) > ' ') {
                break;
            }
        }
        return index;
    }

    abstract long valueOfFloatLiteral(
            CharSequence str, int startIndex, int endIndex,
            boolean isNegative, long significand, int exponent,
            boolean isSignificandTruncated, int exponentOfTruncatedSignificand);

    abstract long valueOfHexLiteral(
            CharSequence str, int startIndex, int endIndex,
            boolean isNegative, long significand, int exponent,
            boolean isSignificandTruncated, int exponentOfTruncatedSignificand);

    private int tryToParseEightDigits(CharSequence str, int offset) {
        // Performance: We extract the chars in two steps so that we
        //              can benefit from out of order execution in the CPU.
        long first = str.charAt(offset)
                | (long) str.charAt(offset + 1) << 16
                | (long) str.charAt(offset + 2) << 32
                | (long) str.charAt(offset + 3) << 48;

        long second = str.charAt(offset + 4)
                | (long) str.charAt(offset + 5) << 16
                | (long) str.charAt(offset + 6) << 32
                | (long) str.charAt(offset + 7) << 48;

        return FastDoubleSwar.tryToParseEightDigitsUtf16(first, second);
    }
}
