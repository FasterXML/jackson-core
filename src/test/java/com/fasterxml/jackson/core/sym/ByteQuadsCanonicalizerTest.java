package com.fasterxml.jackson.core.sym;

import com.fasterxml.jackson.core.BaseTest;
import org.junit.Ignore;

public class ByteQuadsCanonicalizerTest extends BaseTest {
    @Ignore
    public void testMultiplyByFourFifths() {
        for (long i = 0; i <= Integer.MAX_VALUE; i++) {
            int number = (int) i;
            int expected = (int) (number * 0.80);
            int actual = ByteQuadsCanonicalizer.multiplyByFourFifths(number);
            assertEquals(expected, actual, number);
        }
    }
}