/*
 * @(#)FastDoubleParserLexcicallyGeneratedTest.java
 * Copyright © 2021. Werner Randelshofer, Switzerland. MIT License.
 */

package com.fasterxml.jackson.core.io.doubleparser;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FastDoubleParserFromCharSequenceLexicallyGeneratedTest extends AbstractLexicallyGeneratedTest {
    protected double parse(String str) {
        return FastDoubleParser.parseDouble(str);
    }

    protected void testAgainstJdk(String str) {
        double expected = 0.0;
        boolean isExpectedToFail = false;
        try {
            expected = Double.parseDouble(str);
        } catch (NumberFormatException t) {
            isExpectedToFail = true;
        }

        double actual = 0;
        boolean actualFailed = false;
        try {
            actual = FastDoubleParser.parseDouble(str);
        } catch (NumberFormatException t) {
            actualFailed = true;
        }

        assertEquals(isExpectedToFail, actualFailed);
        if (!isExpectedToFail) {
            assertEquals(expected, actual, "str=" + str);
            assertEquals(Double.doubleToLongBits(expected), Double.doubleToLongBits(actual),
                    "longBits of " + expected);
        }
    }
}
