package tools.jackson.core.io.xjb;

import tools.jackson.core.io.NumberOutput;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.charset.StandardCharsets;

/**
 * Java port of the "xjb" float/double-to-decimal-string algorithm, as implemented
 * in jsoniter-scala's JsonWriter (JVM/JVM-native module):
 * https://github.com/plokhotnyuk/jsoniter-scala/blob/master/jsoniter-scala-core/jvm-native/src/main/scala/com/github/plokhotnyuk/jsoniter_scala/core/JsonWriter.scala
 *
 * Original algorithm: "xjb: Fast Float to String Algorithm" by Xiang JunBo and Wang TieJun.
 * https://github.com/xjb714/xjb
 *
 * This is a faithful, mostly line-for-line transliteration of the Scala source into
 * plain Java (no external dependencies), preserving all bit tricks and lookup tables.
 * Output matches {@code Float.toString}/{@code Double.toString} formatting conventions
 * (shortest round-tripping decimal, same switch to scientific notation thresholds as
 * used by jsoniter-scala, i.e. decimal exponent &lt; -3 or &gt;= 7 for float/double).
 *
 * @since 3.3
 */
public final class XJBWriter {

    private XJBWriter() {
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    public static String toString(float x) {
        if (Float.isNaN(x)) return "NaN";
        if (Float.isInfinite(x)) return x < 0 ? "-Infinity" : "Infinity";
        byte[] buf = new byte[NumberOutput.MAX_FLOAT_CHARS];
        int pos = writeFloat(x, buf, 0);
        return new String(buf, 0, pos, StandardCharsets.ISO_8859_1);
    }

    public static String toString(double x) {
        if (Double.isNaN(x)) return "NaN";
        if (Double.isInfinite(x)) return x < 0 ? "-Infinity" : "Infinity";
        byte[] buf = new byte[NumberOutput.MAX_DOUBLE_CHARS];
        int pos = writeDouble(x, buf, 0);
        return new String(buf, 0, pos, StandardCharsets.ISO_8859_1);
    }

    /**
     * Writes the decimal string representation of {@code x} into {@code buf} starting at
     * {@code from}. The buffer must have at least
     * {@link tools.jackson.core.io.NumberOutput#MAX_FLOAT_CHARS} bytes of free space
     * from {@code from}.
     *
     * @return the position just after the last byte written
     */
    public static int writeFloat(float x, byte[] buf, int from) {
        int bits = Float.floatToRawIntBits(x);
        int pos = from;
        // Check for non-finite (NaN, Infinity) before sign — NaN ignores sign bit
        int e2IEEE = (bits >> 23) & 0xFF;
        if (e2IEEE == 0xFF) {
            if ((bits & 0x7FFFFF) != 0) {
                // NaN
                buf[pos] = 'N'; buf[pos + 1] = 'a'; buf[pos + 2] = 'N';
                return pos + 3;
            }
            // Infinity
            if (bits < 0) {
                buf[pos] = '-';
                pos++;
            }
            buf[pos]     = 'I'; buf[pos + 1] = 'n'; buf[pos + 2] = 'f'; buf[pos + 3] = 'i';
            buf[pos + 4] = 'n'; buf[pos + 5] = 'i'; buf[pos + 6] = 't'; buf[pos + 7] = 'y';
            return pos + 8;
        }
        if (bits < 0) {
            buf[pos] = '-';
            pos += 1;
        }
        if (bits << 1 == 0) {
            setInt(buf, pos, 0x302E30);
            pos += 3;
        } else {
            int m2IEEE = bits & 0x7FFFFF;
            int e2 = e2IEEE - 150;
            int m2 = m2IEEE | 0x800000;
            int m10 = 0, e10 = 0;
            if (e2 == 0) {
                m10 = m2;
            } else if ((e2 >= -23 && e2 < 0) && (m2 << e2) == 0) {
                m10 = m2 >> -e2;
            } else {
                if (e2IEEE == 0) {
                    m2 = m2IEEE;
                    e2 = -149;
                }
                if (m2IEEE == 0) e10 = (e2 * 315653 - 131237) >> 20;
                else e10 = (e2 * 315653) >> 20;
                int h = (((e10 + 1) * -217707) >> 16) + e2;
                long pow10 = FLOAT_POW10S[31 - e10];
                long halfUlpPlusEven = (pow10 >>> (28 - h)) + ((m2IEEE + 1) & 1);
                long hi64 = unsignedMultiplyHigh1(pow10, ((long) m2) << (h + 37));
                long dotOne = hi64 & 0xFFFFFFFFFL;
                m10 = (int) (hi64 >>> 36) * 10;
                long cmp = (m2IEEE == 0) ? (halfUlpPlusEven >>> 1) : halfUlpPlusEven;
                if (Long.compareUnsigned(cmp, dotOne) <= 0) {
                    if (Long.compareUnsigned(halfUlpPlusEven, 0xFFFFFFFFFL - dotOne) > 0) {
                        m10 += 10;
                    } else {
                        m10 += (int) ((dotOne * 20 + ((int) (hi64 >>> 32) & 0xF) + 0xFFFFFFFF9L) >>> 37);
                    }
                }
                if (m2IEEE == 0 && ((e2 == -119) || (e2 == 64) || (e2 == 67))) m10 += 1;
            }
            int len = digitCount(m10);
            e10 += len - 1;
            short[] ds = DIGITS;
            if (e10 < -3 || e10 >= 7) {
                int lastPos = writeSignificantFractionDigits(m10, pos + len, pos, buf, ds);
                setShort(buf, pos, (short) (buf[pos + 1] | 0x2E00));
                if (lastPos - 3 < pos) {
                    buf[lastPos] = '0';
                    pos = lastPos + 1;
                } else {
                    pos = lastPos;
                }
                setShort(buf, pos, (short) 0x2D45);
                pos += 1;
                if (e10 < 0) {
                    e10 = -e10;
                    pos += 1;
                }
                if (e10 < 10) {
                    buf[pos] = (byte) (e10 | '0');
                    pos += 1;
                } else {
                    setShort(buf, pos, ds[e10]);
                    pos += 2;
                }
            } else if (e10 < 0) {
                int dotPos = pos + 1;
                setInt(buf, pos, 0x30303030);
                pos -= e10;
                pos = writeSignificantFractionDigits(m10, pos + len, pos, buf, ds);
                buf[dotPos] = '.';
            } else if (e10 < len - 1) {
                int lastPos = writeSignificantFractionDigits(m10, pos + len, pos, buf, ds);
                long bs = getLong(buf, pos);
                int s = e10 << 3;
                long m = 0xFFFFFFFFFFFF0000L << s;
                long d1 = (~m & bs) >> 8;
                long d2 = 0x2E00L << s;
                long d3 = m & bs;
                setLong(buf, pos, d1 | d2 | d3);
                pos = lastPos;
            } else {
                pos += len;
                setShort(buf, pos, (short) 0x302E);
                int lastPos = pos;
                while (true) {
                    pos -= 2;
                    if (m10 < 100) break;
                    int q1 = (int) ((m10 * 1374389535L) >> 37); // divide a positive int by 100
                    setShort(buf, pos, ds[m10 - q1 * 100]);
                    m10 = q1;
                }
                if (m10 < 10) buf[pos + 1] = (byte) (m10 | '0');
                else setShort(buf, pos, ds[m10]);
                pos = lastPos + 2;
            }
        }
        return pos;
    }

    /**
     * Writes the decimal string representation of {@code x} into {@code buf} starting at
     * {@code from}. The buffer must have at least
     * {@link tools.jackson.core.io.NumberOutput#MAX_DOUBLE_CHARS} bytes of free space
     * from {@code from}.
     *
     * @return the position just after the last byte written
     */
    public static int writeDouble(double x, byte[] buf, int from) {
        long bits = Double.doubleToRawLongBits(x);
        int pos = from;
        // Check for non-finite (NaN, Infinity) before sign — NaN ignores sign bit
        int e2IEEE = (int) (bits >> 52) & 0x7FF;
        if (e2IEEE == 0x7FF) {
            if ((bits & 0xFFFFFFFFFFFFFL) != 0) {
                // NaN
                buf[pos] = 'N'; buf[pos + 1] = 'a'; buf[pos + 2] = 'N';
                return pos + 3;
            }
            // Infinity
            if (bits < 0L) {
                buf[pos] = '-';
                pos++;
            }
            buf[pos]     = 'I'; buf[pos + 1] = 'n'; buf[pos + 2] = 'f'; buf[pos + 3] = 'i';
            buf[pos + 4] = 'n'; buf[pos + 5] = 'i'; buf[pos + 6] = 't'; buf[pos + 7] = 'y';
            return pos + 8;
        }
        if (bits < 0L) {
            buf[pos] = '-';
            pos += 1;
        }
        if (bits << 1 == 0L) {
            setInt(buf, pos, 0x302E30);
            pos += 3;
        } else {
            long m2IEEE = bits & 0xFFFFFFFFFFFFFL;
            int e2 = e2IEEE - 1075;
            long m2 = m2IEEE | 0x10000000000000L;
            long m10 = 0L;
            int e10 = 0;
            if (e2 == 0) {
                m10 = m2;
            } else if ((e2 >= -52 && e2 < 0) && (m2 << e2) == 0) {
                m10 = m2 >> -e2;
            } else {
                if (e2IEEE == 0) {
                    m2 = m2IEEE;
                    e2 = -1074;
                }
                if (m2IEEE == 0) e10 = (e2 * 315653 - 131237) >> 20;
                else e10 = (e2 * 315653) >> 20;
                int h = (((e10 + 1) * -217707) >> 16) + e2;
                int i = (292 - e10) << 1;
                long pow10_1 = DOUBLE_POW10S[i];
                long pow10_2 = DOUBLE_POW10S[i + 1];
                long halfUlpPlusEven = (pow10_1 >>> -h) + (((int) m2 + 1) & 1);
                long cb = m2 << (h + 7);
                long lo64_1 = unsignedMultiplyHigh2(pow10_2, cb);
                long lo64_2 = pow10_1 * cb;
                long hi64 = unsignedMultiplyHigh1(pow10_1, cb);
                long lo64 = lo64_1 + lo64_2;
                hi64 += Long.compareUnsigned(lo64, lo64_1) >>> 31;
                long dotOne = (hi64 << 58) | (lo64 >>> 6);
                int mCorr = 0;
                boolean roundUp;
                if (Long.compareUnsigned(-1L - dotOne, halfUlpPlusEven) < 0) {
                    mCorr = 10;
                    roundUp = false;
                } else if (m2IEEE != 0) {
                    roundUp = Long.compareUnsigned(halfUlpPlusEven, dotOne) <= 0;
                } else {
                    long tmp = (dotOne >>> 4) * 10L;
                    if (Long.compareUnsigned(tmp & 0x0FFFFFFFFFFFFFFFL, (halfUlpPlusEven >>> 4) * 5L) > 0) {
                        mCorr = (int) (tmp >>> 60) + 1;
                        roundUp = false;
                    } else {
                        roundUp = Long.compareUnsigned(halfUlpPlusEven >>> 1, dotOne) <= 0;
                    }
                }
                if (roundUp) {
                    m10 = (hi64 * 10L + unsignedMultiplyHigh2(lo64, 10L)
                            + (dotOne == 0x4000000000000000L ? 0x1FL : 0x20L)) >>> 6;
                } else {
                    m10 = (hi64 >>> 6) * 10L + mCorr;
                }
            }
            int len = digitCount(m10);
            e10 += len - 1;
            short[] ds = DIGITS;
            if (e10 < -3 || e10 >= 7) {
                int lastPos = writeSignificantFractionDigits(m10, pos + len, pos, buf, ds);
                setShort(buf, pos, (short) (buf[pos + 1] | 0x2E00));
                if (lastPos - 3 < pos) {
                    buf[lastPos] = '0';
                    pos = lastPos + 1;
                } else {
                    pos = lastPos;
                }
                setShort(buf, pos, (short) 0x2D45);
                pos += 1;
                if (e10 < 0) {
                    e10 = -e10;
                    pos += 1;
                }
                if (e10 < 10) {
                    buf[pos] = (byte) (e10 | '0');
                    pos += 1;
                } else if (e10 < 100) {
                    setShort(buf, pos, ds[e10]);
                    pos += 2;
                } else {
                    pos = write3Digits(e10, pos, buf, ds);
                }
            } else if (e10 < 0) {
                int dotPos = pos + 1;
                setInt(buf, pos, 0x30303030);
                pos -= e10;
                pos = writeSignificantFractionDigits(m10, pos + len, pos, buf, ds);
                buf[dotPos] = '.';
            } else if (e10 < len - 1) {
                int lastPos = writeSignificantFractionDigits(m10, pos + len, pos, buf, ds);
                long bs = getLong(buf, pos);
                int s = e10 << 3;
                long m = 0xFFFFFFFFFFFF0000L << s;
                long d1 = (~m & bs) >> 8;
                long d2 = 0x2E00L << s;
                long d3 = m & bs;
                setLong(buf, pos, d1 | d2 | d3);
                pos = lastPos;
            } else {
                pos += len;
                setShort(buf, pos, (short) 0x302E);
                int q0 = (int) m10;
                int lastPos = pos;
                while (true) {
                    pos -= 2;
                    if (q0 < 100) break;
                    int q1 = (int) ((q0 * 1374389535L) >> 37); // divide a positive int by 100
                    setShort(buf, pos, ds[q0 - q1 * 100]);
                    q0 = q1;
                }
                if (q0 < 10) buf[pos + 1] = (byte) (q0 | '0');
                else setShort(buf, pos, ds[q0]);
                pos = lastPos + 2;
            }
        }
        return pos;
    }

    /**
     * Writes the decimal string representation of {@code x} into {@code buf} starting at
     * {@code from}. The buffer must have at least
     * {@link tools.jackson.core.io.NumberOutput#MAX_FLOAT_CHARS} chars of free space
     * from {@code from}.
     *
     * @return the position just after the last char written
     *
     * @since 3.3
     */
    public static int writeFloat(float x, char[] buf, int from) {
        int bits = Float.floatToRawIntBits(x);
        int pos = from;
        // Check for non-finite (NaN, Infinity) before sign — NaN ignores sign bit
        int e2IEEE = (bits >> 23) & 0xFF;
        if (e2IEEE == 0xFF) {
            if ((bits & 0x7FFFFF) != 0) {
                // NaN
                buf[pos] = 'N'; buf[pos + 1] = 'a'; buf[pos + 2] = 'N';
                return pos + 3;
            }
            // Infinity
            if (bits < 0) {
                buf[pos] = '-';
                pos++;
            }
            buf[pos]     = 'I'; buf[pos + 1] = 'n'; buf[pos + 2] = 'f'; buf[pos + 3] = 'i';
            buf[pos + 4] = 'n'; buf[pos + 5] = 'i'; buf[pos + 6] = 't'; buf[pos + 7] = 'y';
            return pos + 8;
        }
        if (bits < 0) {
            buf[pos] = '-';
            pos += 1;
        }
        if (bits << 1 == 0) {
            buf[pos] = '0'; buf[pos + 1] = '.'; buf[pos + 2] = '0';
            pos += 3;
        } else {
            int m2IEEE = bits & 0x7FFFFF;
            int e2 = e2IEEE - 150;
            int m2 = m2IEEE | 0x800000;
            int m10 = 0, e10 = 0;
            if (e2 == 0) {
                m10 = m2;
            } else if ((e2 >= -23 && e2 < 0) && (m2 << e2) == 0) {
                m10 = m2 >> -e2;
            } else {
                if (e2IEEE == 0) {
                    m2 = m2IEEE;
                    e2 = -149;
                }
                if (m2IEEE == 0) e10 = (e2 * 315653 - 131237) >> 20;
                else e10 = (e2 * 315653) >> 20;
                int h = (((e10 + 1) * -217707) >> 16) + e2;
                long pow10 = FLOAT_POW10S[31 - e10];
                long halfUlpPlusEven = (pow10 >>> (28 - h)) + ((m2IEEE + 1) & 1);
                long hi64 = unsignedMultiplyHigh1(pow10, ((long) m2) << (h + 37));
                long dotOne = hi64 & 0xFFFFFFFFFL;
                m10 = (int) (hi64 >>> 36) * 10;
                long cmp = (m2IEEE == 0) ? (halfUlpPlusEven >>> 1) : halfUlpPlusEven;
                if (Long.compareUnsigned(cmp, dotOne) <= 0) {
                    if (Long.compareUnsigned(halfUlpPlusEven, 0xFFFFFFFFFL - dotOne) > 0) {
                        m10 += 10;
                    } else {
                        m10 += (int) ((dotOne * 20 + ((int) (hi64 >>> 32) & 0xF) + 0xFFFFFFFF9L) >>> 37);
                    }
                }
                if (m2IEEE == 0 && ((e2 == -119) || (e2 == 64) || (e2 == 67))) m10 += 1;
            }
            int len = digitCount(m10);
            e10 += len - 1;
            short[] ds = DIGITS;
            if (e10 < -3 || e10 >= 7) {
                int lastPos = writeSignificantFractionDigitsChar(m10, pos + len, pos, buf, ds);
                // Equivalent of setShort(buf, pos, (short)(buf[pos + 1] | 0x2E00)):
                // move second digit to first position, insert '.' after it
                buf[pos] = buf[pos + 1];
                buf[pos + 1] = '.';
                if (lastPos - 3 < pos) {
                    buf[lastPos] = '0';
                    pos = lastPos + 1;
                } else {
                    pos = lastPos;
                }
                buf[pos] = 'E';
                buf[pos + 1] = '-';
                pos += 1;
                if (e10 < 0) {
                    e10 = -e10;
                    pos += 1;
                }
                if (e10 < 10) {
                    buf[pos] = (char) (e10 + '0');
                    pos += 1;
                } else {
                    short d = ds[e10];
                    buf[pos] = (char) (d & 0xFF);
                    buf[pos + 1] = (char) ((d >> 8) & 0xFF);
                    pos += 2;
                }
            } else if (e10 < 0) {
                int dotPos = pos + 1;
                buf[pos] = '0'; buf[pos + 1] = '0'; buf[pos + 2] = '0'; buf[pos + 3] = '0';
                pos -= e10;
                pos = writeSignificantFractionDigitsChar(m10, pos + len, pos, buf, ds);
                buf[dotPos] = '.';
            } else if (e10 < len - 1) {
                int lastPos = writeSignificantFractionDigitsChar(m10, pos + len, pos, buf, ds);
                // The pair-based digit writer may have written a padding char at pos.
                // Shift the integer-part digits left by 1 to remove it, then insert '.'
                System.arraycopy(buf, pos + 1, buf, pos, e10 + 1);
                buf[pos + e10 + 1] = '.';
                pos = lastPos;
            } else {
                pos += len;
                buf[pos] = '.';
                buf[pos + 1] = '0';
                int lastPos = pos;
                pos -= 2;
                while (true) {
                    if (m10 < 100) break;
                    int q1 = (int) ((m10 * 1374389535L) >> 37);
                    short d = ds[m10 - q1 * 100];
                    buf[pos] = (char) (d & 0xFF);
                    buf[pos + 1] = (char) ((d >> 8) & 0xFF);
                    m10 = q1;
                    pos -= 2;
                }
                if (m10 < 10) buf[pos + 1] = (char) (m10 + '0');
                else {
                    short d = ds[m10];
                    buf[pos] = (char) (d & 0xFF);
                    buf[pos + 1] = (char) ((d >> 8) & 0xFF);
                }
                pos = lastPos + 2;
            }
        }
        return pos;
    }

    /**
     * Writes the decimal string representation of {@code x} into {@code buf} starting at
     * {@code from}. The buffer must have at least
     * {@link tools.jackson.core.io.NumberOutput#MAX_DOUBLE_CHARS} chars of free space
     * from {@code from}.
     *
     * @return the position just after the last char written
     *
     * @since 3.3
     */
    public static int writeDouble(double x, char[] buf, int from) {
        long bits = Double.doubleToRawLongBits(x);
        int pos = from;
        // Check for non-finite (NaN, Infinity) before sign — NaN ignores sign bit
        int e2IEEE = (int) (bits >> 52) & 0x7FF;
        if (e2IEEE == 0x7FF) {
            if ((bits & 0xFFFFFFFFFFFFFL) != 0) {
                // NaN
                buf[pos] = 'N'; buf[pos + 1] = 'a'; buf[pos + 2] = 'N';
                return pos + 3;
            }
            // Infinity
            if (bits < 0L) {
                buf[pos] = '-';
                pos++;
            }
            buf[pos]     = 'I'; buf[pos + 1] = 'n'; buf[pos + 2] = 'f'; buf[pos + 3] = 'i';
            buf[pos + 4] = 'n'; buf[pos + 5] = 'i'; buf[pos + 6] = 't'; buf[pos + 7] = 'y';
            return pos + 8;
        }
        if (bits < 0L) {
            buf[pos] = '-';
            pos += 1;
        }
        if (bits << 1 == 0L) {
            buf[pos] = '0'; buf[pos + 1] = '.'; buf[pos + 2] = '0';
            pos += 3;
        } else {
            long m2IEEE = bits & 0xFFFFFFFFFFFFFL;
            int e2 = e2IEEE - 1075;
            long m2 = m2IEEE | 0x10000000000000L;
            long m10 = 0L;
            int e10 = 0;
            if (e2 == 0) {
                m10 = m2;
            } else if ((e2 >= -52 && e2 < 0) && (m2 << e2) == 0) {
                m10 = m2 >> -e2;
            } else {
                if (e2IEEE == 0) {
                    m2 = m2IEEE;
                    e2 = -1074;
                }
                if (m2IEEE == 0) e10 = (e2 * 315653 - 131237) >> 20;
                else e10 = (e2 * 315653) >> 20;
                int h = (((e10 + 1) * -217707) >> 16) + e2;
                int i = (292 - e10) << 1;
                long pow10_1 = DOUBLE_POW10S[i];
                long pow10_2 = DOUBLE_POW10S[i + 1];
                long halfUlpPlusEven = (pow10_1 >>> -h) + (((int) m2 + 1) & 1);
                long cb = m2 << (h + 7);
                long lo64_1 = unsignedMultiplyHigh2(pow10_2, cb);
                long lo64_2 = pow10_1 * cb;
                long hi64 = unsignedMultiplyHigh1(pow10_1, cb);
                long lo64 = lo64_1 + lo64_2;
                hi64 += Long.compareUnsigned(lo64, lo64_1) >>> 31;
                long dotOne = (hi64 << 58) | (lo64 >>> 6);
                int mCorr = 0;
                boolean roundUp;
                if (Long.compareUnsigned(-1L - dotOne, halfUlpPlusEven) < 0) {
                    mCorr = 10;
                    roundUp = false;
                } else if (m2IEEE != 0) {
                    roundUp = Long.compareUnsigned(halfUlpPlusEven, dotOne) <= 0;
                } else {
                    long tmp = (dotOne >>> 4) * 10L;
                    if (Long.compareUnsigned(tmp & 0x0FFFFFFFFFFFFFFFL, (halfUlpPlusEven >>> 4) * 5L) > 0) {
                        mCorr = (int) (tmp >>> 60) + 1;
                        roundUp = false;
                    } else {
                        roundUp = Long.compareUnsigned(halfUlpPlusEven >>> 1, dotOne) <= 0;
                    }
                }
                if (roundUp) {
                    m10 = (hi64 * 10L + unsignedMultiplyHigh2(lo64, 10L)
                            + (dotOne == 0x4000000000000000L ? 0x1FL : 0x20L)) >>> 6;
                } else {
                    m10 = (hi64 >>> 6) * 10L + mCorr;
                }
            }
            int len = digitCount(m10);
            e10 += len - 1;
            short[] ds = DIGITS;
            if (e10 < -3 || e10 >= 7) {
                int lastPos = writeSignificantFractionDigitsLongChar(m10, pos + len, pos, buf, ds);
                // Equivalent of setShort(buf, pos, (short)(buf[pos + 1] | 0x2E00)):
                // move second digit to first position, insert '.' after it
                buf[pos] = buf[pos + 1];
                buf[pos + 1] = '.';
                if (lastPos - 3 < pos) {
                    buf[lastPos] = '0';
                    pos = lastPos + 1;
                } else {
                    pos = lastPos;
                }
                buf[pos] = 'E';
                buf[pos + 1] = '-';
                pos += 1;
                if (e10 < 0) {
                    e10 = -e10;
                    pos += 1;
                }
                if (e10 < 10) {
                    buf[pos] = (char) (e10 + '0');
                    pos += 1;
                } else if (e10 < 100) {
                    short d = ds[e10];
                    buf[pos] = (char) (d & 0xFF);
                    buf[pos + 1] = (char) ((d >> 8) & 0xFF);
                    pos += 2;
                } else {
                    pos = write3DigitsChar(e10, pos, buf, ds);
                }
            } else if (e10 < 0) {
                int dotPos = pos + 1;
                buf[pos] = '0'; buf[pos + 1] = '0'; buf[pos + 2] = '0'; buf[pos + 3] = '0';
                pos -= e10;
                pos = writeSignificantFractionDigitsLongChar(m10, pos + len, pos, buf, ds);
                buf[dotPos] = '.';
            } else if (e10 < len - 1) {
                int lastPos = writeSignificantFractionDigitsLongChar(m10, pos + len, pos, buf, ds);
                // The pair-based digit writer may have written a padding char at pos.
                // Shift the integer-part digits left by 1 to remove it, then insert '.'
                System.arraycopy(buf, pos + 1, buf, pos, e10 + 1);
                buf[pos + e10 + 1] = '.';
                pos = lastPos;
            } else {
                pos += len;
                buf[pos] = '.';
                buf[pos + 1] = '0';
                int q0 = (int) m10;
                int lastPos = pos;
                pos -= 2;
                while (true) {
                    if (q0 < 100) break;
                    int q1 = (int) ((q0 * 1374389535L) >> 37);
                    short d = ds[q0 - q1 * 100];
                    buf[pos] = (char) (d & 0xFF);
                    buf[pos + 1] = (char) ((d >> 8) & 0xFF);
                    q0 = q1;
                    pos -= 2;
                }
                if (q0 < 10) buf[pos + 1] = (char) (q0 + '0');
                else {
                    short d = ds[q0];
                    buf[pos] = (char) (d & 0xFF);
                    buf[pos + 1] = (char) ((d >> 8) & 0xFF);
                }
                pos = lastPos + 2;
            }
        }
        return pos;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static long unsignedMultiplyHigh1(long x, long y) {
        // Works only when x is negative and y is positive (matches call sites above).
        return Math.multiplyHigh(x, y) + y;
    }

    private static long unsignedMultiplyHigh2(long x, long y) {
        // Works only when y is positive (matches call sites above).
        return Math.multiplyHigh(x, y) + ((x >> 63) & y);
    }

    // Adoption of a trick from Daniel Lemire's blog that works for numbers up to 10^18:
    // https://lemire.me/blog/2021/06/03/computing-the-number-of-digits-of-an-integer-even-faster/
    private static int digitCount(long x) {
        return (int) ((OFFSETS[Long.numberOfLeadingZeros(x)] + x) >> 58);
    }

    private static int writeSignificantFractionDigits(long x, int p, int pl, byte[] buf, short[] ds) {
        int q0 = (int) x;
        int pos = p;
        int posLim = pl;
        if (q0 != x) {
            long q1 = Math.multiplyHigh(x, 6189700196426901375L) >>> 25; // divide a positive long by 100000000
            int r1 = (int) (x - q1 * 100000000L);
            int posm8 = pos - 8;
            if (r1 == 0) {
                q0 = (int) q1;
                pos = posm8;
            } else {
                writeFractionDigits((int) q1, posm8, posLim, buf, ds);
                q0 = r1;
                posLim = posm8;
            }
        }
        return writeSignificantFractionDigits(q0, pos, posLim, buf, ds);
    }

    private static int writeSignificantFractionDigits(int x, int p, int posLim, byte[] buf, short[] ds) {
        int q0 = x;
        int q1 = 0;
        int pos = p;
        while (true) {
            long qp = q0 * 1374389535L;
            q1 = (int) (qp >> 37); // divide a positive int by 100
            if ((qp & 0x1FC0000000L) != 0) break; // check if q is divisible by 100
            q0 = q1;
            pos -= 2;
        }
        short d = ds[q0 - q1 * 100];
        setShort(buf, pos - 1, d);
        writeFractionDigits(q1, pos - 2, posLim, buf, ds);
        return pos + ((0x3039 - d) >>> 31);
    }

    private static void writeFractionDigits(int x, int p, int posLim, byte[] buf, short[] ds) {
        int q0 = x;
        int pos = p;
        while (pos > posLim) {
            int q1 = (int) ((q0 * 1374389535L) >> 37); // divide a positive int by 100
            setShort(buf, pos - 1, ds[q0 - q1 * 100]);
            q0 = q1;
            pos -= 2;
        }
    }

    private static int write3Digits(int x, int pos, byte[] buf, short[] ds) {
        int q1 = (x * 1311) >> 17; // divide a small positive int by 100
        setInt(buf, pos, (ds[x - q1 * 100] << 8) | q1 | '0');
        return pos + 3;
    }

    // char[] variants of the digit-writing helpers

    private static int writeSignificantFractionDigitsLongChar(long x, int p, int pl, char[] buf, short[] ds) {
        int q0 = (int) x;
        int pos = p;
        int posLim = pl;
        if (q0 != x) {
            long q1 = Math.multiplyHigh(x, 6189700196426901375L) >>> 25; // divide a positive long by 100000000
            int r1 = (int) (x - q1 * 100000000L);
            int posm8 = pos - 8;
            if (r1 == 0) {
                q0 = (int) q1;
                pos = posm8;
            } else {
                writeFractionDigitsChar((int) q1, posm8, posLim, buf, ds);
                q0 = r1;
                posLim = posm8;
            }
        }
        return writeSignificantFractionDigitsChar(q0, pos, posLim, buf, ds);
    }

    private static int writeSignificantFractionDigitsChar(int x, int p, int posLim, char[] buf, short[] ds) {
        int q0 = x;
        int q1 = 0;
        int pos = p;
        while (true) {
            long qp = q0 * 1374389535L;
            q1 = (int) (qp >> 37); // divide a positive int by 100
            if ((qp & 0x1FC0000000L) != 0) break; // check if q is divisible by 100
            q0 = q1;
            pos -= 2;
        }
        short d = ds[q0 - q1 * 100];
        buf[pos - 1] = (char) (d & 0xFF);
        buf[pos] = (char) ((d >> 8) & 0xFF);
        writeFractionDigitsChar(q1, pos - 2, posLim, buf, ds);
        return pos + ((0x3039 - d) >>> 31);
    }

    private static void writeFractionDigitsChar(int x, int p, int posLim, char[] buf, short[] ds) {
        int q0 = x;
        int pos = p;
        while (pos > posLim) {
            int q1 = (int) ((q0 * 1374389535L) >> 37); // divide a positive int by 100
            short d = ds[q0 - q1 * 100];
            buf[pos - 1] = (char) (d & 0xFF);
            buf[pos] = (char) ((d >> 8) & 0xFF);
            q0 = q1;
            pos -= 2;
        }
    }

    private static int write3DigitsChar(int x, int pos, char[] buf, short[] ds) {
        int q1 = (x * 1311) >> 17; // divide a small positive int by 100
        short d = ds[x - q1 * 100];
        buf[pos] = (char) (q1 + '0');
        buf[pos + 1] = (char) (d & 0xFF);
        buf[pos + 2] = (char) ((d >> 8) & 0xFF);
        return pos + 3;
    }

    // ------------------------------------------------------------------
    // Little-endian byte array access — VarHandle via XJBVarHandleAccess on Java 9+,
    // manual fallback for Android (where XJBVarHandleAccess fails to load)
    // ------------------------------------------------------------------

    // MethodHandles bound to XJBVarHandleAccess static methods; null on Android
    private static final MethodHandle MH_SET_INT;
    private static final MethodHandle MH_SET_SHORT;
    private static final MethodHandle MH_SET_LONG;
    private static final MethodHandle MH_GET_LONG;

    static {
        MethodHandle setInt = null, setShort = null, setLong = null, getLong = null;
        try {
            Class<?> vhAccess = Class.forName("tools.jackson.core.io.xjb.XJBVarHandleAccess");
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            setInt = lookup.findStatic(vhAccess, "setInt",
                    MethodType.methodType(void.class, byte[].class, int.class, int.class));
            setShort = lookup.findStatic(vhAccess, "setShort",
                    MethodType.methodType(void.class, byte[].class, int.class, short.class));
            setLong = lookup.findStatic(vhAccess, "setLong",
                    MethodType.methodType(void.class, byte[].class, int.class, long.class));
            getLong = lookup.findStatic(vhAccess, "getLong",
                    MethodType.methodType(long.class, byte[].class, int.class));
        } catch (Throwable t) {
            // Android SDK or older JDK without VarHandle — fall back to manual byte access
        }
        MH_SET_INT = setInt;
        MH_SET_SHORT = setShort;
        MH_SET_LONG = setLong;
        MH_GET_LONG = getLong;
    }

    private static void setInt(byte[] buf, int pos, int v) {
        if (MH_SET_INT != null) {
            try {
                MH_SET_INT.invokeExact(buf, pos, v);
                return;
            } catch (Throwable t) { /* fall through */ }
        }
        setIntFallback(buf, pos, v);
    }

    private static void setShort(byte[] buf, int pos, short v) {
        if (MH_SET_SHORT != null) {
            try {
                MH_SET_SHORT.invokeExact(buf, pos, v);
                return;
            } catch (Throwable t) { /* fall through */ }
        }
        setShortFallback(buf, pos, v);
    }

    private static void setLong(byte[] buf, int pos, long v) {
        if (MH_SET_LONG != null) {
            try {
                MH_SET_LONG.invokeExact(buf, pos, v);
                return;
            } catch (Throwable t) { /* fall through */ }
        }
        setLongFallback(buf, pos, v);
    }

    private static long getLong(byte[] buf, int pos) {
        if (MH_GET_LONG != null) {
            try {
                return (long) MH_GET_LONG.invokeExact(buf, pos);
            } catch (Throwable t) { /* fall through */ }
        }
        return getLongFallback(buf, pos);
    }

    // ------------------------------------------------------------------
    // Fallback byte-level implementations (used on Android / without VarHandle)
    // Package-private so tests can exercise them directly.
    // ------------------------------------------------------------------

    static void setIntFallback(byte[] buf, int pos, int v) {
        buf[pos]     = (byte) v;
        buf[pos + 1] = (byte) (v >> 8);
        buf[pos + 2] = (byte) (v >> 16);
        buf[pos + 3] = (byte) (v >> 24);
    }

    static void setShortFallback(byte[] buf, int pos, short v) {
        buf[pos]     = (byte) v;
        buf[pos + 1] = (byte) (v >> 8);
    }

    static void setLongFallback(byte[] buf, int pos, long v) {
        buf[pos]     = (byte) v;
        buf[pos + 1] = (byte) (v >> 8);
        buf[pos + 2] = (byte) (v >> 16);
        buf[pos + 3] = (byte) (v >> 24);
        buf[pos + 4] = (byte) (v >> 32);
        buf[pos + 5] = (byte) (v >> 40);
        buf[pos + 6] = (byte) (v >> 48);
        buf[pos + 7] = (byte) (v >> 56);
    }

    static long getLongFallback(byte[] buf, int pos) {
        return (buf[pos] & 0xFFL)
                | ((buf[pos + 1] & 0xFFL) << 8)
                | ((buf[pos + 2] & 0xFFL) << 16)
                | ((buf[pos + 3] & 0xFFL) << 24)
                | ((buf[pos + 4] & 0xFFL) << 32)
                | ((buf[pos + 5] & 0xFFL) << 40)
                | ((buf[pos + 6] & 0xFFL) << 48)
                | ((buf[pos + 7] & 0xFFL) << 56);
    }

    // ------------------------------------------------------------------
    // Lookup tables (verbatim from jsoniter-scala's JsonWriter.scala)
    // ------------------------------------------------------------------

    // Index i corresponds to Long.numberOfLeadingZeros(x) == i, for positive x.
    private static final long[] OFFSETS = {
            5088146770730811392L, 5088146770730811392L, 5088146770730811392L, 5088146770730811392L,
            5088146770730811392L, 5088146770730811392L, 5088146770730811392L, 5088146770730811392L,
            4889916394579099648L, 4889916394579099648L, 4889916394579099648L, 4610686018427387904L,
            4610686018427387904L, 4610686018427387904L, 4610686018427387904L, 4323355642275676160L,
            4323355642275676160L, 4323355642275676160L, 4035215266123964416L, 4035215266123964416L,
            4035215266123964416L, 3746993889972252672L, 3746993889972252672L, 3746993889972252672L,
            3746993889972252672L, 3458764413820540928L, 3458764413820540928L, 3458764413820540928L,
            3170534127668829184L, 3170534127668829184L, 3170534127668829184L, 2882303760517117440L,
            2882303760517117440L, 2882303760517117440L, 2882303760517117440L, 2594073385265405696L,
            2594073385265405696L, 2594073385265405696L, 2305843009203693952L, 2305843009203693952L,
            2305843009203693952L, 2017612633060982208L, 2017612633060982208L, 2017612633060982208L,
            2017612633060982208L, 1729382256910170464L, 1729382256910170464L, 1729382256910170464L,
            1441151880758548720L, 1441151880758548720L, 1441151880758548720L, 1152921504606845976L,
            1152921504606845976L, 1152921504606845976L, 1152921504606845976L, 864691128455135132L,
            864691128455135132L, 864691128455135132L, 576460752303423478L, 576460752303423478L,
            576460752303423478L, 576460752303423478L, 576460752303423478L, 576460752303423478L,
            576460752303423478L
    };

    // Two ASCII decimal digit chars (00-99) packed little-endian into a short.
    private static final short[] DIGITS = {
            12336, 12592, 12848, 13104, 13360, 13616, 13872, 14128, 14384, 14640,
            12337, 12593, 12849, 13105, 13361, 13617, 13873, 14129, 14385, 14641,
            12338, 12594, 12850, 13106, 13362, 13618, 13874, 14130, 14386, 14642,
            12339, 12595, 12851, 13107, 13363, 13619, 13875, 14131, 14387, 14643,
            12340, 12596, 12852, 13108, 13364, 13620, 13876, 14132, 14388, 14644,
            12341, 12597, 12853, 13109, 13365, 13621, 13877, 14133, 14389, 14645,
            12342, 12598, 12854, 13110, 13366, 13622, 13878, 14134, 14390, 14646,
            12343, 12599, 12855, 13111, 13367, 13623, 13879, 14135, 14391, 14647,
            12344, 12600, 12856, 13112, 13368, 13624, 13880, 14136, 14392, 14648,
            12345, 12601, 12857, 13113, 13369, 13625, 13881, 14137, 14393, 14649
    };

    // Table of 64-bit approximations of powers of ten used for float conversion,
    // indexed by (exponent + 32) for exponents -32..44.
    private static final long[] FLOAT_POW10S = {
            0xCFB11EAD453994BBL, 0x81CEB32C4B43FCF5L, 0xA2425FF75E14FC32L, 0xCAD2F7F5359A3B3FL,
            0xFD87B5F28300CA0EL, 0x9E74D1B791E07E49L, 0xC612062576589DDBL, 0xF79687AED3EEC552L,
            0x9ABE14CD44753B53L, 0xC16D9A0095928A28L, 0xF1C90080BAF72CB2L, 0x971DA05074DA7BEFL,
            0xBCE5086492111AEBL, 0xEC1E4A7DB69561A6L, 0x9392EE8E921D5D08L, 0xB877AA3236A4B44AL,
            0xE69594BEC44DE15CL, 0x901D7CF73AB0ACDAL, 0xB424DC35095CD810L, 0xE12E13424BB40E14L,
            0x8CBCCC096F5088CCL, 0xAFEBFF0BCB24AAFFL, 0xDBE6FECEBDEDD5BFL, 0x89705F4136B4A598L,
            0xABCC77118461CEFDL, 0xD6BF94D5E57A42BDL, 0x8637BD05AF6C69B6L, 0xA7C5AC471B478424L,
            0xD1B71758E219652CL, 0x83126E978D4FDF3CL, 0xA3D70A3D70A3D70BL, 0xCCCCCCCCCCCCCCCDL,
            0x8000000000000000L, 0xA000000000000000L, 0xC800000000000000L, 0xFA00000000000000L,
            0x9C40000000000000L, 0xC350000000000000L, 0xF424000000000000L, 0x9896800000000000L,
            0xBEBC200000000000L, 0xEE6B280000000000L, 0x9502F90000000000L, 0xBA43B74000000000L,
            0xE8D4A51000000000L, 0x9184E72A00000000L, 0xB5E620F480000000L, 0xE35FA931A0000000L,
            0x8E1BC9BF04000000L, 0xB1A2BC2EC5000000L, 0xDE0B6B3A76400000L, 0x8AC7230489E80000L,
            0xAD78EBC5AC620000L, 0xD8D726B7177A8000L, 0x878678326EAC9000L, 0xA968163F0A57B400L,
            0xD3C21BCECCEDA100L, 0x84595161401484A0L, 0xA56FA5B99019A5C8L, 0xCECB8F27F4200F3AL,
            0x813F3978F8940985L, 0xA18F07D736B90BE6L, 0xC9F2C9CD04674EDFL, 0xFC6F7C4045812297L,
            0x9DC5ADA82B70B59EL, 0xC5371912364CE306L, 0xF684DF56C3E01BC7L, 0x9A130B963A6C115DL,
            0xC097CE7BC90715B4L, 0xF0BDC21ABB48DB21L, 0x96769950B50D88F5L, 0xBC143FA4E250EB32L,
            0xEB194F8E1AE525FEL, 0x92EFD1B8D0CF37BFL, 0xB7ABC627050305AEL, 0xE596B7B0C643C71AL,
            0x8F7E32CE7BEA5C70L
    };

    // Table of 128-bit (hi, lo) approximations of powers of ten used for double conversion,
    // indexed by pairs, for exponents -293..324 (see the "(292 - e10) << 1" indexing above).
    private static final long[] DOUBLE_POW10S = {
            0xCC5FC196FEFD7D0CL, 0x1E53ED49A96272C9L, 0xFF77B1FCBEBCDC4FL, 0x25E8E89C13BB0F7BL,
            0x9FAACF3DF73609B1L, 0x77B191618C54E9ADL, 0xC795830D75038C1DL, 0xD59DF5B9EF6A2418L,
            0xF97AE3D0D2446F25L, 0x4B0573286B44AD1EL, 0x9BECCE62836AC577L, 0x4EE367F9430AEC33L,
            0xC2E801FB244576D5L, 0x229C41F793CDA740L, 0xF3A20279ED56D48AL, 0x6B43527578C11110L,
            0x9845418C345644D6L, 0x830A13896B78AAAAL, 0xBE5691EF416BD60CL, 0x23CC986BC656D554L,
            0xEDEC366B11C6CB8FL, 0x2CBFBE86B7EC8AA9L, 0x94B3A202EB1C3F39L, 0x7BF7D71432F3D6AAL,
            0xB9E08A83A5E34F07L, 0xDAF5CCD93FB0CC54L, 0xE858AD248F5C22C9L, 0xD1B3400F8F9CFF69L,
            0x91376C36D99995BEL, 0x23100809B9C21FA2L, 0xB58547448FFFFB2DL, 0xABD40A0C2832A78BL,
            0xE2E69915B3FFF9F9L, 0x16C90C8F323F516DL, 0x8DD01FAD907FFC3BL, 0xAE3DA7D97F6792E4L,
            0xB1442798F49FFB4AL, 0x99CD11CFDF41779DL, 0xDD95317F31C7FA1DL, 0x40405643D711D584L,
            0x8A7D3EEF7F1CFC52L, 0x482835EA666B2573L, 0xAD1C8EAB5EE43B66L, 0xDA3243650005EED0L,
            0xD863B256369D4A40L, 0x90BED43E40076A83L, 0x873E4F75E2224E68L, 0x5A7744A6E804A292L,
            0xA90DE3535AAAE202L, 0x711515D0A205CB37L, 0xD3515C2831559A83L, 0x0D5A5B44CA873E04L,
            0x8412D9991ED58091L, 0xE858790AFE9486C3L, 0xA5178FFF668AE0B6L, 0x626E974DBE39A873L,
            0xCE5D73FF402D98E3L, 0xFB0A3D212DC81290L, 0x80FA687F881C7F8EL, 0x7CE66634BC9D0B9AL,
            0xA139029F6A239F72L, 0x1C1FFFC1EBC44E81L, 0xC987434744AC874EL, 0xA327FFB266B56221L,
            0xFBE9141915D7A922L, 0x4BF1FF9F0062BAA9L, 0x9D71AC8FADA6C9B5L, 0x6F773FC3603DB4AAL,
            0xC4CE17B399107C22L, 0xCB550FB4384D21D4L, 0xF6019DA07F549B2BL, 0x7E2A53A146606A49L,
            0x99C102844F94E0FBL, 0x2EDA7444CBFC426EL, 0xC0314325637A1939L, 0xFA911155FEFB5309L,
            0xF03D93EEBC589F88L, 0x793555AB7EBA27CBL, 0x96267C7535B763B5L, 0x4BC1558B2F3458DFL,
            0xBBB01B9283253CA2L, 0x9EB1AAEDFB016F17L, 0xEA9C227723EE8BCBL, 0x465E15A979C1CADDL,
            0x92A1958A7675175FL, 0x0BFACD89EC191ECAL, 0xB749FAED14125D36L, 0xCEF980EC671F667CL,
            0xE51C79A85916F484L, 0x82B7E12780E7401BL, 0x8F31CC0937AE58D2L, 0xD1B2ECB8B0908811L,
            0xB2FE3F0B8599EF07L, 0x861FA7E6DCB4AA16L, 0xDFBDCECE67006AC9L, 0x67A791E093E1D49BL,
            0x8BD6A141006042BDL, 0xE0C8BB2C5C6D24E1L, 0xAECC49914078536DL, 0x58FAE9F773886E19L,
            0xDA7F5BF590966848L, 0xAF39A475506A899FL, 0x888F99797A5E012DL, 0x6D8406C952429604L,
            0xAAB37FD7D8F58178L, 0xC8E5087BA6D33B84L, 0xD5605FCDCF32E1D6L, 0xFB1E4A9A90880A65L,
            0x855C3BE0A17FCD26L, 0x5CF2EEA09A550680L, 0xA6B34AD8C9DFC06FL, 0xF42FAA48C0EA481FL,
            0xD0601D8EFC57B08BL, 0xF13B94DAF124DA27L, 0x823C12795DB6CE57L, 0x76C53D08D6B70859L,
            0xA2CB1717B52481EDL, 0x54768C4B0C64CA6FL, 0xCB7DDCDDA26DA268L, 0xA9942F5DCF7DFD0AL,
            0xFE5D54150B090B02L, 0xD3F93B35435D7C4DL, 0x9EFA548D26E5A6E1L, 0xC47BC5014A1A6DB0L,
            0xC6B8E9B0709F109AL, 0x359AB6419CA1091CL, 0xF867241C8CC6D4C0L, 0xC30163D203C94B63L,
            0x9B407691D7FC44F8L, 0x79E0DE63425DCF1EL, 0xC21094364DFB5636L, 0x985915FC12F542E5L,
            0xF294B943E17A2BC4L, 0x3E6F5B7B17B2939EL, 0x979CF3CA6CEC5B5AL, 0xA705992CEECF9C43L,
            0xBD8430BD08277231L, 0x50C6FF782A838354L, 0xECE53CEC4A314EBDL, 0xA4F8BF5635246429L,
            0x940F4613AE5ED136L, 0x871B7795E136BE9AL, 0xB913179899F68584L, 0x28E2557B59846E40L,
            0xE757DD7EC07426E5L, 0x331AEADA2FE589D0L, 0x9096EA6F3848984FL, 0x3FF0D2C85DEF7622L,
            0xB4BCA50B065ABE63L, 0x0FED077A756B53AAL, 0xE1EBCE4DC7F16DFBL, 0xD3E8495912C62895L,
            0x8D3360F09CF6E4BDL, 0x64712DD7ABBBD95DL, 0xB080392CC4349DECL, 0xBD8D794D96AACFB4L,
            0xDCA04777F541C567L, 0xECF0D7A0FC5583A1L, 0x89E42CAAF9491B60L, 0xF41686C49DB57245L,
            0xAC5D37D5B79B6239L, 0x311C2875C522CED6L, 0xD77485CB25823AC7L, 0x7D633293366B828CL,
            0x86A8D39EF77164BCL, 0xAE5DFF9C02033198L, 0xA8530886B54DBDEBL, 0xD9F57F830283FDFDL,
            0xD267CAA862A12D66L, 0xD072DF63C324FD7CL, 0x8380DEA93DA4BC60L, 0x4247CB9E59F71E6EL,
            0xA46116538D0DEB78L, 0x52D9BE85F074E609L, 0xCD795BE870516656L, 0x67902E276C921F8CL,
            0x806BD9714632DFF6L, 0x00BA1CD8A3DB53B7L, 0xA086CFCD97BF97F3L, 0x80E8A40ECCD228A5L,
            0xC8A883C0FDAF7DF0L, 0x6122CD128006B2CEL, 0xFAD2A4B13D1B5D6CL, 0x796B805720085F82L,
            0x9CC3A6EEC6311A63L, 0xCBE3303674053BB1L, 0xC3F490AA77BD60FCL, 0xBEDBFC4411068A9DL,
            0xF4F1B4D515ACB93BL, 0xEE92FB5515482D45L, 0x991711052D8BF3C5L, 0x751BDD152D4D1C4BL,
            0xBF5CD54678EEF0B6L, 0xD262D45A78A0635EL, 0xEF340A98172AACE4L, 0x86FB897116C87C35L,
            0x9580869F0E7AAC0EL, 0xD45D35E6AE3D4DA1L, 0xBAE0A846D2195712L, 0x8974836059CCA10AL,
            0xE998D258869FACD7L, 0x2BD1A438703FC94CL, 0x91FF83775423CC06L, 0x7B6306A34627DDD0L,
            0xB67F6455292CBF08L, 0x1A3BC84C17B1D543L, 0xE41F3D6A7377EECAL, 0x20CABA5F1D9E4A94L,
            0x8E938662882AF53EL, 0x547EB47B7282EE9DL, 0xB23867FB2A35B28DL, 0xE99E619A4F23AA44L,
            0xDEC681F9F4C31F31L, 0x6405FA00E2EC94D5L, 0x8B3C113C38F9F37EL, 0xDE83BC408DD3DD05L,
            0xAE0B158B4738705EL, 0x9624AB50B148D446L, 0xD98DDAEE19068C76L, 0x3BADD624DD9B0958L,
            0x87F8A8D4CFA417C9L, 0xE54CA5D70A80E5D7L, 0xA9F6D30A038D1DBCL, 0x5E9FCF4CCD211F4DL,
            0xD47487CC8470652BL, 0x7647C32000696720L, 0x84C8D4DFD2C63F3BL, 0x29ECD9F40041E074L,
            0xA5FB0A17C777CF09L, 0xF468107100525891L, 0xCF79CC9DB955C2CCL, 0x7182148D4066EEB5L,
            0x81AC1FE293D599BFL, 0xC6F14CD848405531L, 0xA21727DB38CB002FL, 0xB8ADA00E5A506A7DL,
            0xCA9CF1D206FDC03BL, 0xA6D90811F0E4851DL, 0xFD442E4688BD304AL, 0x908F4A166D1DA664L,
            0x9E4A9CEC15763E2EL, 0x9A598E4E043287FFL, 0xC5DD44271AD3CDBAL, 0x40EFF1E1853F29FEL,
            0xF7549530E188C128L, 0xD12BEE59E68EF47DL, 0x9A94DD3E8CF578B9L, 0x82BB74F8301958CFL,
            0xC13A148E3032D6E7L, 0xE36A52363C1FAF02L, 0xF18899B1BC3F8CA1L, 0xDC44E6C3CB279AC2L,
            0x96F5600F15A7B7E5L, 0x29AB103A5EF8C0BAL, 0xBCB2B812DB11A5DEL, 0x7415D448F6B6F0E8L,
            0xEBDF661791D60F56L, 0x111B495B3464AD22L, 0x936B9FCEBB25C995L, 0xCAB10DD900BEEC35L,
            0xB84687C269EF3BFBL, 0x3D5D514F40EEA743L, 0xE65829B3046B0AFAL, 0x0CB4A5A3112A5113L,
            0x8FF71A0FE2C2E6DCL, 0x47F0E785EABA72ACL, 0xB3F4E093DB73A093L, 0x59ED216765690F57L,
            0xE0F218B8D25088B8L, 0x306869C13EC3532DL, 0x8C974F7383725573L, 0x1E414218C73A13FCL,
            0xAFBD2350644EEACFL, 0xE5D1929EF90898FBL, 0xDBAC6C247D62A583L, 0xDF45F746B74ABF3AL,
            0x894BC396CE5DA772L, 0x6B8BBA8C328EB784L, 0xAB9EB47C81F5114FL, 0x066EA92F3F326565L,
            0xD686619BA27255A2L, 0xC80A537B0EFEFEBEL, 0x8613FD0145877585L, 0xBD06742CE95F5F37L,
            0xA798FC4196E952E7L, 0x2C48113823B73705L, 0xD17F3B51FCA3A7A0L, 0xF75A15862CA504C6L,
            0x82EF85133DE648C4L, 0x9A984D73DBE722FCL, 0xA3AB66580D5FDAF5L, 0xC13E60D0D2E0EBBBL,
            0xCC963FEE10B7D1B3L, 0x318DF905079926A9L, 0xFFBBCFE994E5C61FL, 0xFDF17746497F7053L,
            0x9FD561F1FD0F9BD3L, 0xFEB6EA8BEDEFA634L, 0xC7CABA6E7C5382C8L, 0xFE64A52EE96B8FC1L,
            0xF9BD690A1B68637BL, 0x3DFDCE7AA3C673B1L, 0x9C1661A651213E2DL, 0x06BEA10CA65C084FL,
            0xC31BFA0FE5698DB8L, 0x486E494FCFF30A63L, 0xF3E2F893DEC3F126L, 0x5A89DBA3C3EFCCFBL,
            0x986DDB5C6B3A76B7L, 0xF89629465A75E01DL, 0xBE89523386091465L, 0xF6BBB397F1135824L,
            0xEE2BA6C0678B597FL, 0x746AA07DED582E2DL, 0x94DB483840B717EFL, 0xA8C2A44EB4571CDDL,
            0xBA121A4650E4DDEBL, 0x92F34D62616CE414L, 0xE896A0D7E51E1566L, 0x77B020BAF9C81D18L,
            0x915E2486EF32CD60L, 0x0ACE1474DC1D122FL, 0xB5B5ADA8AAFF80B8L, 0x0D819992132456BBL,
            0xE3231912D5BF60E6L, 0x10E1FFF697ED6C6AL, 0x8DF5EFABC5979C8FL, 0xCA8D3FFA1EF463C2L,
            0xB1736B96B6FD83B3L, 0xBD308FF8A6B17CB3L, 0xDDD0467C64BCE4A0L, 0xAC7CB3F6D05DDBDFL,
            0x8AA22C0DBEF60EE4L, 0x6BCDF07A423AA96CL, 0xAD4AB7112EB3929DL, 0x86C16C98D2C953C7L,
            0xD89D64D57A607744L, 0xE871C7BF077BA8B8L, 0x87625F056C7C4A8BL, 0x11471CD764AD4973L,
            0xA93AF6C6C79B5D2DL, 0xD598E40D3DD89BD0L, 0xD389B47879823479L, 0x4AFF1D108D4EC2C4L,
            0x843610CB4BF160CBL, 0xCEDF722A585139BBL, 0xA54394FE1EEDB8FEL, 0xC2974EB4EE658829L,
            0xCE947A3DA6A9273EL, 0x733D226229FEEA33L, 0x811CCC668829B887L, 0x0806357D5A3F5260L,
            0xA163FF802A3426A8L, 0xCA07C2DCB0CF26F8L, 0xC9BCFF6034C13052L, 0xFC89B393DD02F0B6L,
            0xFC2C3F3841F17C67L, 0xBBAC2078D443ACE3L, 0x9D9BA7832936EDC0L, 0xD54B944B84AA4C0EL,
            0xC5029163F384A931L, 0x0A9E795E65D4DF12L, 0xF64335BCF065D37DL, 0x4D4617B5FF4A16D6L,
            0x99EA0196163FA42EL, 0x504BCED1BF8E4E46L, 0xC06481FB9BCF8D39L, 0xE45EC2862F71E1D7L,
            0xF07DA27A82C37088L, 0x5D767327BB4E5A4DL, 0x964E858C91BA2655L, 0x3A6A07F8D510F870L,
            0xBBE226EFB628AFEAL, 0x890489F70A55368CL, 0xEADAB0ABA3B2DBE5L, 0x2B45AC74CCEA842FL,
            0x92C8AE6B464FC96FL, 0x3B0B8BC90012929EL, 0xB77ADA0617E3BBCBL, 0x09CE6EBB40173745L,
            0xE55990879DDCAABDL, 0xCC420A6A101D0516L, 0x8F57FA54C2A9EAB6L, 0x9FA946824A12232EL,
            0xB32DF8E9F3546564L, 0x47939822DC96ABFAL, 0xDFF9772470297EBDL, 0x59787E2B93BC56F8L,
            0x8BFBEA76C619EF36L, 0x57EB4EDB3C55B65BL, 0xAEFAE51477A06B03L, 0xEDE622920B6B23F2L,
            0xDAB99E59958885C4L, 0xE95FAB368E45ECEEL, 0x88B402F7FD75539BL, 0x11DBCB0218EBB415L,
            0xAAE103B5FCD2A881L, 0xD652BDC29F26A11AL, 0xD59944A37C0752A2L, 0x4BE76D3346F04960L,
            0x857FCAE62D8493A5L, 0x6F70A4400C562DDCL, 0xA6DFBD9FB8E5B88EL, 0xCB4CCD500F6BB953L,
            0xD097AD07A71F26B2L, 0x7E2000A41346A7A8L, 0x825ECC24C873782FL, 0x8ED400668C0C28C9L,
            0xA2F67F2DFA90563BL, 0x728900802F0F32FBL, 0xCBB41EF979346BCAL, 0x4F2B40A03AD2FFBAL,
            0xFEA126B7D78186BCL, 0xE2F610C84987BFA9L, 0x9F24B832E6B0F436L, 0x0DD9CA7D2DF4D7CAL,
            0xC6EDE63FA05D3143L, 0x91503D1C79720DBCL, 0xF8A95FCF88747D94L, 0x75A44C6397CE912BL,
            0x9B69DBE1B548CE7CL, 0xC986AFBE3EE11ABBL, 0xC24452DA229B021BL, 0xFBE85BADCE996169L,
            0xF2D56790AB41C2A2L, 0xFAE27299423FB9C4L, 0x97C560BA6B0919A5L, 0xDCCD879FC967D41BL,
            0xBDB6B8E905CB600FL, 0x5400E987BBC1C921L, 0xED246723473E3813L, 0x290123E9AAB23B69L,
            0x9436C0760C86E30BL, 0xF9A0B6720AAF6522L, 0xB94470938FA89BCEL, 0xF808E40E8D5B3E6AL,
            0xE7958CB87392C2C2L, 0xB60B1D1230B20E05L, 0x90BD77F3483BB9B9L, 0xB1C6F22B5E6F48C3L,
            0xB4ECD5F01A4AA828L, 0x1E38AEB6360B1AF4L, 0xE2280B6C20DD5232L, 0x25C6DA63C38DE1B1L,
            0x8D590723948A535FL, 0x579C487E5A38AD0FL, 0xB0AF48EC79ACE837L, 0x2D835A9DF0C6D852L,
            0xDCDB1B2798182244L, 0xF8E431456CF88E66L, 0x8A08F0F8BF0F156BL, 0x1B8E9ECB641B5900L,
            0xAC8B2D36EED2DAC5L, 0xE272467E3D222F40L, 0xD7ADF884AA879177L, 0x5B0ED81DCC6ABB10L,
            0x86CCBB52EA94BAEAL, 0x98E947129FC2B4EAL, 0xA87FEA27A539E9A5L, 0x3F2398D747B36225L,
            0xD29FE4B18E88640EL, 0x8EEC7F0D19A03AAEL, 0x83A3EEEEF9153E89L, 0x1953CF68300424ADL,
            0xA48CEAAAB75A8E2BL, 0x5FA8C3423C052DD8L, 0xCDB02555653131B6L, 0x3792F412CB06794EL,
            0x808E17555F3EBF11L, 0xE2BBD88BBEE40BD1L, 0xA0B19D2AB70E6ED6L, 0x5B6ACEAEAE9D0EC5L,
            0xC8DE047564D20A8BL, 0xF245825A5A445276L, 0xFB158592BE068D2EL, 0xEED6E2F0F0D56713L,
            0x9CED737BB6C4183DL, 0x55464DD69685606CL, 0xC428D05AA4751E4CL, 0xAA97E14C3C26B887L,
            0xF53304714D9265DFL, 0xD53DD99F4B3066A9L, 0x993FE2C6D07B7FABL, 0xE546A8038EFE402AL,
            0xBF8FDB78849A5F96L, 0xDE98520472BDD034L, 0xEF73D256A5C0F77CL, 0x963E66858F6D4441L,
            0x95A8637627989AADL, 0xDDE7001379A44AA9L, 0xBB127C53B17EC159L, 0x5560C018580D5D53L,
            0xE9D71B689DDE71AFL, 0xAAB8F01E6E10B4A7L, 0x9226712162AB070DL, 0xCAB3961304CA70E9L,
            0xB6B00D69BB55C8D1L, 0x3D607B97C5FD0D23L, 0xE45C10C42A2B3B05L, 0x8CB89A7DB77C506BL,
            0x8EB98A7A9A5B04E3L, 0x77F3608E92ADB243L, 0xB267ED1940F1C61CL, 0x55F038B237591ED4L,
            0xDF01E85F912E37A3L, 0x6B6C46DEC52F6689L, 0x8B61313BBABCE2C6L, 0x2323AC4B3B3DA016L,
            0xAE397D8AA96C1B77L, 0xABEC975E0A0D081BL, 0xD9C7DCED53C72255L, 0x96E7BD358C904A22L,
            0x881CEA14545C7575L, 0x7E50D64177DA2E55L, 0xAA242499697392D2L, 0xDDE50BD1D5D0B9EAL,
            0xD4AD2DBFC3D07787L, 0x955E4EC64B44E865L, 0x84EC3C97DA624AB4L, 0xBD5AF13BEF0B113FL,
            0xA6274BBDD0FADD61L, 0xECB1AD8AEACDD58FL, 0xCFB11EAD453994BAL, 0x67DE18EDA5814AF3L,
            0x81CEB32C4B43FCF4L, 0x80EACF948770CED8L, 0xA2425FF75E14FC31L, 0xA1258379A94D028EL,
            0xCAD2F7F5359A3B3EL, 0x096EE45813A04331L, 0xFD87B5F28300CA0DL, 0x8BCA9D6E188853FDL,
            0x9E74D1B791E07E48L, 0x775EA264CF55347EL, 0xC612062576589DDAL, 0x95364AFE032A819EL,
            0xF79687AED3EEC551L, 0x3A83DDBD83F52205L, 0x9ABE14CD44753B52L, 0xC4926A9672793543L,
            0xC16D9A0095928A27L, 0x75B7053C0F178294L, 0xF1C90080BAF72CB1L, 0x5324C68B12DD6339L,
            0x971DA05074DA7BEEL, 0xD3F6FC16EBCA5E04L, 0xBCE5086492111AEAL, 0x88F4BB1CA6BCF585L,
            0xEC1E4A7DB69561A5L, 0x2B31E9E3D06C32E6L, 0x9392EE8E921D5D07L, 0x3AFF322E62439FD0L,
            0xB877AA3236A4B449L, 0x09BEFEB9FAD487C3L, 0xE69594BEC44DE15BL, 0x4C2EBE687989A9B4L,
            0x901D7CF73AB0ACD9L, 0x0F9D37014BF60A11L, 0xB424DC35095CD80FL, 0x538484C19EF38C95L,
            0xE12E13424BB40E13L, 0x2865A5F206B06FBAL, 0x8CBCCC096F5088CBL, 0xF93F87B7442E45D4L,
            0xAFEBFF0BCB24AAFEL, 0xF78F69A51539D749L, 0xDBE6FECEBDEDD5BEL, 0xB573440E5A884D1CL,
            0x89705F4136B4A597L, 0x31680A88F8953031L, 0xABCC77118461CEFCL, 0xFDC20D2B36BA7C3EL,
            0xD6BF94D5E57A42BCL, 0x3D32907604691B4DL, 0x8637BD05AF6C69B5L, 0xA63F9A49C2C1B110L,
            0xA7C5AC471B478423L, 0x0FCF80DC33721D54L, 0xD1B71758E219652BL, 0xD3C36113404EA4A9L,
            0x83126E978D4FDF3BL, 0x645A1CAC083126EAL, 0xA3D70A3D70A3D70AL, 0x3D70A3D70A3D70A4L,
            0xCCCCCCCCCCCCCCCCL, 0xCCCCCCCCCCCCCCCDL, 0x8000000000000000L, 0x0L,
            0xA000000000000000L, 0x0L, 0xC800000000000000L, 0x0L,
            0xFA00000000000000L, 0x0L, 0x9C40000000000000L, 0x0L,
            0xC350000000000000L, 0x0L, 0xF424000000000000L, 0x0L,
            0x9896800000000000L, 0x0L, 0xBEBC200000000000L, 0x0L,
            0xEE6B280000000000L, 0x0L, 0x9502F90000000000L, 0x0L,
            0xBA43B74000000000L, 0x0L, 0xE8D4A51000000000L, 0x0L,
            0x9184E72A00000000L, 0x0L, 0xB5E620F480000000L, 0x0L,
            0xE35FA931A0000000L, 0x0L, 0x8E1BC9BF04000000L, 0x0L,
            0xB1A2BC2EC5000000L, 0x0L, 0xDE0B6B3A76400000L, 0x0L,
            0x8AC7230489E80000L, 0x0L, 0xAD78EBC5AC620000L, 0x0L,
            0xD8D726B7177A8000L, 0x0L, 0x878678326EAC9000L, 0x0L,
            0xA968163F0A57B400L, 0x0L, 0xD3C21BCECCEDA100L, 0x0L,
            0x84595161401484A0L, 0x0L, 0xA56FA5B99019A5C8L, 0x0L,
            0xCECB8F27F4200F3AL, 0x0L, 0x813F3978F8940984L, 0x4000000000000000L,
            0xA18F07D736B90BE5L, 0x5000000000000000L, 0xC9F2C9CD04674EDEL, 0xA400000000000000L,
            0xFC6F7C4045812296L, 0x4D00000000000000L, 0x9DC5ADA82B70B59DL, 0xF020000000000000L,
            0xC5371912364CE305L, 0x6C28000000000000L, 0xF684DF56C3E01BC6L, 0xC732000000000000L,
            0x9A130B963A6C115CL, 0x3C7F400000000000L, 0xC097CE7BC90715B3L, 0x4B9F100000000000L,
            0xF0BDC21ABB48DB20L, 0x1E86D40000000000L, 0x96769950B50D88F4L, 0x1314448000000000L,
            0xBC143FA4E250EB31L, 0x17D955A000000000L, 0xEB194F8E1AE525FDL, 0x5DCFAB0800000000L,
            0x92EFD1B8D0CF37BEL, 0x5AA1CAE500000000L, 0xB7ABC627050305ADL, 0xF14A3D9E40000000L,
            0xE596B7B0C643C719L, 0x6D9CCD05D0000000L, 0x8F7E32CE7BEA5C6FL, 0xE4820023A2000000L,
            0xB35DBF821AE4F38BL, 0xDDA2802C8A800000L, 0xE0352F62A19E306EL, 0xD50B2037AD200000L,
            0x8C213D9DA502DE45L, 0x4526F422CC340000L, 0xAF298D050E4395D6L, 0x9670B12B7F410000L,
            0xDAF3F04651D47B4CL, 0x3C0CDD765F114000L, 0x88D8762BF324CD0FL, 0xA5880A69FB6AC800L,
            0xAB0E93B6EFEE0053L, 0x8EEA0D047A457A00L, 0xD5D238A4ABE98068L, 0x72A4904598D6D880L,
            0x85A36366EB71F041L, 0x47A6DA2B7F864750L, 0xA70C3C40A64E6C51L, 0x999090B65F67D924L,
            0xD0CF4B50CFE20765L, 0xFFF4B4E3F741CF6DL, 0x82818F1281ED449FL, 0xBFF8F10E7A8921A5L,
            0xA321F2D7226895C7L, 0xAFF72D52192B6A0EL, 0xCBEA6F8CEB02BB39L, 0x9BF4F8A69F764491L,
            0xFEE50B7025C36A08L, 0x02F236D04753D5B5L, 0x9F4F2726179A2245L, 0x01D762422C946591L,
            0xC722F0EF9D80AAD6L, 0x424D3AD2B7B97EF6L, 0xF8EBAD2B84E0D58BL, 0xD2E0898765A7DEB3L,
            0x9B934C3B330C8577L, 0x63CC55F49F88EB30L, 0xC2781F49FFCFA6D5L, 0x3CBF6B71C76B25FCL,
            0xF316271C7FC3908AL, 0x8BEF464E3945EF7BL, 0x97EDD871CFDA3A56L, 0x97758BF0E3CBB5ADL,
            0xBDE94E8E43D0C8ECL, 0x3D52EEED1CBEA318L, 0xED63A231D4C4FB27L, 0x4CA7AAA863EE4BDEL,
            0x945E455F24FB1CF8L, 0x8FE8CAA93E74EF6BL, 0xB975D6B6EE39E436L, 0xB3E2FD538E122B45L,
            0xE7D34C64A9C85D44L, 0x60DBBCA87196B617L, 0x90E40FBEEA1D3A4AL, 0xBC8955E946FE31CEL,
            0xB51D13AEA4A488DDL, 0x6BABAB6398BDBE42L, 0xE264589A4DCDAB14L, 0xC696963C7EED2DD2L,
            0x8D7EB76070A08AECL, 0xFC1E1DE5CF543CA3L, 0xB0DE65388CC8ADA8L, 0x3B25A55F43294BCCL,
            0xDD15FE86AFFAD912L, 0x49EF0EB713F39EBFL, 0x8A2DBF142DFCC7ABL, 0x6E3569326C784338L,
            0xACB92ED9397BF996L, 0x49C2C37F07965405L, 0xD7E77A8F87DAF7FBL, 0xDC33745EC97BE907L,
            0x86F0AC99B4E8DAFDL, 0x69A028BB3DED71A4L, 0xA8ACD7C0222311BCL, 0xC40832EA0D68CE0DL,
            0xD2D80DB02AABD62BL, 0xF50A3FA490C30191L, 0x83C7088E1AAB65DBL, 0x792667C6DA79E0FBL,
            0xA4B8CAB1A1563F52L, 0x577001B891185939L, 0xCDE6FD5E09ABCF26L, 0xED4C0226B55E6F87L,
            0x80B05E5AC60B6178L, 0x544F8158315B05B5L, 0xA0DC75F1778E39D6L, 0x696361AE3DB1C722L,
            0xC913936DD571C84CL, 0x03BC3A19CD1E38EAL, 0xFB5878494ACE3A5FL, 0x04AB48A04065C724L,
            0x9D174B2DCEC0E47BL, 0x62EB0D64283F9C77L, 0xC45D1DF942711D9AL, 0x3BA5D0BD324F8395L,
            0xF5746577930D6500L, 0xCA8F44EC7EE3647AL, 0x9968BF6ABBE85F20L, 0x7E998B13CF4E1ECCL,
            0xBFC2EF456AE276E8L, 0x9E3FEDD8C321A67FL, 0xEFB3AB16C59B14A2L, 0xC5CFE94EF3EA101FL,
            0x95D04AEE3B80ECE5L, 0xBBA1F1D158724A13L, 0xBB445DA9CA61281FL, 0x2A8A6E45AE8EDC98L,
            0xEA1575143CF97226L, 0xF52D09D71A3293BEL, 0x924D692CA61BE758L, 0x593C2626705F9C57L,
            0xB6E0C377CFA2E12EL, 0x6F8B2FB00C77836DL, 0xE498F455C38B997AL, 0x0B6DFB9C0F956448L,
            0x8EDF98B59A373FECL, 0x4724BD4189BD5EADL, 0xB2977EE300C50FE7L, 0x58EDEC91EC2CB658L,
            0xDF3D5E9BC0F653E1L, 0x2F2967B66737E3EEL, 0x8B865B215899F46CL, 0xBD79E0D20082EE75L,
            0xAE67F1E9AEC07187L, 0xECD8590680A3AA12L, 0xDA01EE641A708DE9L, 0xE80E6F4820CC9496L,
            0x884134FE908658B2L, 0x3109058D147FDCDEL, 0xAA51823E34A7EEDEL, 0xBD4B46F0599FD416L,
            0xD4E5E2CDC1D1EA96L, 0x6C9E18AC7007C91BL, 0x850FADC09923329EL, 0x03E2CF6BC604DDB1L,
            0xA6539930BF6BFF45L, 0x84DB8346B786151DL, 0xCFE87F7CEF46FF16L, 0xE612641865679A64L,
            0x81F14FAE158C5F6EL, 0x4FCB7E8F3F60C07FL, 0xA26DA3999AEF7749L, 0xE3BE5E330F38F09EL,
            0xCB090C8001AB551CL, 0x5CADF5BFD3072CC6L, 0xFDCB4FA002162A63L, 0x73D9732FC7C8F7F7L,
            0x9E9F11C4014DDA7EL, 0x2867E7FDDCDD9AFBL, 0xC646D63501A1511DL, 0xB281E1FD541501B9L,
            0xF7D88BC24209A565L, 0x1F225A7CA91A4227L, 0x9AE757596946075FL, 0x3375788DE9B06959L,
            0xC1A12D2FC3978937L, 0x0052D6B1641C83AFL, 0xF209787BB47D6B84L, 0xC0678C5DBD23A49BL,
            0x9745EB4D50CE6332L, 0xF840B7BA963646E1L, 0xBD176620A501FBFFL, 0xB650E5A93BC3D899L,
            0xEC5D3FA8CE427AFFL, 0xA3E51F138AB4CEBFL, 0x93BA47C980E98CDFL, 0xC66F336C36B10138L,
            0xB8A8D9BBE123F017L, 0xB80B0047445D4185L, 0xE6D3102AD96CEC1DL, 0xA60DC059157491E6L,
            0x9043EA1AC7E41392L, 0x87C89837AD68DB30L, 0xB454E4A179DD1877L, 0x29BABE4598C311FCL,
            0xE16A1DC9D8545E94L, 0xF4296DD6FEF3D67BL, 0x8CE2529E2734BB1DL, 0x1899E4A65F58660DL,
            0xB01AE745B101E9E4L, 0x5EC05DCFF72E7F90L, 0xDC21A1171D42645DL, 0x76707543F4FA1F74L,
            0x899504AE72497EBAL, 0x6A06494A791C53A9L, 0xABFA45DA0EDBDE69L, 0x0487DB9D17636893L,
            0xD6F8D7509292D603L, 0x45A9D2845D3C42B7L, 0x865B86925B9BC5C2L, 0x0B8A2392BA45A9B3L,
            0xA7F26836F282B732L, 0x8E6CAC7768D7141FL, 0xD1EF0244AF2364FFL, 0x3207D795430CD927L,
            0x8335616AED761F1FL, 0x7F44E6BD49E807B9L, 0xA402B9C5A8D3A6E7L, 0x5F16206C9C6209A7L,
            0xCD036837130890A1L, 0x36DBA887C37A8C10L, 0x802221226BE55A64L, 0xC2494954DA2C978AL,
            0xA02AA96B06DEB0FDL, 0xF2DB9BAA10B7BD6DL, 0xC83553C5C8965D3DL, 0x6F92829494E5ACC8L,
            0xFA42A8B73ABBF48CL, 0xCB772339BA1F17FAL, 0x9C69A97284B578D7L, 0xFF2A760414536EFCL,
            0xC38413CF25E2D70DL, 0xFEF5138519684ABBL, 0xF46518C2EF5B8CD1L, 0x7EB258665FC25D6AL,
            0x98BF2F79D5993802L, 0xEF2F773FFBD97A62L, 0xBEEEFB584AFF8603L, 0xAAFB550FFACFD8FBL,
            0xEEAABA2E5DBF6784L, 0x95BA2A53F983CF39L, 0x952AB45CFA97A0B2L, 0xDD945A747BF26184L,
            0xBA756174393D88DFL, 0x94F971119AEEF9E5L, 0xE912B9D1478CEB17L, 0x7A37CD5601AAB85EL,
            0x91ABB422CCB812EEL, 0xAC62E055C10AB33BL, 0xB616A12B7FE617AAL, 0x577B986B314D600AL,
            0xE39C49765FDF9D94L, 0xED5A7E85FDA0B80CL, 0x8E41ADE9FBEBC27DL, 0x14588F13BE847308L,
            0xB1D219647AE6B31CL, 0x596EB2D8AE258FC9L, 0xDE469FBD99A05FE3L, 0x6FCA5F8ED9AEF3BCL,
            0x8AEC23D680043BEEL, 0x25DE7BB9480D5855L, 0xADA72CCC20054AE9L, 0xAF561AA79A10AE6BL,
            0xD910F7FF28069DA4L, 0x1B2BA1518094DA05L, 0x87AA9AFF79042286L, 0x90FB44D2F05D0843L,
            0xA99541BF57452B28L, 0x353A1607AC744A54L, 0xD3FA922F2D1675F2L, 0x42889B8997915CE9L,
            0x847C9B5D7C2E09B7L, 0x69956135FEBADA12L, 0xA59BC234DB398C25L, 0x43FAB9837E699096L,
            0xCF02B2C21207EF2EL, 0x94F967E45E03F4BCL, 0x8161AFB94B44F57DL, 0x1D1BE0EEBAC278F6L,
            0xA1BA1BA79E1632DCL, 0x6462D92A69731733L, 0xCA28A291859BBF93L, 0x7D7B8F7503CFDCFFL,
            0xFCB2CB35E702AF78L, 0x5CDA735244C3D43FL, 0x9DEFBF01B061ADABL, 0x3A0888136AFA64A8L,
            0xC56BAEC21C7A1916L, 0x088AAA1845B8FDD1L, 0xF6C69A72A3989F5BL, 0x8AAD549E57273D46L,
            0x9A3C2087A63F6399L, 0x36AC54E2F678864CL, 0xC0CB28A98FCF3C7FL, 0x84576A1BB416A7DEL,
            0xF0FDF2D3F3C30B9FL, 0x656D44A2A11C51D6L, 0x969EB7C47859E743L, 0x9F644AE5A4B1B326L,
            0xBC4665B596706114L, 0x873D5D9F0DDE1FEFL, 0xEB57FF22FC0C7959L, 0xA90CB506D155A7EBL,
            0x9316FF75DD87CBD8L, 0x09A7F12442D588F3L, 0xB7DCBF5354E9BECEL, 0x0C11ED6D538AEB30L,
            0xE5D3EF282A242E81L, 0x8F1668C8A86DA5FBL, 0x8FA475791A569D10L, 0xF96E017D694487BDL,
            0xB38D92D760EC4455L, 0x37C981DCC395A9ADL, 0xE070F78D3927556AL, 0x85BBE253F47B1418L,
            0x8C469AB843B89562L, 0x93956D7478CCEC8FL, 0xAF58416654A6BABBL, 0x387AC8D1970027B3L,
            0xDB2E51BFE9D0696AL, 0x06997B05FCC0319FL, 0x88FCF317F22241E2L, 0x441FECE3BDF81F04L,
            0xAB3C2FDDEEAAD25AL, 0xD527E81CAD7626C4L, 0xD60B3BD56A5586F1L, 0x8A71E223D8D3B075L,
            0x85C7056562757456L, 0xF6872D5667844E4AL, 0xA738C6BEBB12D16CL, 0xB428F8AC016561DCL,
            0xD106F86E69D785C7L, 0xE13336D701BEBA53L, 0x82A45B450226B39CL, 0xECC0024661173474L,
            0xA34D721642B06084L, 0x27F002D7F95D0191L, 0xCC20CE9BD35C78A5L, 0x31EC038DF7B441F5L,
            0xFF290242C83396CEL, 0x7E67047175A15272L, 0x9F79A169BD203E41L, 0x0F0062C6E984D387L,
            0xC75809C42C684DD1L, 0x52C07B78A3E60869L, 0xF92E0C3537826145L, 0xA7709A56CCDF8A83L,
            0x9BBCC7A142B17CCBL, 0x88A66076400BB692L, 0xC2ABF989935DDBFEL, 0x6ACFF893D00EA436L,
            0xF356F7EBF83552FEL, 0x0583F6B8C4124D44L, 0x98165AF37B2153DEL, 0xC3727A337A8B704BL,
            0xBE1BF1B059E9A8D6L, 0x744F18C0592E4C5DL, 0xEDA2EE1C7064130CL, 0x1162DEF06F79DF74L,
            0x9485D4D1C63E8BE7L, 0x8ADDCB5645AC2BA9L, 0xB9A74A0637CE2EE1L, 0x6D953E2BD7173693L,
            0xE8111C87C5C1BA99L, 0xC8FA8DB6CCDD0438L, 0x910AB1D4DB9914A0L, 0x1D9C9892400A22A3L,
            0xB54D5E4A127F59C8L, 0x2503BEB6D00CAB4CL, 0xE2A0B5DC971F303AL, 0x2E44AE64840FD61EL,
            0x8DA471A9DE737E24L, 0x5CEAECFED289E5D3L, 0xB10D8E1456105DADL, 0x7425A83E872C5F48L,
            0xDD50F1996B947518L, 0xD12F124E28F7771AL, 0x8A5296FFE33CC92FL, 0x82BD6B70D99AAA70L,
            0xACE73CBFDC0BFB7BL, 0x636CC64D1001550CL, 0xD8210BEFD30EFA5AL, 0x3C47F7E05401AA4FL,
            0x8714A775E3E95C78L, 0x65ACFAEC34810A72L, 0xA8D9D1535CE3B396L, 0x7F1839A741A14D0EL,
            0xD31045A8341CA07CL, 0x1EDE48111209A051L, 0x83EA2B892091E44DL, 0x934AED0AAB460433L,
            0xA4E4B66B68B65D60L, 0xF81DA84D56178540L, 0xCE1DE40642E3F4B9L, 0x36251260AB9D668FL,
            0x80D2AE83E9CE78F3L, 0xC1D72B7C6B42601AL, 0xA1075A24E4421730L, 0xB24CF65B8612F820L,
            0xC94930AE1D529CFCL, 0xDEE033F26797B628L, 0xFB9B7CD9A4A7443CL, 0x169840EF017DA3B2L,
            0x9D412E0806E88AA5L, 0x8E1F289560EE864FL, 0xC491798A08A2AD4EL, 0xF1A6F2BAB92A27E3L,
            0xF5B5D7EC8ACB58A2L, 0xAE10AF696774B1DCL, 0x9991A6F3D6BF1765L, 0xACCA6DA1E0A8EF2AL,
            0xBFF610B0CC6EDD3FL, 0x17FD090A58D32AF4L, 0xEFF394DCFF8A948EL, 0xDDFC4B4CEF07F5B1L,
            0x95F83D0A1FB69CD9L, 0x4ABDAF101564F98FL, 0xBB764C4CA7A4440FL, 0x9D6D1AD41ABE37F2L,
            0xEA53DF5FD18D5513L, 0x84C86189216DC5EEL, 0x92746B9BE2F8552CL, 0x32FD3CF5B4E49BB5L,
            0xB7118682DBB66A77L, 0x3FBC8C33221DC2A2L, 0xE4D5E82392A40515L, 0x0FABAF3FEAA5334BL,
            0x8F05B1163BA6832DL, 0x29CB4D87F2A7400FL, 0xB2C71D5BCA9023F8L, 0x743E20E9EF511013L,
            0xDF78E4B2BD342CF6L, 0x914DA9246B255417L, 0x8BAB8EEFB6409C1AL, 0x1AD089B6C2F7548FL,
            0xAE9672ABA3D0C320L, 0xA184AC2473B529B2L, 0xDA3C0F568CC4F3E8L, 0xC9E5D72D90A2741FL,
            0x8865899617FB1871L, 0x7E2FA67C7A658893L, 0xAA7EEBFB9DF9DE8DL, 0xDDBB901B98FEEAB8L,
            0xD51EA6FA85785631L, 0x552A74227F3EA566L, 0x8533285C936B35DEL, 0xD53A88958F872760L,
            0xA67FF273B8460356L, 0x8A892ABAF368F138L, 0xD01FEF10A657842CL, 0x2D2B7569B0432D86L,
            0x8213F56A67F6B29BL, 0x9C3B29620E29FC74L, 0xA298F2C501F45F42L, 0x8349F3BA91B47B90L,
            0xCB3F2F7642717713L, 0x241C70A936219A74L, 0xFE0EFB53D30DD4D7L, 0xED238CD383AA0111L,
            0x9EC95D1463E8A506L, 0xF4363804324A40ABL, 0xC67BB4597CE2CE48L, 0xB143C6053EDCD0D6L,
            0xF81AA16FDC1B81DAL, 0xDD94B7868E94050BL, 0x9B10A4E5E9913128L, 0xCA7CF2B4191C8327L,
            0xC1D4CE1F63F57D72L, 0xFD1C2F611F63A3F1L, 0xF24A01A73CF2DCCFL, 0xBC633B39673C8CEDL,
            0x976E41088617CA01L, 0xD5BE0503E085D814L, 0xBD49D14AA79DBC82L, 0x4B2D8644D8A74E19L,
            0xEC9C459D51852BA2L, 0xDDF8E7D60ED1219FL, 0x93E1AB8252F33B45L, 0xCABB90E5C942B504L,
            0xB8DA1662E7B00A17L, 0x3D6A751F3B936244L, 0xE7109BFBA19C0C9DL, 0x0CC512670A783AD5L,
            0x906A617D450187E2L, 0x27FB2B80668B24C6L, 0xB484F9DC9641E9DAL, 0xB1F9F660802DEDF7L,
            0xE1A63853BBD26451L, 0x5E7873F8A0396974L, 0x8D07E33455637EB2L, 0xDB0B487B6423E1E9L,
            0xB049DC016ABC5E5FL, 0x91CE1A9A3D2CDA63L, 0xDC5C5301C56B75F7L, 0x7641A140CC7810FCL,
            0x89B9B3E11B6329BAL, 0xA9E904C87FCB0A9EL, 0xAC2820D9623BF429L, 0x546345FA9FBDCD45L,
            0xD732290FBACAF133L, 0xA97C177947AD4096L, 0x867F59A9D4BED6C0L, 0x49ED8EABCCCC485EL,
            0xA81F301449EE8C70L, 0x5C68F256BFFF5A75L, 0xD226FC195C6A2F8CL, 0x73832EEC6FFF3112L,
            0x83585D8FD9C25DB7L, 0xC831FD53C5FF7EACL, 0xA42E74F3D032F525L, 0xBA3E7CA8B77F5E56L,
            0xCD3A1230C43FB26FL, 0x28CE1BD2E55F35ECL, 0x80444B5E7AA7CF85L, 0x7980D163CF5B81B4L,
            0xA0555E361951C366L, 0xD7E105BCC3326220L, 0xC86AB5C39FA63440L, 0x8DD9472BF3FEFAA8L,
            0xFA856334878FC150L, 0xB14F98F6F0FEB952L, 0x9C935E00D4B9D8D2L, 0x6ED1BF9A569F33D4L,
            0xC3B8358109E84F07L, 0x0A862F80EC4700C9L, 0xF4A642E14C6262C8L, 0xCD27BB612758C0FBL,
            0x98E7E9CCCFBD7DBDL, 0x8038D51CB897789DL, 0xBF21E44003ACDD2CL, 0xE0470A63E6BD56C4L,
            0xEEEA5D5004981478L, 0x1858CCFCE06CAC75L, 0x95527A5202DF0CCBL, 0x0F37801E0C43EBC9L,
            0xBAA718E68396CFFDL, 0xD30560258F54E6BBL, 0xE950DF20247C83FDL, 0x47C6B82EF32A206AL,
            0x91D28B7416CDD27EL, 0x4CDC331D57FA5442L, 0xB6472E511C81471DL, 0xE0133FE4ADF8E953L,
            0xE3D8F9E563A198E5L, 0x58180FDDD97723A7L, 0x8E679C2F5E44FF8FL, 0x570F09EAA7EA7649L,
            0xB201833B35D63F73L, 0x2CD2CC6551E513DBL, 0xDE81E40A034BCF4FL, 0xF8077F7EA65E58D2L,
            0x8B112E86420F6191L, 0xFB04AFAF27FAF783L, 0xADD57A27D29339F6L, 0x79C5DB9AF1F9B564L,
            0xD94AD8B1C7380874L, 0x18375281AE7822BDL, 0x87CEC76F1C830548L, 0x8F2293910D0B15B6L,
            0xA9C2794AE3A3C69AL, 0xB2EB3875504DDB23L, 0xD433179D9C8CB841L, 0x5FA60692A46151ECL,
            0x849FEEC281D7F328L, 0xDBC7C41BA6BCD334L, 0xA5C7EA73224DEFF3L, 0x12B9B522906C0801L,
            0xCF39E50FEAE16BEFL, 0xD768226B34870A01L, 0x81842F29F2CCE375L, 0xE6A1158300D46641L,
            0xA1E53AF46F801C53L, 0x60495AE3C1097FD1L, 0xCA5E89B18B602368L, 0x385BB19CB14BDFC5L,
            0xFCF62C1DEE382C42L, 0x46729E03DD9ED7B6L, 0x9E19DB92B4E31BA9L, 0x6C07A2C26A8346D2L
    };
}
