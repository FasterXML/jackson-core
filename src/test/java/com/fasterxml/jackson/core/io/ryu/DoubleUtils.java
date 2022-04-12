/*
 * Copyright 2016 Heng Yuan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fasterxml.jackson.core.io.ryu;

import java.math.BigInteger;

/**
 * The algorithm and code used here are from
 * 	Jaffer, Aubrey. "Easy Accurate Reading and Writing of Floating-Point
 * 	Numbers." arXiv preprint arXiv:1310.8121 (2013).
 *  (<a href="https://arxiv.org/pdf/1310.8121.pdf">PDF</a>)
 * <p>
 * And here is the license given from him:
 * <pre>
 * Copyright (c) Aubrey Jaffer 2014
 * Permission to copy this software, to modify it, to redistribute it,
 * to distribute modified versions, and to use it for any purpose is
 * granted, subject to the following restrictions and understandings.
 *
 * 1.  Any copy made of this software must include this copyright notice
 * in full.
 *
 * 2.  I have made no warranty or representation that the operation of
 * this software will be error-free, and I am under no obligation to
 * provide any services, by way of maintenance, update, or otherwise.
 *
 * 3.  In conjunction with products arising from the use of this
 * material, there shall be no use of my name in any advertising,
 * promotional, or sales literature without prior written consent in
 * each case.
 * </pre>
 * <p>
 * I made minor modifications to fit my need.
 *
 * @author	Heng Yuan
 */
class DoubleUtils
{
    private final static int dblMantDig = 53;
    private final static BigInteger[] bp5a = new BigInteger[326];

    private static final long lp5[] =
            { 1L, 5L, 25L, 125L, 625L, 3125L, 15625L, 78125L, 390625L, 1953125L,
                    9765625L, 48828125L, 244140625L, 1220703125L, 6103515625L, 30517578125L,
                    152587890625L, 762939453125L, 3814697265625L, 19073486328125L,
                    95367431640625L, 476837158203125L, 2384185791015625L, 11920928955078125L,
                    59604644775390625L, 298023223876953125L, 1490116119384765625L,
                    7450580596923828125L };

    private final static BigInteger bp5 (int p)
    {
        BigInteger[] pa = bp5a;
        if (pa[p] != null)
            return pa[p];
        else if (p < lp5.length)
        {
            BigInteger v = BigInteger.valueOf (lp5[p]);
            pa[p] = v;
            return v;
        }
        else
        {
            // use divide-n-conquer strategy to compute the number
            int a = p >> 1;
            int b = p - a; // b has 50% chance being the same as a
            BigInteger v = bp5 (a).multiply (bp5 (b));
            pa[p] = v;
            return v;
        }
    }

    private final static double llog2 = Math.log10 (2);

    private static long rq (BigInteger num, BigInteger den)
    {
        BigInteger quorem[] = num.divideAndRemainder (den);
        long quo = quorem[0].longValue ();
        int cmpflg = quorem[1].shiftLeft (1).compareTo (den);
        if ((quo & 1L) == 0L ? 1 == cmpflg : -1 < cmpflg)
            return quo + 1L;
        else
            return quo;
    }

    private static double metd (long lmant, int point)
    {
        BigInteger mant = BigInteger.valueOf (lmant);
        if (point >= 0)
        {
            BigInteger num = mant.multiply (bp5 (point));
            int bex = num.bitLength () - dblMantDig;
            if (bex <= 0)
                return Math.scalb (num.doubleValue (), point);
            long quo = rq (num, BigInteger.ONE.shiftLeft (bex));
            return Math.scalb ((double) quo, bex + point);
        }
        int maxpow = bp5a.length - 1;
        BigInteger scl = (-point <= maxpow) ? bp5 (-point) : bp5 (maxpow).multiply (bp5 (-point - maxpow));
        int bex = mant.bitLength () - scl.bitLength () - dblMantDig;
        BigInteger num = mant.shiftLeft (-bex);
        long quo = rq (num, scl);
        if (64 - Long.numberOfLeadingZeros (quo) > dblMantDig)
        {
            bex++;
            quo = rq (num, scl.shiftLeft (1));
        }
        return Math.scalb ((double) quo, bex + point);
    }

    /**
     * This function converts a double representation to a string format.
     *
     * @param	f
     *			A double value.
     * @return	A string representation of the double value f.
     */
    public static String toString (double f)
    {
        long lbits = Double.doubleToLongBits (f);
        if (f != f)
            return "NaN";
        if (f + f == f)
            return (f == 0.0) ? "0" : ((f > 0) ? "Infinity" : "-Infinity");
        StringBuilder str = new StringBuilder (24);
        if (f < 0)
        {
            str.append ('-');
            // there is a rounding bug for negative values with positive
            // exp, such as -5e100.  Negate the value to avoid the issue.
            f = -f;
        }
        int ue2 = (int) (lbits >>> 52 & 0x7ff);
        int e2 = ue2 - 1023 - 52 + (ue2 == 0 ? 1 : 0);
        int point = (int) Math.ceil (e2 * llog2);
        long lquo;
        long lmant = (lbits & ((1L << 52) - 1)) + (ue2 == 0 ? 0L : 1L << 52);
        BigInteger mant = BigInteger.valueOf (lmant);
        if (e2 > 0)
        {
            BigInteger num = mant.shiftLeft (e2 - point);
            lquo = rq (num, bp5 (point));
            if (metd (lquo, point) != f)
                lquo = rq (num.shiftLeft (1), bp5 (--point));
        }
        else
        {
            BigInteger num = mant.multiply (bp5 (-point));
            BigInteger den = BigInteger.ONE.shiftLeft (point - e2);
            lquo = rq (num, den);
            if (metd (lquo, point) != f)
            {
                point--;
                lquo = rq (num.multiply (BigInteger.TEN), den);
            }
        }
        String sman = Long.toString (lquo);
        int len = sman.length (), lent = len;
        while (sman.charAt (lent - 1) == '0')
        {
            lent--;
        }
        int exp = point + len - 1;

        if (exp >= 0 && exp < len)
        {
            // length string is longer than exp
            // so the period is in the middle of sman
            ++exp; // exp is now the period location
            str.append (sman, 0, exp);
            if (lent > exp)
            {
                str.append ('.');
                str.append (sman, exp, lent);
            }
        }
        else if (exp < 0 && exp > -5)
        {
            str.append ("0.");
            str.append ("0000".substring (0, -exp - 1));
            str.append (sman, 0, lent);
        }
        else
        {
            // scientific notation

            str.append (sman, 0, 1);
            if (lent > 1)
            {
                str.append ('.');
                str.append (sman, 1, lent);
            }
            if (exp != 0)
            {
                str.append ('e');
                str.append (exp);
            }
        }
        return str.toString ();
    }
}