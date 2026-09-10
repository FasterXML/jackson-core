package com.fasterxml.jackson.core.io;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.*;

class NumberInputTest
    extends com.fasterxml.jackson.core.JUnit5TestBase
{
    @Test
    void nastySmallDouble()
    {
        //relates to https://github.com/FasterXML/jackson-core/issues/750
        //prior to jackson v2.14, this value used to be returned as Double.MIN_VALUE
        final String nastySmallDouble = "2.2250738585072012e-308";
        assertEquals(Double.parseDouble(nastySmallDouble), NumberInput.parseDouble(nastySmallDouble, false));
        assertEquals(Double.parseDouble(nastySmallDouble), NumberInput.parseDouble(nastySmallDouble, true));
    }

    @Test
    void parseFloat()
    {
        final String exampleFloat = "1.199999988079071";
        assertEquals(1.1999999f, NumberInput.parseFloat(exampleFloat, false));
        assertEquals(1.1999999f, NumberInput.parseFloat(exampleFloat, true));
        assertEquals(1.2f, (float)NumberInput.parseDouble(exampleFloat, false));
        assertEquals(1.2f, (float)NumberInput.parseDouble(exampleFloat, true));

        final String exampleFloat2 = "7.006492321624086e-46";
        assertEquals("1.4E-45", Float.toString(NumberInput.parseFloat(exampleFloat2, false)));
        assertEquals("1.4E-45", Float.toString(NumberInput.parseFloat(exampleFloat2, true)));
    }

    @Test
    void parseLongBigInteger()
    {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            stringBuilder.append(7);
        }
        String test1000 = stringBuilder.toString();
        assertEquals(new BigInteger(test1000), NumberInput.parseBigInteger(test1000, false));
        assertEquals(new BigInteger(test1000), NumberInput.parseBigInteger(test1000, true));
        for (int i = 0; i < 1000; i++) {
            stringBuilder.append(7);
        }
        String test2000 = stringBuilder.toString();
        assertEquals(new BigInteger(test2000), NumberInput.parseBigInteger(test2000, false));
        assertEquals(new BigInteger(test2000), NumberInput.parseBigInteger(test2000, true));
    }

    @Test
    void bigIntegerWithRadix()
    {
        final String val = "1ABCDEF";
        final int radix = 16;
        BigInteger expected = new BigInteger(val, radix);
        assertEquals(expected, NumberInput.parseBigIntegerWithRadix(val, radix, true));
        assertEquals(expected, NumberInput.parseBigIntegerWithRadix(val, radix, false));
    }

    @Test
    void parseBigIntegerFailsWithENotation()
    {
        try {
            NumberInput.parseBigInteger("1e10", false);
            fail("expected NumberFormatException");
        } catch (NumberFormatException e) {
            verifyException(e, "1e10");
        }
    }

    @Test
    void looksLikeValidNumberTrue()
    {
        assertTrue(NumberInput.looksLikeValidNumber("0"));
        assertTrue(NumberInput.looksLikeValidNumber("1"));
        assertTrue(NumberInput.looksLikeValidNumber("-1"));
        assertTrue(NumberInput.looksLikeValidNumber("+1")); // non-JSON
        assertTrue(NumberInput.looksLikeValidNumber("0001")); // non-JSON

        // https://github.com/FasterXML/jackson-databind/issues/4435
        assertTrue(NumberInput.looksLikeValidNumber(".0"));
        assertTrue(NumberInput.looksLikeValidNumber("-.0"));
        assertTrue(NumberInput.looksLikeValidNumber("+.0"));
        assertTrue(NumberInput.looksLikeValidNumber(".01"));
        assertTrue(NumberInput.looksLikeValidNumber("-.01"));
        assertTrue(NumberInput.looksLikeValidNumber("+.01"));

        assertTrue(NumberInput.looksLikeValidNumber("0.01"));
        assertTrue(NumberInput.looksLikeValidNumber("-0.10"));
        assertTrue(NumberInput.looksLikeValidNumber("+0.25")); // non-JSON

        assertTrue(NumberInput.looksLikeValidNumber("10.33"));
        assertTrue(NumberInput.looksLikeValidNumber("-1.39"));
        assertTrue(NumberInput.looksLikeValidNumber("+125.0")); // non-JSON
        
        assertTrue(NumberInput.looksLikeValidNumber("1E10"));
        assertTrue(NumberInput.looksLikeValidNumber("-1E10"));
        assertTrue(NumberInput.looksLikeValidNumber("1e-10"));
        assertTrue(NumberInput.looksLikeValidNumber("1e+10"));
        assertTrue(NumberInput.looksLikeValidNumber("+1e+10"));
        assertTrue(NumberInput.looksLikeValidNumber("1.4E-45"));
        assertTrue(NumberInput.looksLikeValidNumber("1.4e+45"));

        // https://github.com/FasterXML/jackson-core/issues/1308
        assertTrue(NumberInput.looksLikeValidNumber("0."));
        assertTrue(NumberInput.looksLikeValidNumber("6."));
        assertTrue(NumberInput.looksLikeValidNumber("65."));
        assertTrue(NumberInput.looksLikeValidNumber("654."));
        assertTrue(NumberInput.looksLikeValidNumber("65432."));
        assertTrue(NumberInput.looksLikeValidNumber("-0."));
        assertTrue(NumberInput.looksLikeValidNumber("-6."));
        assertTrue(NumberInput.looksLikeValidNumber("-65."));
        assertTrue(NumberInput.looksLikeValidNumber("-654."));
        assertTrue(NumberInput.looksLikeValidNumber("-65432."));
        assertTrue(NumberInput.looksLikeValidNumber("+0."));
        assertTrue(NumberInput.looksLikeValidNumber("+6."));
        assertTrue(NumberInput.looksLikeValidNumber("+65."));
        assertTrue(NumberInput.looksLikeValidNumber("+654."));
        assertTrue(NumberInput.looksLikeValidNumber("+65432."));
    }

    @Test
    void looksLikeValidNumberFalse()
    {
        // https://github.com/FasterXML/jackson-databind/issues/4435 and
        // https://github.com/FasterXML/jackson-core/issues/1308
        assertFalse(NumberInput.looksLikeValidNumber(""));
        assertFalse(NumberInput.looksLikeValidNumber(" "));
        assertFalse(NumberInput.looksLikeValidNumber("   "));
        assertFalse(NumberInput.looksLikeValidNumber("."));
        assertFalse(NumberInput.looksLikeValidNumber("10_000"));
        assertFalse(NumberInput.looksLikeValidNumber("-"));
        assertFalse(NumberInput.looksLikeValidNumber("+"));
        assertFalse(NumberInput.looksLikeValidNumber("-."));
        assertFalse(NumberInput.looksLikeValidNumber("+."));
        assertFalse(NumberInput.looksLikeValidNumber("-E"));
        assertFalse(NumberInput.looksLikeValidNumber("+E"));
    }

    // [core#1699]: following tests exercise the branches of the hand-rolled
    //   scanner that replaced the original regexp

    @Test
    void looksLikeValidNumberSigns()
    {
        _assertValid("+0", "-0", "+9", "-9", "+.5", "-.5", "+5.", "-5.",
                "+1e5", "-1e5", "+1.5e-5", "-1.5e+5");
        // Sign alone, repeated or misplaced
        _assertInvalid("+", "-", "++1", "--1", "+-1", "-+1", "1+", "1-",
                "+ 1", "1+1", "1-1");
    }

    @Test
    void looksLikeValidNumberIntegers()
    {
        _assertValid("0", "9", "00", "0001", "1234567890", "9999999999999999999999");
        _assertInvalid("", " ", "x", "1x", "x1", "1 2", "0x10", "10_000",
                "Infinity", "-Infinity", "NaN");
    }

    @Test
    void looksLikeValidNumberDecimals()
    {
        // Leading dot, trailing dot, and both sides present
        _assertValid(".0", ".5", "0.", "5.", "0.0", "00.00", "1.5", "-0.10", "+0.25");
        // A dot needs a digit on at least one side, and only one dot is allowed
        _assertInvalid(".", "-.", "+.", "..", "1..2", "1.2.3", ".1.", "1..", ".." + ".");
    }

    @Test
    void looksLikeValidNumberExponents()
    {
        _assertValid("1e1", "1E1", "1e+1", "1e-1", "1e0", "1e007", "0e0",
                "1.5e10", ".5e10", "1.5E-45", "1.4e+45");
        // Exponent marker must be followed by at least one digit, optionally signed
        _assertInvalid("1e", "1E", "1e+", "1e-", "e10", "E10", "+e10", "e", "E",
                "1e+x", "1ee1", "1e1e1", "1e1.5", "1e.5", "1e1.", "5e5e5");
        // Trailing dot cannot carry an exponent: the "12." form is a separate,
        // exponent-less case
        _assertInvalid("1.e5", "1.e", "12.e5", "-12.E5");
    }

    @Test
    void looksLikeValidNumberNonAsciiDigits()
    {
        // Only ASCII digits count, matching the original regexp's [0-9]
        _assertInvalid("\u0661\u0662\u0663", // Arabic-Indic 123
                "\uFF11\uFF12\uFF13", // full-width 123
                "1\u0661", "\u06603"); // mixed
    }

    @Test
    void looksLikeValidNumberWhitespace()
    {
        // No trimming is performed by this method
        _assertInvalid(" 1", "1 ", " 1 ", "\t1", "1\t", "\n1", "1\n", "1\r", "  ");
    }

    @Test
    void looksLikeValidNumberNull()
    {
        assertFalse(NumberInput.looksLikeValidNumber(null));
    }

    private void _assertValid(String... inputs) {
        for (String input : inputs) {
            assertTrue(NumberInput.looksLikeValidNumber(input),
                    "Should be valid: \"" + input + "\"");
        }
    }

    private void _assertInvalid(String... inputs) {
        for (String input : inputs) {
            assertFalse(NumberInput.looksLikeValidNumber(input),
                    "Should be invalid: \"" + input + "\"");
        }
    }

    // [core#1699]: used to backtrack quadratically on long input
    @Test
    void looksLikeValidNumberLongInput()
    {
        final int len = 1_000_000;
        final String digits = _repeat('9', len);

        assertTimeoutPreemptively(java.time.Duration.ofSeconds(10), new Executable() {
            @Override
            public void execute() {
                // Valid: plain digits, and digits with fraction and exponent
                assertTrue(NumberInput.looksLikeValidNumber(digits));
                assertTrue(NumberInput.looksLikeValidNumber("-" + digits + "." + digits + "e" + digits));
                // Invalid: worst case for the old regexp, a long digit run that
                // only fails on the very last character
                assertFalse(NumberInput.looksLikeValidNumber(digits + "x"));
                assertFalse(NumberInput.looksLikeValidNumber(digits + "." + digits + "x"));
            }
        });
    }

    private String _repeat(char c, int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; ++i) {
            sb.append(c);
        }
        return sb.toString();
    }
}
