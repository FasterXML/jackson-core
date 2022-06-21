package com.fasterxml.jackson.core.io.schubfach;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static com.fasterxml.jackson.core.io.schubfach.FloatToDecimalChecker.*;
import static java.lang.Float.*;
import static java.lang.StrictMath.scalb;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FloatToDecimalTest {
    /*
    MIN_NORMAL is incorrectly rendered by the JDK.
    */
    @Test
    void testExtremeValues() {
        toDec(NEGATIVE_INFINITY);
        toDec(-MAX_VALUE);
        toDec(-MIN_NORMAL);
        toDec(-MIN_VALUE);
        toDec(-0.0f);
        toDec(0.0f);
        toDec(MIN_VALUE);
        toDec(MIN_NORMAL);
        toDec(MAX_VALUE);
        toDec(POSITIVE_INFINITY);
        toDec(NaN);

        /*
        Quiet NaNs have the most significant bit of the mantissa as 1,
        while signaling NaNs have it as 0.
        Exercise 4 combinations of quiet/signaling NaNs and
        "positive/negative" NaNs.
         */
        toDec(intBitsToFloat(0x7FC0_0001));
        toDec(intBitsToFloat(0x7F80_0001));
        toDec(intBitsToFloat(0xFFC0_0001));
        toDec(intBitsToFloat(0xFF80_0001));

        /*
        All values treated specially by Schubfach
         */
        for (int c = 1; c < C_TINY; ++c) {
            toDec(c * MIN_VALUE);
        }
    }

    /*
    Some "powers of 10" are incorrectly rendered by the JDK.
    The rendering is either too long or it is not the closest decimal.
     */
    @Test
    void testPowersOf10() {
        for (int e = E_MIN; e <= E_MAX; ++e) {
            toDec(parseFloat("1e" + e));
        }
    }

    /*
    Many powers of 2 are incorrectly rendered by the JDK.
    The rendering is either too long or it is not the closest decimal.
     */
    @Test
    void testPowersOf2() {
        for (float v = MIN_VALUE; v <= MAX_VALUE; v *= 2) {
            toDec(v);
        }
    }

    @Test
    void testConstants() {
        assertTrue(P == FloatToDecimal.P, "P");
        assertTrue((long) (float) C_MIN == C_MIN, "C_MIN");
        assertTrue((long) (float) C_MAX == C_MAX, "C_MAX");
        assertTrue(MIN_VALUE == Float.MIN_VALUE, "MIN_VALUE");
        assertTrue(MIN_NORMAL == Float.MIN_NORMAL, "MIN_NORMAL");
        assertTrue(MAX_VALUE == Float.MAX_VALUE, "MAX_VALUE");

        assertTrue(Q_MIN == FloatToDecimal.Q_MIN, "Q_MIN");
        assertTrue(Q_MAX == FloatToDecimal.Q_MAX, "Q_MAX");

        assertTrue(K_MIN == FloatToDecimal.K_MIN, "K_MIN");
        assertTrue(K_MAX == FloatToDecimal.K_MAX, "K_MAX");
        assertTrue(H == FloatToDecimal.H, "H");

        assertTrue(E_MIN == FloatToDecimal.E_MIN, "E_MIN");
        assertTrue(E_MAX == FloatToDecimal.E_MAX, "E_MAX");
        assertTrue(C_TINY == FloatToDecimal.C_TINY, "C_TINY");
    }

    @Test
    void testSomeAnomalies() {
        for (String dec : Anomalies) {
            toDec(parseFloat(dec));
        }
    }

    @Test
    void testPaxson() {
        for (int i = 0; i < PaxsonSignificands.length; ++i) {
            toDec(scalb(PaxsonSignificands[i], PaxsonExponents[i]));
        }
    }

    /*
    Tests all positive integers below 2^23.
    These are all exact floats and exercise the fast path.
     */
    @Test
    void testInts() {
        for (int i = 1; i < 1 << P - 1; ++i) {
            toDec(i);
        }
    }

    @Test
    void randomNumberTests() {
        FloatToDecimalChecker.randomNumberTests(1_000_000, new Random());
    }
}
