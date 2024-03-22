package com.fasterxml.jackson.core.io;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class BigIntegerParserTest extends com.fasterxml.jackson.core.JUnit5TestBase {

    @Test
    void fastParseBigIntegerFailsWithENotation() {
        String num = "2e308";
        try {
            BigIntegerParser.parseWithFastParser(num);
            fail("expected NumberFormatException");
        } catch (NumberFormatException nfe) {
            // expected
        }
    }

    @Test
    void longStringFastParseBigInteger() {
        try {
            BigIntegerParser.parseWithFastParser(genLongString());
            fail("expected NumberFormatException");
        } catch (NumberFormatException nfe) {
            assertTrue(nfe.getMessage().startsWith("Value \"AAAAA"), "exception message starts as expected?");
            assertTrue(nfe.getMessage().contains("truncated"), "exception message value contains: truncated");
            assertTrue(nfe.getMessage().contains("BigInteger"), "exception message value contains: BigInteger");
        }
    }

    @Test
    void longStringFastParseBigIntegerRadix() {
        try {
            BigIntegerParser.parseWithFastParser(genLongString(), 8);
            fail("expected NumberFormatException");
        } catch (NumberFormatException nfe) {
            assertTrue(nfe.getMessage().startsWith("Value \"AAAAA"), "exception message starts as expected?");
            assertTrue(nfe.getMessage().contains("truncated"), "exception message value contains: truncated");
            assertTrue(nfe.getMessage().contains("radix 8"), "exception message value contains: radix 8");
            assertTrue(nfe.getMessage().contains("BigInteger"), "exception message value contains: BigInteger");
        }
    }

    static String genLongString() {
        final int len = 1500;
        final StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append("A");
        }
        return sb.toString();
    }
}
