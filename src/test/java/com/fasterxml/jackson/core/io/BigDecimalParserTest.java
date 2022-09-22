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
        testXParse("123");
        testXParse("-123");
        testXParse("123.456");
        testXParse("-123.456");
        testXParse("12345678900987654321");
        testXParse("-12345678900987654321");
        testXParse("1234567890.0987654321");
        testXParse("-1234567890.0987654321");
        testXParse("1E+3");
        testXParse("1E-3");
        testXParse("-1E+3");
        testXParse("-1E-3");
    }

    private void testXParse(String s) {
        assertEquals(new BigDecimal(s), BigDecimalParser.xparse(s));
    }
}
