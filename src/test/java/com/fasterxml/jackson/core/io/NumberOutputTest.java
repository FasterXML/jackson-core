package com.fasterxml.jackson.core.io;

import java.util.Random;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class NumberOutputTest
{
    @Test
    void intPrinting() throws Exception
    {
        assertIntPrint(0);
        assertIntPrint(-3);
        assertIntPrint(1234);
        assertIntPrint(-1234);
        assertIntPrint(56789);
        assertIntPrint(-56789);
        assertIntPrint(999999);
        assertIntPrint(-999999);
        assertIntPrint(1000000);
        assertIntPrint(-1000000);
        assertIntPrint(10000001);
        assertIntPrint(-10000001);
        assertIntPrint(-100000012);
        assertIntPrint(100000012);
        assertIntPrint(1999888777);
        assertIntPrint(-1999888777);
        assertIntPrint(Integer.MAX_VALUE);
        assertIntPrint(Integer.MIN_VALUE);

        Random rnd = new Random(12345L);
        for (int i = 0; i < 251000; ++i) {
            assertIntPrint(rnd.nextInt());
        }
    }

    @Test
    void longPrinting() throws Exception
    {
        // First, let's just cover couple of edge cases
        assertLongPrint(0L, 0);
        assertLongPrint(1L, 0);
        assertLongPrint(-1L, 0);
        assertLongPrint(Long.MAX_VALUE, 0);
        assertLongPrint(Long.MIN_VALUE, 0);
        assertLongPrint(Long.MAX_VALUE-1L, 0);
        assertLongPrint(Long.MIN_VALUE+1L, 0);

        Random rnd = new Random(12345L);
        // Bigger value space, need more iterations for long
        for (int i = 0; i < 678000; ++i) {
            long l = ((long) rnd.nextInt() << 32) | rnd.nextInt();
            assertLongPrint(l, i);
        }
    }

    // // // Tests for divBy1000
    
    @Test
    void divBy1000Small()
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
    void divBy1000Sampled()
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
    void divBy1000FullRange() {
        // To get to Integer.MAX_VALUE, need to check for overflow
        for (int number = 0; number >= 0; ++number) {
            int expected = number / 1000;
            int actual = NumberOutput.divBy1000(number);
            if (expected != actual) { // only construct String if fail
                fail("With "+number+" should get "+expected+", got: "+actual);
            }
        }
    }

    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */

    private void assertIntPrint(int value)
    {
        String exp = ""+value;
        String act = printToString(value);

        if (!exp.equals(act)) {
            assertEquals(exp, act, "Expected conversion (exp '"+exp+"', len "+exp.length()+"; act len "+act.length()+")");
        }
        String alt = NumberOutput.toString(value);
        if (!exp.equals(alt)) {
            assertEquals(exp, act, "Expected conversion (exp '"+exp+"', len "+exp.length()+"; act len "+act.length()+")");
        }
    }

    private void assertLongPrint(long value, int index)
    {
        String exp = ""+value;
        String act = printToString(value);

        if (!exp.equals(act)) {
            assertEquals(exp, act, "Expected conversion (exp '"+exp+"', len "+exp.length()+"; act len "+act.length()+"; number index "+index+")");
        }
        String alt = NumberOutput.toString(value);
        if (!exp.equals(alt)) {
            assertEquals(exp, act, "Expected conversion (exp '"+exp+"', len "+exp.length()+"; act len "+act.length()+"; number index "+index+")");
        }
    }

    private String printToString(int value)
    {
        char[] buffer = new char[12];
        int offset = NumberOutput.outputInt(value, buffer, 0);
        return new String(buffer, 0, offset);
    }

    private String printToString(long value)
    {
        char[] buffer = new char[22];
        int offset = NumberOutput.outputLong(value, buffer, 0);
        return new String(buffer, 0, offset);
    }
}