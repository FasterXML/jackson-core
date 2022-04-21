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
    }

    public void testParseFloat()
    {
        final String exampleFloat = "1.199999988079071";
        assertEquals(1.1999999f, NumberInput.parseFloat(exampleFloat));
    }
}

