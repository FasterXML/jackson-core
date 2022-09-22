package com.fasterxml.jackson.core.io;

import java.math.BigDecimal;

public class FastBigDecimalParserTest extends com.fasterxml.jackson.core.BaseTest {

    public void testParse() {
        testParse("123");
        testParse("-123");
        testParse("123.456");
        testParse("-123.456");
        testParse("12345678900987654321");
        testParse("-12345678900987654321");
        testParse("1234567890.0987654321");
        testParse("-1234567890.0987654321");
        testParse("1E+3");
        testParse("1E-3");
        testParse("-1E+3");
        testParse("-1E-3");
    }

    private void testParse(String s) {
        assertEquals(new BigDecimal(s), FastBigDecimalParser.parse(s));
    }
}
