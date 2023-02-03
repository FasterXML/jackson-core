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
            return parseBigDecimal(chars, off, len, len / 10);

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
            final int reportLen = Math.min(len, MAX_CHARS_TO_REPORT);
            throw new NumberFormatException("Value \"" + new String(ch, off, reportLen)
                    + "\" can not be represented as `java.math.BigDecimal`, reason: " + nfe.getMessage());
        }
    }

    private static BigDecimal parseBigDecimal(final char[] chars, final int off, final int len, final int splitLen) {
        boolean numHasSign = false;
        boolean expHasSign = false;
        boolean neg = false;
        int numIdx = off;
        int expIdx = -1;
        int dotIdx = -1;
        int scale = 0;
        final int endIdx = off + len;

        for (int i = off; i < endIdx; i++) {
            char c = chars[i];
            switch (c) {
            case '+':
                if (expIdx >= 0) {
                    if (expHasSign) {
                        throw new NumberFormatException("Multiple signs in exponent");
                    }
                    expHasSign = true;
                } else {
                    if (numHasSign) {
                        throw new NumberFormatException("Multiple signs in number");
                    }
                    numHasSign = true;
                    numIdx = i + 1;
                }
                break;
            case '-':
                if (expIdx >= 0) {
                    if (expHasSign) {
                        throw new NumberFormatException("Multiple signs in exponent");
                    }
                    expHasSign = true;
                } else {
                    if (numHasSign) {
                        throw new NumberFormatException("Multiple signs in number");
                    }
                    numHasSign = true;
                    neg = true;
                    numIdx = i + 1;
                }
                break;
            case 'e':
            case 'E':
                if (expIdx >= 0) {
                    throw new NumberFormatException("Multiple exponent markers");
                }
                expIdx = i;
                break;
            case '.':
                if (dotIdx >= 0) {
                    throw new NumberFormatException("Multiple decimal points");
                }
                dotIdx = i;
                break;
            default:
                if (dotIdx >= 0 && expIdx == -1) {
                    scale++;
                }
            }
        }

        int numEndIdx;
        int exp = 0;
        if (expIdx >= 0) {
            numEndIdx = expIdx;
            String expStr = new String(chars, expIdx + 1, endIdx - expIdx - 1);
            exp = Integer.parseInt(expStr);
            scale = adjustScale(scale, exp);
        } else {
            numEndIdx = endIdx;
        }

        BigDecimal res;

        if (dotIdx >= 0) {
            int leftLen = dotIdx - numIdx;
            BigDecimal left = toBigDecimalRec(chars, numIdx, leftLen, exp, splitLen);

            int rightLen = numEndIdx - dotIdx - 1;
            BigDecimal right = toBigDecimalRec(chars, dotIdx + 1, rightLen, exp - rightLen, splitLen);

            res = left.add(right);
        } else {
            res = toBigDecimalRec(chars, numIdx, numEndIdx - numIdx, exp, splitLen);
        }

        if (scale != 0) {
            res = res.setScale(scale);
        }

        if (neg) {
            res = res.negate();
        }

        return res;
    }

    private static int adjustScale(int scale, long exp) {
        long adjScale = scale - exp;
        if (adjScale > Integer.MAX_VALUE || adjScale < Integer.MIN_VALUE) {
            throw new NumberFormatException(
                    "Scale out of range: " + adjScale + " while adjusting scale " + scale + " to exponent " + exp);
        }

        return (int) adjScale;
    }

    private static BigDecimal toBigDecimalRec(final char[] chars, final int off, final int len,
                                              final int scale, final int splitLen) {
        if (len > splitLen) {
            int mid = len / 2;
            BigDecimal left = toBigDecimalRec(chars, off, mid, scale + len - mid, splitLen);
            BigDecimal right = toBigDecimalRec(chars, off + mid, len - mid, scale, splitLen);

            return left.add(right);
        }

        return len == 0 ? BigDecimal.ZERO : new BigDecimal(chars, off, len).movePointRight(scale);
    }
}
