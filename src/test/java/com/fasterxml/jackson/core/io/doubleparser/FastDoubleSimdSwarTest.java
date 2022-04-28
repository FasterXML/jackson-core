
/*
 * @(#)FastDoubleSimdUtf16SwarTest.java
 * Copyright © 2022. Werner Randelshofer, Switzerland. MIT License.
 */

package com.fasterxml.jackson.core.io.doubleparser;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FastDoubleSimdSwarTest extends AbstractFastDoubleSimdTest {
    @Override
    void testDec(String s, int offset, int expected) {
        char[] chars = s.toCharArray();

        int actual = FastDoubleSimd.tryToParseEightDigitsUtf16Swar(chars, offset);
        assertEquals(expected, actual);


        long first = chars[offset + 0] | ((long) chars[offset + 1] << 16) | ((long) chars[offset + 2] << 32) | ((long) chars[offset + 3] << 48);
        long second = chars[offset + 4] | ((long) chars[offset + 5] << 16) | ((long) chars[offset + 6] << 32) | ((long) chars[offset + 7] << 48);
        actual = FastDoubleSimd.tryToParseEightDigitsUtf16Swar(first, second);
        assertEquals(expected, actual);


        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        actual = FastDoubleSimd.tryToParseEightDigitsUtf8Swar(bytes, offset);
        assertEquals(expected, actual);

        long value = ((bytes[offset + 7] & 0xffL) << 56)
                | ((bytes[offset + 6] & 0xffL) << 48)
                | ((bytes[offset + 5] & 0xffL) << 40)
                | ((bytes[offset + 4] & 0xffL) << 32)
                | ((bytes[offset + 3] & 0xffL) << 24)
                | ((bytes[offset + 2] & 0xffL) << 16)
                | ((bytes[offset + 1] & 0xffL) << 8)
                | (bytes[offset] & 0xffL);

        int result;
        long val = value - 0x3030303030303030L;
        long det = ((value + 0x4646464646464646L) | val) &
                0x8080808080808080L;
        if (det != 0L) {
            result = -1;
        } else {// The last 2 multiplications in this algorithm are independent of each
// other.
            long mask = 0x000000FF_000000FFL;
            val = (val * 0xa_01L) >>> 8;// 1+(10<<8)
            val = (((val & mask) * 0x000F4240_00000064L)//100 + (1000000 << 32)
                    + (((val >>> 16) & mask) * 0x00002710_00000001L)) >>> 32;// 1 + (10000 << 32)
            result = (int) val;
        }

        actual = result;
        assertEquals(expected, actual);
    }

    @Override
    void testHex(String s, int offset, long expected) {
        char[] chars = s.toCharArray();
        long actual = FastDoubleSimd.tryToParseEightHexDigitsUtf16Swar(chars, offset);
        assertEquals(expected, actual);

        long first = (long) chars[offset + 0] << 48
                | (long) chars[offset + 1] << 32
                | (long) chars[offset + 2] << 16
                | (long) chars[offset + 3];

        long second = (long) chars[offset + 4] << 48
                | (long) chars[offset + 5] << 32
                | (long) chars[offset + 6] << 16
                | (long) chars[offset + 7];
        actual = FastDoubleSimd.tryToParseEightHexDigitsUtf16Swar(first, second);
        assertEquals(expected, actual);

        actual = FastDoubleSimd.tryToParseEightHexDigitsUtf8Swar(s.getBytes(StandardCharsets.UTF_8), offset);
        assertEquals(expected, actual);

    }
}
