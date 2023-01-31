package com.fasterxml.jackson.core.io;

public class BigDecimalParserTest extends com.fasterxml.jackson.core.BaseTest {
    public void testLongStringParse() {
        try {
            BigDecimalParser.parse(genLongString());
            fail("expected NumberFormatException");
        } catch (NumberFormatException nfe) {
            assertTrue("exception message starts as expected?", nfe.getMessage().startsWith("Value \"AAAAA"));
            assertTrue("exception message value contains truncated", nfe.getMessage().contains("truncated"));
        }
    }

    public void testLongStringFastParse() {
        try {
            BigDecimalParser.parseWithFastParser(genLongString());
            fail("expected NumberFormatException");
        } catch (NumberFormatException nfe) {
            assertTrue("exception message starts as expected?", nfe.getMessage().startsWith("Value \"AAAAA"));
            assertTrue("exception message value contains truncated", nfe.getMessage().contains("truncated"));
        }
    }

    private String genLongString() {
        return BigIntegerParserTest.genLongString();
    }
}
