package com.fasterxml.jackson.core.io.schubfach;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static com.fasterxml.jackson.core.io.schubfach.DoubleToDecimalChecker.*;
import static java.lang.Double.*;
import static java.lang.StrictMath.scalb;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DoubleToDecimalTest {
    @Test
    void testExtremeValues() {
        toDec(NEGATIVE_INFINITY);
        toDec(-MAX_VALUE);
        toDec(-MIN_NORMAL);
        toDec(-MIN_VALUE);
        toDec(-0.0);
        toDec(0.0);
        toDec(MIN_VALUE);
        toDec(MIN_NORMAL);
        toDec(MAX_VALUE);
        toDec(POSITIVE_INFINITY);
        toDec(NaN);

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
            toDec(c * MIN_VALUE);
        }
    }

    /*
    A few "powers of 10" are incorrectly rendered by the JDK.
    The rendering is either too long or it is not the closest decimal.
     */
    @Test
    void testPowersOf10() {
        for (int e = E_MIN; e <= E_MAX; ++e) {
            toDec(parseDouble("1e" + e));
        }
    }

    /*
    Many powers of 2 are incorrectly rendered by the JDK.
    The rendering is either too long or it is not the closest decimal.
    */
    @Test
    void testPowersOf2() {
        for (double v = MIN_VALUE; v <= MAX_VALUE; v *= 2) {
            toDec(v);
        }
    }

    @Test
    void testSomeAnomalies() {
        for (String dec : Anomalies) {
            toDec(parseDouble(dec));
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
        assertTrue(P == DoubleToDecimal.P, "P");
        assertTrue((long) (double) C_MIN == C_MIN, "C_MIN");
        assertTrue((long) (double) C_MAX == C_MAX, "C_MAX");
        assertTrue(MIN_VALUE == Double.MIN_VALUE, "MIN_VALUE");
        assertTrue(MIN_NORMAL == Double.MIN_NORMAL, "MIN_NORMAL");
        assertTrue(MAX_VALUE == Double.MAX_VALUE, "MAX_VALUE");

        assertTrue(Q_MIN == DoubleToDecimal.Q_MIN, "Q_MIN");
        assertTrue(Q_MAX == DoubleToDecimal.Q_MAX, "Q_MAX");

        assertTrue(K_MIN == DoubleToDecimal.K_MIN, "K_MIN");
        assertTrue(K_MAX == DoubleToDecimal.K_MAX, "K_MAX");
        assertTrue(H == DoubleToDecimal.H, "H");

        assertTrue(E_MIN == DoubleToDecimal.E_MIN, "E_MIN");
        assertTrue(E_MAX == DoubleToDecimal.E_MAX, "E_MAX");
        assertTrue(C_TINY == DoubleToDecimal.C_TINY, "C_TINY");
    }

    @Test
    void randomNumberTests() {
        DoubleToDecimalChecker.randomNumberTests(1_000_000, new Random());
    }
}
