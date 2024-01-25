package com.fasterxml.jackson.core.io;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NumberOutputTest {
    @Test
    @Ignore
    public void testDivBy1000() {
        for (long i = 0; i <= Integer.MAX_VALUE; i++) {
            int number = (int) i;
            int expected = number / 1000;
            int actual = NumberOutput.divBy1000(number);
            assertEquals("input=" + number, expected, actual);
        }
    }
}