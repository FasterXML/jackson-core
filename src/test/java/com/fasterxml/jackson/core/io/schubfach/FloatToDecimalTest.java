package com.fasterxml.jackson.core.io.schubfach;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static com.fasterxml.jackson.core.io.schubfach.FloatToDecimalChecker.*;
import static java.lang.Float.intBitsToFloat;
import static java.lang.StrictMath.scalb;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FloatToDecimalTest {
    /*
    MIN_NORMAL is incorrectly rendered by the JDK.
    */
    @Test
    void testExtremeValues() {
        toDec(Float.NEGATIVE_INFINITY);
        toDec(-Float.MAX_VALUE);
        toDec(-Float.MIN_NORMAL);
        toDec(-Float.MIN_VALUE);
        toDec(-0.0f);
        toDec(0.0f);
        toDec(Float.MIN_VALUE);
        toDec(Float.MIN_NORMAL);
        toDec(Float.MAX_VALUE);
        toDec(Float.POSITIVE_INFINITY);
        toDec(Float.NaN);

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
            toDec(c * Float.MIN_VALUE);
        }
    }

    /*
    Some "powers of 10" are incorrectly rendered by the JDK.
    The rendering is either too long or it is not the closest decimal.
     */
    @Test
    void testPowersOf10() {
        for (int e = E_MIN; e <= E_MAX; ++e) {
            toDec(Float.parseFloat("1e" + e));
        }
    }

    /*
    Many powers of 2 are incorrectly rendered by the JDK.
    The rendering is either too long or it is not the closest decimal.
     */
    @Test
    void testPowersOf2() {
        for (float v = Float.MIN_VALUE; v <= Float.MAX_VALUE; v *= 2) {
            toDec(v);
        }
    }

    @Test
    void testConstants() {
        assertEquals(FloatToDecimal.P, P, "P");
        assertTrue((long) (float) C_MIN == C_MIN, "C_MIN");
        assertTrue((long) (float) C_MAX == C_MAX, "C_MAX");
        assertEquals(Float.MIN_VALUE, MIN_VALUE, "MIN_VALUE");
        assertEquals(Float.MIN_NORMAL, MIN_NORMAL, "MIN_NORMAL");
        assertEquals(Float.MAX_VALUE, MAX_VALUE, "MAX_VALUE");

        assertEquals(FloatToDecimal.Q_MIN, Q_MIN, "Q_MIN");
        assertEquals(FloatToDecimal.Q_MAX, Q_MAX, "Q_MAX");

        assertEquals(FloatToDecimal.K_MIN, K_MIN, "K_MIN");
        assertEquals(FloatToDecimal.K_MAX, K_MAX, "K_MAX");
        assertEquals(FloatToDecimal.H, H, "H");

        assertEquals(FloatToDecimal.E_MIN, E_MIN, "E_MIN");
        assertEquals(FloatToDecimal.E_MAX, E_MAX, "E_MAX");
        assertEquals(FloatToDecimal.C_TINY, C_TINY, "C_TINY");
    }

    @Test
    void testSomeAnomalies() {
        for (String dec : Anomalies) {
            toDec(Float.parseFloat(dec));
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
        // 29-Nov-2022, tatu: Reduce from original due to slowness
        // for (int i = 1; i < 1 << P - 1; ++i) {
        for (int i = 1; i < 1 << P - 1; i += 3) {
            toDec(i);
        }
    }

    @Test
    void randomNumberTests() {
        FloatToDecimalChecker.randomNumberTests(1_000_000, new Random());
    }
}
