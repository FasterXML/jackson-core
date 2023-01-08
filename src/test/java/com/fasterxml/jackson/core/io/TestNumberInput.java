package com.fasterxml.jackson.core.io;

import java.math.BigInteger;

public class TestNumberInput
    extends com.fasterxml.jackson.core.BaseTest
{
    public void testNastySmallDouble()
    {
        //relates to https://github.com/FasterXML/jackson-core/issues/750
        //prior to jackson v2.14, this value used to be returned as Double.MIN_VALUE
        final String nastySmallDouble = "2.2250738585072012e-308";
        assertEquals(Double.parseDouble(nastySmallDouble), NumberInput.parseDouble(nastySmallDouble));
        assertEquals(Double.parseDouble(nastySmallDouble), NumberInput.parseDouble(nastySmallDouble, true));
    }

    public void testParseFloat()
    {
        final String exampleFloat = "1.199999988079071";
        assertEquals(1.1999999f, NumberInput.parseFloat(exampleFloat));
        assertEquals(1.1999999f, NumberInput.parseFloat(exampleFloat, true));
        assertEquals(1.2f, (float)NumberInput.parseDouble(exampleFloat));
        assertEquals(1.2f, (float)NumberInput.parseDouble(exampleFloat, true));

        final String exampleFloat2 = "7.006492321624086e-46";
        assertEquals("1.4E-45", Float.toString(NumberInput.parseFloat(exampleFloat2)));
        assertEquals("1.4E-45", Float.toString(NumberInput.parseFloat(exampleFloat2, true)));
    }

    public void testParseLongBigInteger()
    {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            stringBuilder.append(7);
        }
        String test1000 = stringBuilder.toString();
        assertEquals(new BigInteger(test1000), NumberInput.parseBigInteger(test1000));
        assertEquals(new BigInteger(test1000), NumberInput.parseBigInteger(test1000, true));
        for (int i = 0; i < 1000; i++) {
            stringBuilder.append(7);
        }
        String test2000 = stringBuilder.toString();
        assertEquals(new BigInteger(test2000), NumberInput.parseBigInteger(test2000));
        assertEquals(new BigInteger(test2000), NumberInput.parseBigInteger(test2000, true));
    }

    public void testBigIntegerWithRadix()
    {
        final String val = "1ABCDEF";
        final int radix = 16;
        BigInteger expected = new BigInteger(val, radix);
        assertEquals(expected, NumberInput.parseBigIntegerWithRadix(val, radix, true));
        assertEquals(expected, NumberInput.parseBigIntegerWithRadix(val, radix, false));
    }
}

