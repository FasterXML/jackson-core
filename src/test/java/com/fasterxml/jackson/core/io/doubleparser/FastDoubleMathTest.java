/**
 * References:
 * <dl>
 *     <dt>This class has been derived from "FastDoubleParser".</dt>
 *     <dd>Copyright (c) Werner Randelshofer. Apache 2.0 License.
 *         <a href="https://github.com/wrandelshofer/FastDoubleParser">github.com</a>.</dd>
 * </dl>
 */

package com.fasterxml.jackson.core.io.doubleparser;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * Tests class {@link FastDoubleMath}.
 */
public class FastDoubleMathTest {
    @Test
    public void testFullMultiplication() {
        FastDoubleMath.UInt128 actual = FastDoubleMath.fullMultiplication(0x123456789ABCDEF0L, 0x10L);
        assertEquals(1L, actual.high);
        assertEquals(0x23456789abcdef00L, actual.low);

        actual = FastDoubleMath.fullMultiplication(0x123456789ABCDEF0L, -0x10L);
        assertEquals(0x123456789abcdeeeL, actual.high);
        assertEquals(0xdcba987654321100L, actual.low);
    }

    @TestFactory
    List<DynamicNode> dynamicTestsTryDecFloatToDouble() {
        return Arrays.asList(
                dynamicTest("Inside Clinger fast path \"1000000000000000000e-340\")", () -> testTryDecFloatToDouble(false, 1000000000000000000L, -325, 1000000000000000000e-325)),
                //
                dynamicTest("Inside Clinger fast path (max_clinger_significand, max_clinger_exponent)", () -> testTryDecFloatToDouble(false, 9007199254740991L, 22, 9007199254740991e22)),
                dynamicTest("Outside Clinger fast path (max_clinger_significand, max_clinger_exponent + 1)", () -> testTryDecFloatToDouble(false, 9007199254740991L, 23, 9007199254740991e23)),
                dynamicTest("Outside Clinger fast path (max_clinger_significand + 1, max_clinger_exponent)", () -> testTryDecFloatToDouble(false, 9007199254740992L, 22, 9007199254740992e22)),
                dynamicTest("Inside Clinger fast path (min_clinger_significand + 1, min_clinger_exponent)", () -> testTryDecFloatToDouble(false, 1L, -22, 1e-22)),
                dynamicTest("Outside Clinger fast path (min_clinger_significand + 1, min_clinger_exponent - 1)", () -> testTryDecFloatToDouble(false, 1L, -23, 1e-23)),
                dynamicTest("Outside Clinger fast path, bail out in semi-fast path, -8446744073709551617", () -> testTryDecFloatToDouble(false, -8446744073709551617L, 0, Double.NaN)),
                dynamicTest("Outside Clinger fast path, semi-fast path, -9223372036854775808e7", () -> testTryDecFloatToDouble(false, -9223372036854775808L, 7, 9.223372036854776E25)),
                dynamicTest("Outside Clinger fast path, semi-fast path, exponent out of range, -9223372036854775808e-325", () -> testTryDecFloatToDouble(false, -9223372036854775808L, -325, 9.223372036854776E-307)),
                dynamicTest("Outside Clinger fast path, bail-out in semi-fast path, 1e23", () -> testTryDecFloatToDouble(false, 1L, 23, Double.NaN)),
                dynamicTest("Outside Clinger fast path, mantissa overflows in semi-fast path, 7.2057594037927933e+16", () -> testTryDecFloatToDouble(false, 72057594037927933L, 0, 7.205759403792794E16)),
                dynamicTest("Outside Clinger fast path, bail-out in semi-fast path, 7.3177701707893310e+15", () -> testTryDecFloatToDouble(false, 73177701707893310L, -1, Double.NaN))
        );
    }

    public void testTryDecFloatToDouble(boolean isNegative, long significand, int power, double expected) {
        double actual = FastDoubleMath.tryDecFloatToDouble(isNegative, significand, power);
        assertEquals(expected, actual);
    }
}
