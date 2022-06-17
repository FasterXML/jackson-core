/**
 * References:
 * <dl>
 *     <dt>This class has been derived from "FastDoubleParser".</dt>
 *     <dd>Copyright (c) Werner Randelshofer. Apache 2.0 License.
 *         <a href="https://github.com/wrandelshofer/FastDoubleParser">github.com</a>.</dd>
 * </dl>
 */

package com.fasterxml.jackson.core.io.doubleparser;

public class FastFloatParserFromCharSequenceHandPickedTest extends AbstractFloatHandPickedTest {


    @Override
    float parse(CharSequence str) {
        return FastFloatParser.parseFloat(str);
    }

    @Override
    protected float parse(String str, int offset, int length) {
        return FastFloatParser.parseFloat(str, offset, length);
    }
}
