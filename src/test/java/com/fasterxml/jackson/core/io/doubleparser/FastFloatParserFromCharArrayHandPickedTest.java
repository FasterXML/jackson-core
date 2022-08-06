/**
 * References:
 * <dl>
 *     <dt>This class has been derived from "FastDoubleParser".</dt>
 *     <dd>Copyright (c) Werner Randelshofer. Apache 2.0 License.
 *         <a href="https://github.com/wrandelshofer/FastDoubleParser">github.com</a>.</dd>
 * </dl>
 */

package com.fasterxml.jackson.core.io.doubleparser;

public class FastFloatParserFromCharArrayHandPickedTest extends AbstractFloatHandPickedTest {
    @Override
    float parse(CharSequence str) {
        char[] chars = new char[str.length()];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = str.charAt(i);
        }
        return FastFloatParser.parseFloat(chars);
    }

    @Override
    protected float parse(String str, int offset, int length) {
        return FastFloatParser.parseFloat(str.toCharArray(), offset, length);
    }
}
