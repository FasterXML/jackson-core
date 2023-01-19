package com.fasterxml.jackson.core.io;

import java.math.BigDecimal;
import java.math.BigInteger;

public class TestLazyNumbers
    extends com.fasterxml.jackson.core.BaseTest
{
    public void testLazyDouble()
    {
        String num = "1.234e-2";
        LazyDouble ln = new LazyDouble(num, true);
        assertEquals(num, ln.getText());
        assertEquals(1.234e-2, ln.getNumber());
    }

    public void testLazyFloat()
    {
        String num = "1.234e-2";
        LazyFloat lf = new LazyFloat(num, true);
        assertEquals(num, lf.getText());
        assertEquals(1.234e-2f, lf.getNumber());
    }

    public void testLazyBigDecimal()
    {
        String num = "1.234e-2";
        LazyBigDecimal ld = new LazyBigDecimal(num, true);
        assertEquals(num, ld.getText());
        assertEquals(new BigDecimal(num), ld.getNumber());
    }

    public void testLazyBigInteger()
    {
        String num = "1234567890";
        LazyBigInteger ld = new LazyBigInteger(num, true);
        assertEquals(num, ld.getText());
        assertEquals(new BigInteger(num), ld.getNumber());
    }
}
