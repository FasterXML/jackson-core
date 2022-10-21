package com.fasterxml.jackson.core.io;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Helper class used to implement more optimized parsing of {@link BigDecimal}.
 *<p>
 * This is basically a Java port of the BigDecimal parser code in
 * <a href ="https://github.com/plokhotnyuk/jsoniter-scala/blob/master/jsoniter-scala-core/jvm/src/main/scala/com/github/plokhotnyuk/jsoniter_scala/core/JsonReader.scala">plokhotnyuk/jsoniter_scala</a>.
 * jsoniter-scala code was partly inspired by <a href="https://github.com/eobermuhlner/big-math/commit/7a5419aac8b2adba2aa700ccf00197f97b2ad89f">eobermuhlner/big-math</a>.
 *
 * @since 2.15
 */
public final class FastBigDecimalParser
{
    private final static int MAX_CHARS_TO_REPORT = 1000;

    private FastBigDecimalParser() {}

    public static BigDecimal parse(final String str) {
        return parse(str.toCharArray(), 0, str.length());
    }

    public static BigDecimal parse(final char[] buf, int offset, int len) {
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
                while (c >= '0' && c <= '9') {
                    fracLen++;
                    pos++;
                    if (pos >= limit) {
                        break;
                    }
                    c = buf[pos];
                }
            }
            if ((c | 0x20) == 'e') {
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
        final int[] magnitude = new int[32];
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
