package com.fasterxml.jackson.core.io;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

public class NumberOutputTest
{
    @Test
    public void testDivBy1000Small()
    {
        for (int number = 0; number <= 999_999; ++number) {
            int expected = number / 1000;
            int actual = NumberOutput.divBy1000(number);
            if (expected != actual) { // only construct String if fail
                fail("With "+number+" should get "+expected+", got: "+actual);
            }
        }
    }

    @Test
    public void testDivBy1000Sampled()
    {
        for (int number = 1_000_000; number > 0; number += 7) {
            int expected = number / 1000;
            int actual = NumberOutput.divBy1000(number);
            if (expected != actual) { // only construct String if fail
                fail("With "+number+" should get "+expected+", got: "+actual);
            }
        }
    }

    // And then full range, not included in CI since code shouldn't change;
    // but has been run to verify full range manually
    @Test
    // Comment out for manual testing:
    @Disabled
    public void testDivBy1000FullRange() {
        for (int number = 0; number <= Integer.MAX_VALUE; ++number) {
            int expected = number / 1000;
            int actual = NumberOutput.divBy1000(number);
            if (expected != actual) { // only construct String if fail
                fail("With "+number+" should get "+expected+", got: "+actual);
            }
        }
    }
}