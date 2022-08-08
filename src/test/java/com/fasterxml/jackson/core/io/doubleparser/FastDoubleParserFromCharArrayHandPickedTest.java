/**
 * References:
 * <dl>
 *     <dt>This class has been derived from "FastDoubleParser".</dt>
 *     <dd>Copyright (c) Werner Randelshofer. Apache 2.0 License.
 *         <a href="https://github.com/wrandelshofer/FastDoubleParser">github.com</a>.</dd>
 * </dl>
 */

package com.fasterxml.jackson.core.io.doubleparser;

public class FastDoubleParserFromCharArrayHandPickedTest extends AbstractDoubleHandPickedTest {
    @Override
    double parse(CharSequence str) {
        char[] chars = new char[str.length()];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = str.charAt(i);
        }
        return FastDoubleParser.parseDouble(chars);
    }

    @Override
    protected double parse(String str, int offset, int length) {
        return FastDoubleParser.parseDouble(str.toCharArray(), offset, length);
    }
}
