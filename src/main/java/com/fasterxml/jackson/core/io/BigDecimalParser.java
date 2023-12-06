package com.fasterxml.jackson.core.io;

import ch.randelshofer.fastdoubleparser.JavaBigDecimalParser;

import java.math.BigDecimal;
import java.util.Arrays;

// Based on a great idea of Eric Obermühlner to use a tree of smaller BigDecimals for parsing
// really big numbers with O(n^1.5) complexity instead of O(n^2) when using the constructor
// for a decimal representation from JDK 8/11:
//
// https://github.com/eobermuhlner/big-math/commit/7a5419aac8b2adba2aa700ccf00197f97b2ad89f

/**
 * Helper class used to implement more optimized parsing of {@link BigDecimal} for REALLY
 * big values (over 500 characters)
 *<p>
 * Based on ideas from this
 * <a href="https://github.com/eobermuhlner/big-math/commit/7a5419aac8b2adba2aa700ccf00197f97b2ad89f">this
 * git commit</a>.
 *
 * @since 2.13
 */
public final class BigDecimalParser
{
    final static int MAX_CHARS_TO_REPORT = 1000;

    private BigDecimalParser() {}

    public static BigDecimal parse(String valueStr) {
        return parse(valueStr.toCharArray());
    }

    public static BigDecimal parse(final char[] chars, final int off, final int len) {
        try {
            if (len < 500) {
                return new BigDecimal(chars, off, len);
            }
            return JavaBigDecimalParser.parseBigDecimal(chars, off, len);

        // 20-Aug-2022, tatu: Although "new BigDecimal(...)" only throws NumberFormatException
        //    operations by "parseBigDecimal()" can throw "ArithmeticException", so handle both:
        } catch (ArithmeticException | NumberFormatException e) {
            String desc = e.getMessage();
            // 05-Feb-2021, tatu: Alas, JDK mostly has null message so:
            if (desc == null) {
                desc = "Not a valid number representation";
            }
            String stringToReport;
            if (len <= MAX_CHARS_TO_REPORT) {
                stringToReport = new String(chars, off, len);
            } else {
                stringToReport = new String(Arrays.copyOfRange(chars, off, MAX_CHARS_TO_REPORT))
                    + "(truncated, full length is " + chars.length + " chars)";
            }
            throw new NumberFormatException("Value \"" + stringToReport
                    + "\" can not be represented as `java.math.BigDecimal`, reason: " + desc);
        }
    }

    public static BigDecimal parse(char[] chars) {
        return parse(chars, 0, chars.length);
    }

    public static BigDecimal parseWithFastParser(final String valueStr) {
        try {
            return JavaBigDecimalParser.parseBigDecimal(valueStr);
        } catch (NumberFormatException nfe) {
            final String reportNum = valueStr.length() <= MAX_CHARS_TO_REPORT ?
                    valueStr : valueStr.substring(0, MAX_CHARS_TO_REPORT) + " [truncated]";
            throw new NumberFormatException("Value \"" + reportNum
                    + "\" can not be represented as `java.math.BigDecimal`, reason: " + nfe.getMessage());
        }
    }

    public static BigDecimal parseWithFastParser(final char[] ch, final int off, final int len) {
        try {
            return JavaBigDecimalParser.parseBigDecimal(ch, off, len);
        } catch (NumberFormatException nfe) {
            final String reportNum = len <= MAX_CHARS_TO_REPORT ?
                    new String(ch, off, len) : new String(ch, off, MAX_CHARS_TO_REPORT) + " [truncated]";
            throw new NumberFormatException("Value \"" + reportNum
                    + "\" can not be represented as `java.math.BigDecimal`, reason: " + nfe.getMessage());
        }
    }
}
