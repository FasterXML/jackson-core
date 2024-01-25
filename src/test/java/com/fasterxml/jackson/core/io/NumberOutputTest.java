package com.fasterxml.jackson.core.io;

import com.fasterxml.jackson.core.BaseTest;
import org.junit.Ignore;

public class NumberOutputTest extends BaseTest {
    @Ignore
    public void testDivBy1000() {
        for (long i = 0; i <= Integer.MAX_VALUE; i++) {
            int number = (int) i;
            int expected = number / 1000;
            int actual = NumberOutput.divBy1000(number);
            assertEquals(expected, actual, number);
        }
    }
}