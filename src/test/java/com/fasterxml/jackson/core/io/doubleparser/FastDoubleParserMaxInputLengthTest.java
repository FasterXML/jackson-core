/**
 * References:
 * <dl>
 *     <dt>This class has been derived from "FastDoubleParser".</dt>
 *     <dd>Copyright (c) Werner Randelshofer. Apache 2.0 License.
 *         <a href="https://github.com/wrandelshofer/FastDoubleParser">github.com</a>.</dd>
 * </dl>
 */

package com.fasterxml.jackson.core.io.doubleparser;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

public class FastDoubleParserMaxInputLengthTest {
    @TestFactory
    @Disabled
    List<DynamicNode> dynamicTestsIllegalMaxLengthInputs() {
        return Arrays.asList(
                dynamicTest("empty", () -> testIllegalMaxLengthInput("")),
                dynamicTest("-", () -> testIllegalMaxLengthInput("-")),
                dynamicTest("+", () -> testIllegalMaxLengthInput("+")),
                dynamicTest("1e", () -> testIllegalMaxLengthInput("1e")),
                dynamicTest("1_000", () -> testIllegalMaxLengthInput("1_000")),
                dynamicTest("0.000_1", () -> testIllegalMaxLengthInput("0.000_1")),
                dynamicTest("-e-55", () -> testIllegalMaxLengthInput("-e-55")),
                dynamicTest("1 x", () -> testIllegalMaxLengthInput("1 x")),
                dynamicTest("x 1", () -> testIllegalMaxLengthInput("x 1")),
                dynamicTest("1§", () -> testIllegalMaxLengthInput("1§")),
                dynamicTest("NaN x", () -> testIllegalMaxLengthInput("NaN x")),
                dynamicTest("Infinity x", () -> testIllegalMaxLengthInput("Infinity x")),
                dynamicTest("0x123.456789abcde", () -> testIllegalMaxLengthInput("0x123.456789abcde"))
        );
    }

    @TestFactory
    @Disabled
    List<DynamicNode> dynamicTestsLegalDecFloatMaxLengthLiterals() {
        return Arrays.asList(
                dynamicTest("1e23", () -> testLegalMaxLengthInput("1e23", 1e23)),
                dynamicTest("whitespace after 1", () -> testLegalMaxLengthInput("1 ", 1)),
                dynamicTest("0", () -> testLegalMaxLengthInput("0", 0.0)),
                dynamicTest("-0", () -> testLegalMaxLengthInput("-0", -0.0)),
                dynamicTest("+0", () -> testLegalMaxLengthInput("+0", +0.0)),
                dynamicTest("-0.0", () -> testLegalMaxLengthInput("-0.0", -0.0)),
                dynamicTest("-0.0e-22", () -> testLegalMaxLengthInput("-0.0e-22", -0.0e-22)),
                dynamicTest("-0.0e24", () -> testLegalMaxLengthInput("-0.0e24", -0.0e24)),
                dynamicTest("0e555", () -> testLegalMaxLengthInput("0e555", 0.0)),
                dynamicTest("-0e555", () -> testLegalMaxLengthInput("-0e555", -0.0)),
                dynamicTest("1", () -> testLegalMaxLengthInput("1", 1.0)),
                dynamicTest("-1", () -> testLegalMaxLengthInput("-1", -1.0)),
                dynamicTest("+1", () -> testLegalMaxLengthInput("+1", +1.0)),
                dynamicTest("1e0", () -> testLegalMaxLengthInput("1e0", 1e0)),
                dynamicTest("1.e0", () -> testLegalMaxLengthInput("1.e0", 1e0)),
                dynamicTest(".e2", () -> testLegalMaxLengthInput(".e2", 0)),
                dynamicTest("1e1", () -> testLegalMaxLengthInput("1e1", 1e1)),
                dynamicTest("1e+1", () -> testLegalMaxLengthInput("1e+1", 1e+1)),
                dynamicTest("1e-1", () -> testLegalMaxLengthInput("1e-1", 1e-1)),
                dynamicTest("0049", () -> testLegalMaxLengthInput("0049", 49)),
                dynamicTest("9999999999999999999", () -> testLegalMaxLengthInput("9999999999999999999", 9999999999999999999d)),
                dynamicTest("972150611626518208.0", () -> testLegalMaxLengthInput("972150611626518208.0", 9.7215061162651827E17)),
                dynamicTest("3.7587182468424695418288325e-309", () -> testLegalMaxLengthInput("3.7587182468424695418288325e-309", 3.7587182468424695418288325e-309)),
                dynamicTest("9007199254740992.e-256", () -> testLegalMaxLengthInput("9007199254740992.e-256", 9007199254740992.e-256)),
                dynamicTest("0.1e+3", () -> testLegalMaxLengthInput("0.1e+3",
                        100.0))
        );
    }

    @TestFactory
    List<DynamicNode> dynamicTestsLegalHexFloatMaxLengthLiterals() {
        return Arrays.asList(
                dynamicTest("0x1.0p8", () -> testLegalMaxLengthInput("0x1.0p8", 256))
        );
    }

    double parse(CharSequence str) {
        return FastDoubleParser.parseDouble(str);
    }

    private void testIllegalMaxLengthInput(String s) {
        try {
            parse(new MaxLengthCharSequence(s));
            fail();
        } catch (NumberFormatException e) {
            // success
        }
    }

    private void testLegalMaxLengthInput(String str, double expected) {
        double actual = parse(new MaxLengthCharSequence(str));
        assertEquals(expected, actual, "str(length=Integer.MAX_VALUE)=" + str);
        assertEquals(Double.doubleToLongBits(expected), Double.doubleToLongBits(actual),
                "longBits of " + expected);
    }

    private static class MaxLengthCharSequence implements CharSequence {
        private final String str;
        private int startIndex;
        private int endIndex;

        private MaxLengthCharSequence(String str) {
            this.str = str;
            this.startIndex = 0;
            this.endIndex = Integer.MAX_VALUE;
        }

        private MaxLengthCharSequence(String str, int startIndex, int endIndex) {
            this.str = str;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }

        @Override
        public char charAt(int index) {
            return index - startIndex < endIndex - str.length()
                    ? ' '
                    : str.charAt(index - startIndex - (endIndex - str.length()));
        }

        @Override
        public int length() {
            return endIndex - startIndex;
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return new MaxLengthCharSequence(str, start, end);
        }

        @Override
        public String toString() {
            return str;
        }
    }
}
