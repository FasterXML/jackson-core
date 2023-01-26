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

        Double d = new Double(num);
        LazyDouble ln2 = new LazyDouble(d);
        assertEquals(d.toString(), ln2.getText());
        assertEquals(d, ln2.getNumber());
    }

    public void testLazyFloat()
    {
        String num = "1.234e-2";
        LazyFloat lf = new LazyFloat(num, true);
        assertEquals(num, lf.getText());
        assertEquals(1.234e-2f, lf.getNumber());

        Float f = new Float(num);
        LazyFloat lf2 = new LazyFloat(f);
        assertEquals(f.toString(), lf2.getText());
        assertEquals(f, lf2.getNumber());
    }

    public void testLazyBigDecimal()
    {
        String num = "1.234e-2";
        LazyBigDecimal ld = new LazyBigDecimal(num, true);
        assertEquals(num, ld.getText());
        assertEquals(new BigDecimal(num), ld.getNumber());

        BigDecimal bd = new BigDecimal(num);
        LazyBigDecimal ld2 = new LazyBigDecimal(bd);
        assertEquals(bd.toString(), ld2.getText());
        assertEquals(bd, ld2.getNumber());
    }

    public void testLazyBigInteger()
    {
        String num = "1234567890";
        LazyBigInteger ln = new LazyBigInteger(num, true);
        assertEquals(num, ln.getText());
        assertEquals(new BigInteger(num), ln.getNumber());

        LazyBigInteger ln2 = new LazyBigInteger(new BigInteger(num));
        assertEquals(num, ln2.getText());
        assertEquals(new BigInteger(num), ln2.getNumber());
    }

    public void testLazyInteger()
    {
        String num = "1234567890";
        LazyInteger ln = new LazyInteger(Integer.parseInt(num));
        assertEquals(num, ln.getText());
        assertEquals(Integer.valueOf(num), ln.getNumber());
    }

    public void testLazyLong()
    {
        String num = "123456789012345678";
        LazyLong ln = new LazyLong(Long.parseLong(num));
        assertEquals(num, ln.getText());
        assertEquals(Long.valueOf(num), ln.getNumber());
    }
}
