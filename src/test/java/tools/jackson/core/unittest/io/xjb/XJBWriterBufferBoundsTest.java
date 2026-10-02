package tools.jackson.core.unittest.io.xjb;

import java.util.Random;

import org.junit.jupiter.api.Test;

import tools.jackson.core.io.NumberOutput;
import tools.jackson.core.io.xjb.XJBWriter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that {@link NumberOutput#MAX_DOUBLE_BYTES} / {@link NumberOutput#MAX_FLOAT_BYTES}
 * (and the char[] equivalents) really bound what {@link XJBWriter} touches: the writers use
 * wide (2/4/8-byte) stores, so they can write past the last logical character of the number.
 */
public class XJBWriterBufferBoundsTest
{
    // Longest possible double output: 17 significant digits, sign, and a 3-digit
    // negative exponent -- the case where the exponent digits are written last.
    private final static double LONGEST_DOUBLE = -2.2250738585072014E-308;

    @Test
    public void longestDoubleFitsExactBuffer() {
        assertEquals(24, Double.toString(LONGEST_DOUBLE).length());
        assertDoubleFits(LONGEST_DOUBLE);
        assertDoubleFits(-1.2345678901234567E308);
        assertDoubleFits(1.2345678901234567E-308);
        assertDoubleFits(-Double.MIN_VALUE);
        assertDoubleFits(-Double.MAX_VALUE);
        assertDoubleFits(-Double.MIN_NORMAL);
    }

    @Test
    public void randomDoublesFitExactBuffer() {
        Random r = new Random(1234);
        for (int i = 0; i < 500_000; ++i) {
            assertDoubleFits(Double.longBitsToDouble(r.nextLong()));
        }
    }

    @Test
    public void randomFloatsFitExactBuffer() {
        assertFloatFits(-Float.MIN_VALUE);
        assertFloatFits(-Float.MAX_VALUE);
        assertFloatFits(-Float.MIN_NORMAL);
        Random r = new Random(1234);
        for (int i = 0; i < 500_000; ++i) {
            assertFloatFits(Float.intBitsToFloat(r.nextInt()));
        }
    }

    // Also check the writers do not scribble on bytes past the value they report
    @Test
    public void doubleDoesNotWritePastReturnedOffset() {
        byte[] buf = new byte[NumberOutput.MAX_DOUBLE_BYTES + 16];
        int end = NumberOutput.outputDouble(LONGEST_DOUBLE, buf, 0);
        for (int i = end; i < buf.length; ++i) {
            assertEquals(0, buf[i], "byte at "+i+" (past end "+end+") was modified");
        }
    }

    private void assertDoubleFits(double d) {
        // exact-size buffers: any wide store past the end throws
        try {
            byte[] bytes = new byte[NumberOutput.MAX_DOUBLE_BYTES];
            NumberOutput.outputDouble(d, bytes, 0);
            char[] chars = new char[NumberOutput.MAX_DOUBLE_CHARS];
            NumberOutput.outputDouble(d, chars, 0);
        } catch (IndexOutOfBoundsException e) {
            fail("double "+d+" ("+Double.toString(d)+") overflowed buffer: "+e);
        }
    }

    private void assertFloatFits(float f) {
        try {
            byte[] bytes = new byte[NumberOutput.MAX_FLOAT_BYTES];
            NumberOutput.outputFloat(f, bytes, 0);
            char[] chars = new char[NumberOutput.MAX_FLOAT_CHARS];
            NumberOutput.outputFloat(f, chars, 0);
        } catch (IndexOutOfBoundsException e) {
            fail("float "+f+" ("+Float.toString(f)+") overflowed buffer: "+e);
        }
    }
}
