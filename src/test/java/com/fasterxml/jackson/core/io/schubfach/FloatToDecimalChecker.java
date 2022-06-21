/*
 * Copyright 2018-2020 Raffaello Giulietti
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.fasterxml.jackson.core.io.schubfach;

import java.math.BigDecimal;
import java.util.Random;

import static java.lang.Float.*;
import static java.lang.Integer.numberOfTrailingZeros;
import static java.lang.StrictMath.scalb;
import static com.fasterxml.jackson.core.io.schubfach.MathUtils.flog10pow2;

public class FloatToDecimalChecker extends ToDecimalChecker {

    static final int P =
            numberOfTrailingZeros(floatToRawIntBits(3)) + 2;
    private static final int W = (SIZE - 1) - (P - 1);
    static final int Q_MIN = (-1 << W - 1) - P + 3;
    static final int Q_MAX = (1 << W - 1) - P;
    static final int C_MIN = 1 << P - 1;
    static final int C_MAX = (1 << P) - 1;

    static final int K_MIN = flog10pow2(Q_MIN);
    static final int K_MAX = flog10pow2(Q_MAX);
    static final int H = flog10pow2(P) + 2;

    static final float MIN_VALUE = scalb(1.0f, Q_MIN);
    static final float MIN_NORMAL = scalb((float) C_MIN, Q_MIN);
    static final float MAX_VALUE = scalb((float) C_MAX, Q_MAX);

    static final int E_MIN = e(MIN_VALUE);
    static final int E_MAX = e(MAX_VALUE);

    static final long C_TINY = cTiny(Q_MIN, K_MIN);

    private float v;
    private final int originalBits;

    private FloatToDecimalChecker(float v, String s) {
        super(s);
        this.v = v;
        originalBits = floatToRawIntBits(v);
    }

    @Override
    BigDecimal toBigDecimal() {
        return new BigDecimal(v);
    }

    @Override
    boolean recovers(BigDecimal b) {
        return b.floatValue() == v;
    }

    @Override
    String hexBits() {
        return String.format("0x%01X__%02X__%02X_%04X",
                (originalBits >>> 31) & 0x1,
                (originalBits >>> 23) & 0xFF,
                (originalBits >>> 16) & 0x7F,
                originalBits & 0xFFFF);
    }

    @Override
    boolean recovers(String s) {
        return parseFloat(s) == v;
    }

    @Override
    int minExp() {
        return E_MIN;
    }

    @Override
    int maxExp() {
        return E_MAX;
    }

    @Override
    int maxLen10() {
        return H;
    }

    @Override
    boolean isZero() {
        return v == 0;
    }

    @Override
    boolean isInfinity() {
        return v == POSITIVE_INFINITY;
    }

    @Override
    void negate() {
        v = -v;
    }

    @Override
    boolean isNegative() {
        return originalBits < 0;
    }

    @Override
    boolean isNaN() {
        return Float.isNaN(v);
    }

    static void toDec(float v) {
//        String s = Float.toString(v);
        String s = FloatToDecimal.toString(v);
        new FloatToDecimalChecker(v, s).validate();
    }

    /*
    There are tons of doubles that are rendered incorrectly by the JDK.
    While the renderings correctly round back to the original value,
    they are longer than needed or are not the closest decimal to the double.
    Here are just a very few examples.
     */
    static final String[] Anomalies = {
            // JDK renders these longer than needed.
            "1.1754944E-38", "2.2E-44",
            "1.0E16", "2.0E16", "3.0E16", "5.0E16", "3.0E17",
            "3.2E18", "3.7E18", "3.7E16", "3.72E17",

            // JDK does not render this as the closest.
            "9.9E-44",
    };

    /*
    Values are from
    Paxson V, "A Program for Testing IEEE Decimal-Binary Conversion"
    tables 16 and 17
     */
    static final float[] PaxsonSignificands = {
            12_676_506,
            15_445_013,
            13_734_123,
            12_428_269,
            12_676_506,
            15_334_037,
            11_518_287,
            12_584_953,
            15_961_084,
            14_915_817,
            10_845_484,
            16_431_059,

            16_093_626,
            9_983_778,
            12_745_034,
            12_706_553,
            11_005_028,
            15_059_547,
            16_015_691,
            8_667_859,
            14_855_922,
            14_855_922,
            10_144_164,
            13_248_074,
    };

    static final int[] PaxsonExponents = {
            -102,
            -103,
            86,
            -138,
            -130,
            -146,
            -41,
            -145,
            -125,
            -146,
            -102,
            -61,

            69,
            25,
            104,
            72,
            45,
            71,
            -99,
            56,
            -82,
            -83,
            -110,
            95,
    };

    /*
    Random floats over the whole range.
     */
    private static void testRandom(int randomCount, Random r) {
        for (int i = 0; i < randomCount; ++i) {
            toDec(intBitsToFloat(r.nextInt()));
        }
    }

    /*
    All, really all, 2^32 possible floats. Takes between 90 and 120 minutes.
     */
    public static void testAll() {
        // Avoid wrapping around Integer.MAX_VALUE
        int bits = Integer.MIN_VALUE;
        for (; bits < Integer.MAX_VALUE; ++bits) {
            toDec(intBitsToFloat(bits));
        }
        toDec(intBitsToFloat(bits));
    }

    /*
    All positive 2^31 floats.
     */
    public static void testPositive() {
        // Avoid wrapping around Integer.MAX_VALUE
        int bits = 0;
        for (; bits < Integer.MAX_VALUE; ++bits) {
            toDec(intBitsToFloat(bits));
        }
        toDec(intBitsToFloat(bits));
    }

    public static void randomNumberTests(int randomCount, Random r) {
        testRandom(randomCount, r);
    }
}
