package com.fasterxml.jackson.core.io;

import java.math.BigDecimal;

import ch.randelshofer.fastdoubleparser.JavaBigDecimalParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BigDecimalParserTest extends com.fasterxml.jackson.core.JUnit5TestBase
{
    @Test
    void longInvalidStringParse() {
        try {
            BigDecimalParser.parse(genLongInvalidString());
            fail("expected NumberFormatException");
        } catch (NumberFormatException nfe) {
            assertTrue(nfe.getMessage().startsWith("Value \"AAAAA"), "exception message starts as expected?");
            assertTrue(nfe.getMessage().contains("truncated"), "exception message value contains truncated");
        }
    }

    @Test
    void longInvalidStringFastParse() {
        try {
            BigDecimalParser.parseWithFastParser(genLongInvalidString());
            fail("expected NumberFormatException");
        } catch (NumberFormatException nfe) {
            assertTrue(nfe.getMessage().startsWith("Value \"AAAAA"), "exception message starts as expected?");
            assertTrue(nfe.getMessage().contains("truncated"), "exception message value contains truncated");
        }
    }

    @Test
    void longValidStringParse() {
        String num = genLongValidString(500);
        final BigDecimal EXP = new BigDecimal(num);

        // Parse from String first, then char[]

        assertEquals(EXP, BigDecimalParser.parse(num));
        assertEquals(EXP, BigDecimalParser.parse(num.toCharArray(), 0, num.length()));
    }

    @Test
    void longValidStringFastParse() {
        String num = genLongValidString(500);
        final BigDecimal EXP = new BigDecimal(num);

        // Parse from String first, then char[]
        assertEquals(EXP, BigDecimalParser.parseWithFastParser(num));
        assertEquals(EXP, BigDecimalParser.parseWithFastParser(num.toCharArray(), 0, num.length()));
    }

    @Test
    void issueDatabind4694() {
        final String str = "-11000.0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";
        final BigDecimal expected = new BigDecimal(str);
        assertEquals(expected, JavaBigDecimalParser.parseBigDecimal(str));
        assertEquals(expected, BigDecimalParser.parse(str));
        assertEquals(expected, BigDecimalParser.parseWithFastParser(str));
        final char[] arr = str.toCharArray();
        assertEquals(expected, BigDecimalParser.parse(arr, 0, arr.length));
        assertEquals(expected, BigDecimalParser.parseWithFastParser(arr, 0, arr.length));
    }

    static String genLongInvalidString() {
        final int len = 1500;
        final StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append("A");
        }
        return sb.toString();
    }

    static String genLongValidString(int len) {
        final StringBuilder sb = new StringBuilder(len+5);
        sb.append("0.");
        for (int i = 0; i < len; i++) {
            sb.append('0');
        }
        sb.append('1');
        return sb.toString();
    }
}
