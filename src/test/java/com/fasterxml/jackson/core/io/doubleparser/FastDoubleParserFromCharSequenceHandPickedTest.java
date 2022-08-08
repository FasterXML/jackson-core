/**
 * References:
 * <dl>
 *     <dt>This class has been derived from "FastDoubleParser".</dt>
 *     <dd>Copyright (c) Werner Randelshofer. Apache 2.0 License.
 *         <a href="https://github.com/wrandelshofer/FastDoubleParser">github.com</a>.</dd>
 * </dl>
 */

package com.fasterxml.jackson.core.io.doubleparser;

public class FastDoubleParserFromCharSequenceHandPickedTest extends AbstractDoubleHandPickedTest {


    @Override
    double parse(CharSequence str) {
        return FastDoubleParser.parseDouble(str);
    }

    @Override
    protected double parse(String str, int offset, int length) {
        return FastDoubleParser.parseDouble(str, offset, length);
    }
}
