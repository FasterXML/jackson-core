
/*
 * @(#)FastDoubleParserNumericallyGeneratedTest.java
 * Copyright © 2021. Werner Randelshofer, Switzerland. MIT License.
 */

package com.fasterxml.jackson.core.io.doubleparser;

public class FastFloatParserFromCharSequenceNumericallyGeneratedTest extends AbstractFloatNumericallyGeneratedTest {
    @Override
    protected float parse(String str) {
        return FastFloatParser.parseFloat(str);
    }
}
