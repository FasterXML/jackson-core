package com.fasterxml.jackson.core.io;

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

    public void testNastyAcceptedDoubleValues() {
        final String[] nastyAcceptedDoubleValues = new String[]{"1.1e-23f", "0x.003p12f", "0x1.17742db862a4P-1d"};

        for (String nastyAcceptedDouble : nastyAcceptedDoubleValues) {
            assertEquals(Double.parseDouble(nastyAcceptedDouble), NumberInput.parseDouble(nastyAcceptedDouble));
            assertEquals(Double.parseDouble(nastyAcceptedDouble), NumberInput.parseDouble(nastyAcceptedDouble, true));
        }
    }
}

