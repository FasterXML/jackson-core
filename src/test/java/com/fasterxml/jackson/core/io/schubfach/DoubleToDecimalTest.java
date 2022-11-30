package com.fasterxml.jackson.core.io.schubfach;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static com.fasterxml.jackson.core.io.schubfach.DoubleToDecimalChecker.*;
import static java.lang.Double.longBitsToDouble;
import static java.lang.StrictMath.scalb;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DoubleToDecimalTest {
    @Test
    void testExtremeValues() {
        toDec(Double.NEGATIVE_INFINITY);
        toDec(-Double.MAX_VALUE);
        toDec(-Double.MIN_NORMAL);
        toDec(-Double.MIN_VALUE);
        toDec(-0.0);
        toDec(0.0);
        toDec(Double.MIN_VALUE);
        toDec(Double.MIN_NORMAL);
        toDec(Double.MAX_VALUE);
        toDec(Double.POSITIVE_INFINITY);
        toDec(Double.NaN);

        /*
        Quiet NaNs have the most significant bit of the mantissa as 1,
        while signaling NaNs have it as 0.
        Exercise 4 combinations of quiet/signaling NaNs and
        "positive/negative" NaNs
         */
        toDec(longBitsToDouble(0x7FF8_0000_0000_0001L));
        toDec(longBitsToDouble(0x7FF0_0000_0000_0001L));
        toDec(longBitsToDouble(0xFFF8_0000_0000_0001L));
        toDec(longBitsToDouble(0xFFF0_0000_0000_0001L));

        /*
        All values treated specially by Schubfach
         */
        for (int c = 1; c < C_TINY; ++c) {
            toDec(c * Double.MIN_VALUE);
        }
    }

    /*
    A few "powers of 10" are incorrectly rendered by the JDK.
    The rendering is either too long or it is not the closest decimal.
     */
    @Test
    void testPowersOf10() {
        for (int e = E_MIN; e <= E_MAX; ++e) {
            toDec(Double.parseDouble("1e" + e));
        }
    }

    /*
    Many powers of 2 are incorrectly rendered by the JDK.
    The rendering is either too long or it is not the closest decimal.
    */
    @Test
    void testPowersOf2() {
        for (double v = Double.MIN_VALUE; v <= Double.MAX_VALUE; v *= 2) {
            toDec(v);
        }
    }

    @Test
    void testSomeAnomalies() {
        for (String dec : Anomalies) {
            toDec(Double.parseDouble(dec));
        }
    }

    @Test
    void testPaxson() {
        for (int i = 0; i < PaxsonSignificands.length; ++i) {
            toDec(scalb(PaxsonSignificands[i], PaxsonExponents[i]));
        }
    }

    /*
    Tests all integers of the form yx_xxx_000_000_000_000_000, y != 0.
    These are all exact doubles.
     */
    @Test
    void testLongs() {
        for (int i = 10_000; i < 100_000; ++i) {
            toDec(i * 1e15);
        }
    }

    /*
    Tests all integers up to 1_000_000.
    These are all exact doubles and exercise a fast path.
     */
    @Test
    void testInts() {
        for (int i = 0; i <= 1_000_000; ++i) {
            toDec(i);
        }
    }

    @Test
    void testConstants() {
        assertEquals(DoubleToDecimal.P, P, "P");
        assertTrue((long) (double) C_MIN == C_MIN, "C_MIN");
        assertTrue((long) (double) C_MAX == C_MAX, "C_MAX");
        assertEquals(Double.MIN_VALUE, MIN_VALUE, "MIN_VALUE");
        assertEquals(Double.MIN_NORMAL, MIN_NORMAL, "MIN_NORMAL");
        assertEquals(Double.MAX_VALUE, MAX_VALUE, "MAX_VALUE");

        assertEquals(DoubleToDecimal.Q_MIN, Q_MIN, "Q_MIN");
        assertEquals(DoubleToDecimal.Q_MAX, Q_MAX, "Q_MAX");

        assertEquals(DoubleToDecimal.K_MIN, K_MIN, "K_MIN");
        assertEquals(DoubleToDecimal.K_MAX, K_MAX, "K_MAX");
        assertEquals(DoubleToDecimal.H, H, "H");

        assertEquals(DoubleToDecimal.E_MIN, E_MIN, "E_MIN");
        assertEquals(DoubleToDecimal.E_MAX, E_MAX, "E_MAX");
        assertEquals(DoubleToDecimal.C_TINY, C_TINY, "C_TINY");
    }

    @Test
    void testHardValues() {
        for (double v : hard0()) {
            toDec(v);
        }
        for (double v : hard1()) {
            toDec(v);
        }
        for (double v : hard2()) {
            toDec(v);
        }
        for (double v : hard3()) {
            toDec(v);
        }
    }

    @Test
    void randomNumberTests() {
        // 29-Nov-2022, tatu: Reduce from 1M due to slowness
        DoubleToDecimalChecker.randomNumberTests(250_000, new Random());
    }
}
