/**
 * References:
 * <dl>
 *     <dt>This class has been derived from "FastDoubleParser".</dt>
 *     <dd>Copyright (c) Werner Randelshofer. Apache 2.0 License.
 *         <a href="https://github.com/wrandelshofer/FastDoubleParser">github.com</a>.</dd>
 * </dl>
 */

package com.fasterxml.jackson.core.io.doubleparser;

public class FastFloatParserFromCharSequenceNumericallyGeneratedTest extends AbstractFloatNumericallyGeneratedTest {
    @Override
    protected float parse(String str) {
        return FastFloatParser.parseFloat(str);
    }
}
