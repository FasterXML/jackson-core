package com.fasterxml.jackson.core.io;

import java.math.BigInteger;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

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

        // Fully populated form: sign, integer part, fraction, signed exponent
        assertTrue(NumberInput.looksLikeValidNumber("+1.2e+3"));
        assertTrue(NumberInput.looksLikeValidNumber("-12.34E-56"));

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

        assertFalse(NumberInput.looksLikeValidNumber("1.2.3"));
        assertFalse(NumberInput.looksLikeValidNumber("1.e5"));
        assertFalse(NumberInput.looksLikeValidNumber("1e"));
        assertFalse(NumberInput.looksLikeValidNumber("1e+"));
        assertFalse(NumberInput.looksLikeValidNumber("1e5x"));
        assertFalse(NumberInput.looksLikeValidNumber("1e5.0"));
        assertFalse(NumberInput.looksLikeValidNumber("--1"));
        assertFalse(NumberInput.looksLikeValidNumber("1 "));
        assertFalse(NumberInput.looksLikeValidNumber(" 1"));
        assertFalse(NumberInput.looksLikeValidNumber("0x1F"));
    }

    // [core#1649]: hand-rolled implementation must accept exactly what the
    // original Regexp-based one did
    @Test
    void looksLikeValidNumberMatchesLegacyRegexps()
    {
        final Pattern patternFloat = Pattern.compile("[+-]?[0-9]*[\\.]?[0-9]+([eE][+-]?[0-9]+)?");
        final Pattern patternTrailingDot = Pattern.compile("[+-]?[0-9]+[\\.]");
        // Digit-class bounds ('0', '9') plus the chars just outside it ('/', ':')
        // to catch off-by-one in digit checks; rest are the structural characters
        final char[] alphabet = new char[] { '0', '9', '/', ':', '+', '-', '.', 'e', 'E' };

        // Length 5 needed to reach forms combining both signs ("+1e+1") or
        // integer + fraction + exponent ("1.2e3"); ~66k inputs, runs in ~50 msec
        _verifyAgainstRegexps(patternFloat, patternTrailingDot, alphabet, "", 5);
    }

    private void _verifyAgainstRegexps(Pattern patternFloat, Pattern patternTrailingDot,
            char[] alphabet, String prefix, int remainingLength)
    {
        if (!prefix.isEmpty()) {
            boolean exp = patternFloat.matcher(prefix).matches()
                    || patternTrailingDot.matcher(prefix).matches();
            assertEquals(exp, NumberInput.looksLikeValidNumber(prefix),
                    "Mismatch for input '"+prefix+"'");
        }
        if (remainingLength > 0) {
            for (char c : alphabet) {
                _verifyAgainstRegexps(patternFloat, patternTrailingDot, alphabet,
                        prefix + c, remainingLength - 1);
            }
        }
    }
}
