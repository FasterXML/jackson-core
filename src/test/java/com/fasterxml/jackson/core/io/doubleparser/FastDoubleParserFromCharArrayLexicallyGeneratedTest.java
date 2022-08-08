/**
 * References:
 * <dl>
 *     <dt>This class has been derived from "FastDoubleParser".</dt>
 *     <dd>Copyright (c) Werner Randelshofer. Apache 2.0 License.
 *         <a href="https://github.com/wrandelshofer/FastDoubleParser">github.com</a>.</dd>
 * </dl>
 */

package com.fasterxml.jackson.core.io.doubleparser;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FastDoubleParserFromCharArrayLexicallyGeneratedTest extends AbstractLexicallyGeneratedTest {
    @Override
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
            actual = FastDoubleParser.parseDouble(str.toCharArray());
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
