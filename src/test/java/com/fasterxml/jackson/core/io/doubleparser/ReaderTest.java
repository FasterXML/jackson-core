package com.fasterxml.jackson.core.io.doubleparser;

import ch.randelshofer.fastdoubleparser.JavaDoubleParser;
import ch.randelshofer.fastdoubleparser.JavaFloatParser;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReaderTest {
    private static final int LEN = 1000;
    private static final String[] DOUBLE_STRINGS = new String[LEN];
    private static final String[] FLOAT_STRINGS = new String[LEN];

    static {
        Random rnd = new Random();
        for (int i = 0; i < LEN; i++) {
            DOUBLE_STRINGS[i] = Double.toString(rnd.nextDouble());
            FLOAT_STRINGS[i] = Float.toString(rnd.nextFloat());
        }
    }

    @Test
    void verifyDoubles() {
        for (int i = 0; i < LEN; i++) {
            double fd = JavaDoubleParser.parseDouble(DOUBLE_STRINGS[i]);
            double jd = Double.parseDouble(DOUBLE_STRINGS[i]);
            assertEquals(jd, fd);
        }
    }

    @Test
    void verifyFloats() {
        for (int i = 0; i < LEN; i++) {
            float ff = JavaFloatParser.parseFloat(FLOAT_STRINGS[i]);
            float jf = Float.parseFloat(FLOAT_STRINGS[i]);
            assertEquals(jf, ff);
        }
    }

}