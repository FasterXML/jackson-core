package com.fasterxml.jackson.core.io;

import java.math.BigDecimal;
import java.math.BigInteger;
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
    private final static int MAX_CHARS_TO_REPORT = 1000;

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

    public static BigDecimal xparse(final String str) {
        return xparse(str.toCharArray(), 0, str.length());
    }

    public static BigDecimal xparse(final char[] buf, int offset, int len) {
        final boolean isNeg = buf[offset] == '-';
        final int limit = offset + len;
        if (isNeg) {
            offset++;
        }
        int pos = offset;
        int intLen = 0;
        int fracLen = 0;
        int fracPos = 0;
        int ePos = 0;
        int scale = 0;
        while (pos < limit) {
            char c = buf[pos];
            if (c == '.') {
                fracPos = pos;
                c = buf[++pos];
                while (c >= '0' && c <= '9' && pos < limit) {
                    fracLen++;
                    c = buf[pos++];
                }
            } else if ((c | 0x20) == 'e') {
                ePos = pos;
                c = buf[++pos];
                final boolean isNegExp = c == '-';
                if (isNegExp || c == '+') {
                    c = buf[++pos];
                }
                if (c < '0' || c > '9') {
                    throw new NumberFormatException("unexpected character '" + c + "' at position " + pos);
                }
                long exp = c - '0';
                pos++;
                while (pos < limit) {
                    c = buf[pos++];
                    if (c < '0' || c > '9') {
                        throw new NumberFormatException("unexpected character '" + c + "' at position " + pos);
                    }
                    exp = exp * 10 + (c - '0');
                }
                if (exp > 2147483648L) throw new NumberFormatException("invalid exponent " + exp);
                else if (isNegExp) scale = Math.toIntExact(exp);
                else if (exp == 2147483648L) throw new NumberFormatException("invalid exponent " + -exp);
                else scale = Math.toIntExact(-exp);
            } else if (c < '0' || c > '9') {
                throw new NumberFormatException("unexpected character '" + c + "' at position " + pos);
            } else {
                intLen++;
                pos++;
            }
        }
        if (fracLen != 0) {
            if ((intLen + fracLen) < 19) {
                int from = offset;
                long x = buf[from++] - '0';
                while (from < fracPos) {
                    x = x * 10 + (buf[from++] - '0');
                }
                from++;
                final int parseLimit = ePos > 0 ? ePos : limit;
                while (from < parseLimit) {
                    x = x * 10 + (buf[from++] - '0');
                }
                if (isNeg) x = -x;
                return BigDecimal.valueOf(x, scale + fracLen);
            } else {
                int intLimit = fracPos - offset;
                if (isNeg) intLimit++;
                final int parseLimit = ePos > 0 ? ePos : limit;
                return toBigDecimal(buf, offset, intLimit, isNeg, scale)
                        .add(toBigDecimal(buf, fracPos + 1, parseLimit, isNeg, scale + fracLen));
            }
        } else {
            final int parseLen = ePos > 0 ? (ePos - offset) : len;
            return toBigDecimal(buf, offset, parseLen, isNeg, scale);
        }
    }

    private static BigDecimal toBigDecimal(final char[] buf, final int p, final int limit,
                                           final boolean isNeg, final int scale) {
        final int len = limit - p;
        if (len < 19) {
            int pos = p;
            long x = buf[pos++] - '0';
            while (pos < limit) {
                x = x * 10 + (buf[pos++] - '0');
            }
            if (isNeg) x = -x;
            return BigDecimal.valueOf(x, scale);
        } else if (len <= 36) {
            return toBigDecimal36(buf, p, limit, isNeg, scale);
        } else if (len <= 308) {
            return toBigDecimal308(buf, p, limit, isNeg, scale);
        } else {
            final int mid = len >> 1;
            final int midPos = limit - mid;
            return toBigDecimal(buf, p, midPos, isNeg, scale - mid)
                    .add(toBigDecimal(buf, midPos, limit, isNeg, scale));
        }
    }

    private static BigDecimal toBigDecimal36(final char[] buf, final int p, final int limit,
                                             final boolean isNeg, final int scale) {
        final int firstBlockLimit = limit - 18;
        int pos = p;
        long x1 = buf[pos] - '0';
        pos++;
        while (pos < firstBlockLimit) {
            x1 = x1 * 10 + (buf[pos++] - '0');
        }
        long x2 =
                (buf[pos] * 10 + buf[pos + 1]) * 10000000000000000L +
                        ((buf[pos + 2] * 10 + buf[pos + 3]) * 1000000 +
                                (buf[pos + 4] * 10 + buf[pos + 5]) * 10000 +
                                (buf[pos + 6] * 10 + buf[pos + 7]) * 100 +
                                (buf[pos + 8] * 10 + buf[pos + 9])) * 100000000L +
                        ((buf[pos + 10] * 10 + buf[pos + 11]) * 1000000 +
                                (buf[pos + 12] * 10 + buf[pos + 13]) * 10000 +
                                (buf[pos + 14] * 10 + buf[pos + 15]) * 100 +
                                buf[pos + 16] * 10 + buf[pos + 17]) - 5333333333333333328L; // 5333333333333333328L == '0' * 111111111111111111L
        if (isNeg) {
            x1 = -x1;
            x2 = -x2;
        }
        return BigDecimal.valueOf(x1, scale - 18).add(BigDecimal.valueOf(x2, scale));
    }

    private static BigDecimal toBigDecimal308(final char[] buf, final int p, final int limit,
                                              final boolean isNeg, final int scale) {
        final int len = limit - p;
        final int last = Math.toIntExact(len * 445861642L >> 32); // (len * Math.log(10) / Math.log(1L << 32))
        final int[] magnitude = new int[32]; //TODO possibly, find a way so that these arrays can be reused

        long x = 0L;
        final int firstBlockLimit = len % 9 + p;
        int pos = p;
        while (pos < firstBlockLimit) {
            x = x * 10 + (buf[pos++] - '0');
        }
        magnitude[last] = Math.toIntExact(x);
        int first = last;
        while (pos < limit) {
            x =
                (buf[pos] * 10 + buf[pos + 1]) * 10000000L +
                        ((buf[pos + 2] * 10 + buf[pos + 3]) * 100000 +
                                (buf[pos + 4] * 10 + buf[pos + 5]) * 1000 +
                                (buf[pos + 6] * 10 + buf[pos + 7]) * 10 +
                                buf[pos + 8]) - 5333333328L; // 5333333328L == '0' * 111111111L
            pos += 9;
            first = Math.max(first - 1, 0);
            int i = last;
            while (i >= first) {
                x += (magnitude[i] & 0xFFFFFFFFL) * 1000000000;
                magnitude[i] = Math.toIntExact(x);
                x >>>= 32;
                i--;
            }
        }
        final byte[] bs = new byte[last + 1 << 2];
        int i = 0;
        while (i <= last) {
            int w = magnitude[i];
            int j = i << 2;
            bs[j] = (byte) (w >> 24);
            bs[j + 1] = (byte) (w >> 16);
            bs[j + 2] = (byte) (w >> 8);
            bs[j + 3] = (byte) w;
            i++;
        }
        return new BigDecimal(new BigInteger(isNeg ? -1 : 1, bs), scale);
    }
}
