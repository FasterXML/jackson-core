package com.fasterxml.jackson.core.io;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.fail;

public class NumberOutputTest {
    @Test
    // Comment out for manual testing:
    @Ignore
    public void testDivBy1000() {
        for (int i = 0; i <= Integer.MAX_VALUE; i++) {
            int number = (int) i;
            int expected = number / 1000;
            int actual = NumberOutput.divBy1000(number);
            if (expected != actual) { // only construct String if fail
                fail("With "+number+" should get "+expected+", got: "+actual);
            }
        }
    }
}