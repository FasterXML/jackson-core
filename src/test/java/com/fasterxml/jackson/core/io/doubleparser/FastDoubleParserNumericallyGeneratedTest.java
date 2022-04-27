
/*
 * @(#)FastDoubleParserNumericallyGeneratedTest.java
 * Copyright © 2021. Werner Randelshofer, Switzerland. MIT License.
 */

package com.fasterxml.jackson.core.io.doubleparser;

public class FastDoubleParserNumericallyGeneratedTest extends AbstractNumericallyGeneratedTest {
    @Override
    protected double parse(String str) {
        return FastDoubleParser.parseDouble(str);
    }
}
