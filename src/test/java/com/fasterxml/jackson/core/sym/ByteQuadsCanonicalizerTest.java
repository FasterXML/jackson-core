package com.fasterxml.jackson.core.sym;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ByteQuadsCanonicalizerTest {
    @Test
    @Ignore
    public void testMultiplyByFourFifths() {
        for (long i = 0; i <= Integer.MAX_VALUE; i++) {
            int number = (int) i;
            int expected = (int) (number * 0.80);
            int actual = ByteQuadsCanonicalizer.multiplyByFourFifths(number);
            assertEquals("input=" + number, expected, actual);
        }
    }
}