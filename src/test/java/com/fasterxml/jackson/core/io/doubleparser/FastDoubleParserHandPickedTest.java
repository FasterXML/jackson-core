
/*
 * @(#)FastDoubleParserHandPickedTest.java
 * Copyright © 2021. Werner Randelshofer, Switzerland. MIT License.
 */

package com.fasterxml.jackson.core.io.doubleparser;

public class FastDoubleParserHandPickedTest extends AbstractDoubleHandPickedTest {


    @Override
    double parse(CharSequence str) {
        return FastDoubleParser.parseDouble(str);
    }

    @Override
    protected double parse(String str, int offset, int length) {
        return FastDoubleParser.parseDouble(str, offset, length);
    }
}
