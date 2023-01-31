package com.fasterxml.jackson.core.io;

public class BigIntegerParserTest extends com.fasterxml.jackson.core.BaseTest {

    public void testLongStringFastParseBigInteger() {
        try {
            BigIntegerParser.parseWithFastParser(genLongString());
            fail("expected NumberFormatException");
        } catch (NumberFormatException nfe) {
            assertTrue("exception message starts as expected?", nfe.getMessage().startsWith("Value \"AAAAA"));
            assertTrue("exception message value contains: truncated", nfe.getMessage().contains("truncated"));
            assertTrue("exception message value contains: BigInteger", nfe.getMessage().contains("BigInteger"));
        }
    }

    public void testLongStringFastParseBigIntegerRadix() {
        try {
            BigIntegerParser.parseWithFastParser(genLongString(), 8);
            fail("expected NumberFormatException");
        } catch (NumberFormatException nfe) {
            assertTrue("exception message starts as expected?", nfe.getMessage().startsWith("Value \"AAAAA"));
            assertTrue("exception message value contains: truncated", nfe.getMessage().contains("truncated"));
            assertTrue("exception message value contains: radix 8", nfe.getMessage().contains("radix 8"));
            assertTrue("exception message value contains: BigInteger", nfe.getMessage().contains("BigInteger"));
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
