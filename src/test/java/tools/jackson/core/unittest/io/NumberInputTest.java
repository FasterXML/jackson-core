package tools.jackson.core.unittest.io;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import tools.jackson.core.io.NumberInput;
import tools.jackson.core.unittest.JacksonCoreTestBase;

import static org.junit.jupiter.api.Assertions.*;

class NumberInputTest
    extends JacksonCoreTestBase
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

    @Test
    void parseIntFromCharArray()
    {
        // Test parsing integers from char arrays with various lengths
        // Single digit
        char[] ch1 = "5".toCharArray();
        assertEquals(5, NumberInput.parseInt(ch1, 0, 1));

        // Two digits
        char[] ch2 = "42".toCharArray();
        assertEquals(42, NumberInput.parseInt(ch2, 0, 2));

        // Three digits
        char[] ch3 = "123".toCharArray();
        assertEquals(123, NumberInput.parseInt(ch3, 0, 3));

        // Four digits
        char[] ch4 = "1234".toCharArray();
        assertEquals(1234, NumberInput.parseInt(ch4, 0, 4));

        // Five digits
        char[] ch5 = "12345".toCharArray();
        assertEquals(12345, NumberInput.parseInt(ch5, 0, 5));

        // Six digits
        char[] ch6 = "123456".toCharArray();
        assertEquals(123456, NumberInput.parseInt(ch6, 0, 6));

        // Seven digits
        char[] ch7 = "1234567".toCharArray();
        assertEquals(1234567, NumberInput.parseInt(ch7, 0, 7));

        // Eight digits
        char[] ch8 = "12345678".toCharArray();
        assertEquals(12345678, NumberInput.parseInt(ch8, 0, 8));

        // Nine digits (max for fast path)
        char[] ch9 = "123456789".toCharArray();
        assertEquals(123456789, NumberInput.parseInt(ch9, 0, 9));

        // Test with offset
        char[] chOffset = "abc123def".toCharArray();
        assertEquals(123, NumberInput.parseInt(chOffset, 3, 3));

        // Test with leading plus sign
        char[] chPlus = "+42".toCharArray();
        assertEquals(42, NumberInput.parseInt(chPlus, 0, 3));
    }

    @Test
    void parseIntFromString()
    {
        // Positive numbers
        assertEquals(0, NumberInput.parseInt("0"));
        assertEquals(5, NumberInput.parseInt("5"));
        assertEquals(42, NumberInput.parseInt("42"));
        assertEquals(123, NumberInput.parseInt("123"));
        assertEquals(999999999, NumberInput.parseInt("999999999"));

        // Negative numbers
        assertEquals(-1, NumberInput.parseInt("-1"));
        assertEquals(-42, NumberInput.parseInt("-42"));
        assertEquals(-123, NumberInput.parseInt("-123"));
        assertEquals(-999999999, NumberInput.parseInt("-999999999"));

        // Boundary values
        assertEquals(Integer.MAX_VALUE, NumberInput.parseInt(String.valueOf(Integer.MAX_VALUE)));
        assertEquals(Integer.MIN_VALUE, NumberInput.parseInt(String.valueOf(Integer.MIN_VALUE)));
    }

    @Test
    void parseLongFromCharArray()
    {
        // Test 10 digit number
        char[] ch10 = "1234567890".toCharArray();
        assertEquals(1234567890L, NumberInput.parseLong(ch10, 0, 10));

        // Test 15 digit number
        char[] ch15 = "123456789012345".toCharArray();
        assertEquals(123456789012345L, NumberInput.parseLong(ch15, 0, 15));

        // Test 18 digit number
        char[] ch18 = "123456789012345678".toCharArray();
        assertEquals(123456789012345678L, NumberInput.parseLong(ch18, 0, 18));
    }

    @Test
    void parseLong19Digits()
    {
        // Test parsing exactly 19 digit numbers
        char[] ch19 = "1234567890123456789".toCharArray();
        assertEquals(1234567890123456789L, NumberInput.parseLong19(ch19, 0, false));
        assertEquals(-1234567890123456789L, NumberInput.parseLong19(ch19, 0, true));

        // Test with offset
        char[] chOffset = "xxx9223372036854775807".toCharArray();
        assertEquals(9223372036854775807L, NumberInput.parseLong19(chOffset, 3, false));
    }

    @Test
    void parseLongFromString()
    {
        // Short values (delegates to parseInt)
        assertEquals(0L, NumberInput.parseLong("0"));
        assertEquals(123L, NumberInput.parseLong("123"));
        assertEquals(-456L, NumberInput.parseLong("-456"));

        // Long values
        assertEquals(12345678901L, NumberInput.parseLong("12345678901"));
        assertEquals(-12345678901L, NumberInput.parseLong("-12345678901"));

        // Boundary values
        assertEquals(Long.MAX_VALUE, NumberInput.parseLong(String.valueOf(Long.MAX_VALUE)));
        assertEquals(Long.MIN_VALUE, NumberInput.parseLong(String.valueOf(Long.MIN_VALUE)));
    }

    @Test
    void inLongRangeFromCharArray()
    {
        // Values clearly in range
        char[] small = "12345".toCharArray();
        assertTrue(NumberInput.inLongRange(small, 0, 5, false));
        assertTrue(NumberInput.inLongRange(small, 0, 5, true));

        // Boundary testing - Long.MAX_VALUE = 9223372036854775807 (19 digits)
        char[] maxLong = "9223372036854775807".toCharArray();
        assertTrue(NumberInput.inLongRange(maxLong, 0, 19, false));

        // Just above Long.MAX_VALUE
        char[] aboveMax = "9223372036854775808".toCharArray();
        assertFalse(NumberInput.inLongRange(aboveMax, 0, 19, false));

        // Long.MIN_VALUE = -9223372036854775808 (19 digits without sign)
        char[] minLong = "9223372036854775808".toCharArray();
        assertTrue(NumberInput.inLongRange(minLong, 0, 19, true));

        // Just above Long.MIN_VALUE magnitude
        char[] aboveMin = "9223372036854775809".toCharArray();
        assertFalse(NumberInput.inLongRange(aboveMin, 0, 19, true));

        // 20 digits - always too large
        char[] tooLong = "12345678901234567890".toCharArray();
        assertFalse(NumberInput.inLongRange(tooLong, 0, 20, false));
    }

    @Test
    void inLongRangeFromString()
    {
        // Values clearly in range
        assertTrue(NumberInput.inLongRange("12345", false));
        assertTrue(NumberInput.inLongRange("12345", true));

        // Boundary testing
        assertTrue(NumberInput.inLongRange("9223372036854775807", false));
        assertFalse(NumberInput.inLongRange("9223372036854775808", false));

        assertTrue(NumberInput.inLongRange("9223372036854775808", true));
        assertFalse(NumberInput.inLongRange("9223372036854775809", true));

        // Too many digits
        assertFalse(NumberInput.inLongRange("12345678901234567890", false));
    }
}
