package com.fasterxml.jackson.core.io;

import java.math.BigDecimal;

import com.fasterxml.jackson.core.TestBase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BigDecimalParserTest extends TestBase
{
    @Test
    public void testLongInvalidStringParse() {
        try {
            BigDecimalParser.parse(genLongInvalidString());
            fail("expected NumberFormatException");
        } catch (NumberFormatException nfe) {
            assertTrue(nfe.getMessage().startsWith("Value \"AAAAA"), "exception message starts as expected?");
            assertTrue(nfe.getMessage().contains("truncated"), "exception message value contains truncated");
        }
    }

    @Test
    public void testLongInvalidStringFastParse() {
        try {
            BigDecimalParser.parseWithFastParser(genLongInvalidString());
            fail("expected NumberFormatException");
        } catch (NumberFormatException nfe) {
            assertTrue(nfe.getMessage().startsWith("Value \"AAAAA"), "exception message starts as expected?");
            assertTrue(nfe.getMessage().contains("truncated"), "exception message value contains truncated");
        }
    }

    @Test
    public void testLongValidStringParse() {
        String num = genLongValidString(500);
        final BigDecimal EXP = new BigDecimal(num);

        // Parse from String first, then char[]

        assertEquals(EXP, BigDecimalParser.parse(num));
        assertEquals(EXP, BigDecimalParser.parse(num.toCharArray(), 0, num.length()));
    }

    @Test
    public void testLongValidStringFastParse() {
        String num = genLongValidString(500);
        final BigDecimal EXP = new BigDecimal(num);

        // Parse from String first, then char[]
        assertEquals(EXP, BigDecimalParser.parseWithFastParser(num));
        assertEquals(EXP, BigDecimalParser.parseWithFastParser(num.toCharArray(), 0, num.length()));
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
