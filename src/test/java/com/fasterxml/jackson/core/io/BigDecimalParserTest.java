package com.fasterxml.jackson.core.io;

import java.math.BigDecimal;

public class BigDecimalParserTest extends com.fasterxml.jackson.core.BaseTest {
    public void testLongStringParse() {
        final int len = 1500;
        final StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append("A");
        }
        try {
            BigDecimalParser.parse(sb.toString());
            fail("expected NumberFormatException");
        } catch (NumberFormatException nfe) {
            assertTrue("exception message starts as expected?", nfe.getMessage().startsWith("Value \"AAAAA"));
            assertTrue("exception message value contains truncated", nfe.getMessage().contains("truncated"));
        }
    }

    public void testXParse() {
        assertEquals(new BigDecimal("123"), BigDecimalParser.xparse("123"));
        assertEquals(new BigDecimal("-123"), BigDecimalParser.xparse("-123"));
        assertEquals(new BigDecimal("123.456"), BigDecimalParser.xparse("123.456"));
    }
}
