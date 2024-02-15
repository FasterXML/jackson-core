package com.fasterxml.jackson.core.sym;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.fail;

public class ByteQuadsCanonicalizerTest
{
    @Test
    public void testMultiplyByFourFifths()
    {
        int i = 0;
        for (; i >= 0; i += 7) {
            int expected = (int) (i * 0.80);
            int actual = ByteQuadsCanonicalizer.multiplyByFourFifths(i);
            if (expected != actual) {
                fail("Input for 80% of "+i+" differs: expected="+expected+", actual="+actual);
            }
        }
    }
}