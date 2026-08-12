package tools.jackson.core.unittest.io.schubfach;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import tools.jackson.core.io.NumberOutput;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link NumberOutput#outputFloat} and {@link NumberOutput#outputDouble}
 * which write directly to a byte buffer, avoiding String allocation.
 */
public class SchubfachWriteBufferTest
{
    @Test
    public void testWriteFloatBasic()
    {
        float[] values = {
            0.0f, -0.0f, 1.0f, -1.0f, 1.5f, -1.5f,
            123.456f, -123.456f, 1.0E10f, 1.0E-10f,
            Float.MAX_VALUE, Float.MIN_VALUE, Float.MIN_NORMAL,
            Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY
        };
        for (float v : values) {
            String expected = NumberOutput.toString(v, true);
            byte[] buf = new byte[32];
            int end = NumberOutput.outputFloat(v, buf, 0);
            String actual = new String(buf, 0, end, StandardCharsets.ISO_8859_1);
            assertEquals(expected, actual, "mismatch for float " + v);
        }
    }

    @Test
    public void testWriteDoubleBasic()
    {
        double[] values = {
            0.0, -0.0, 1.0, -1.0, 1.5, -1.5,
            123.456789, -123.456789, 1.0E10, 1.0E-10,
            Double.MAX_VALUE, Double.MIN_VALUE, Double.MIN_NORMAL,
            Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY
        };
        for (double v : values) {
            String expected = NumberOutput.toString(v, true);
            byte[] buf = new byte[48];
            int end = NumberOutput.outputDouble(v, buf, 0);
            String actual = new String(buf, 0, end, StandardCharsets.ISO_8859_1);
            assertEquals(expected, actual, "mismatch for double " + v);
        }
    }

    @Test
    public void testWriteFloatAtOffset()
    {
        byte[] buf = new byte[32];
        buf[0] = (byte) ',';
        int end = NumberOutput.outputFloat(1.5f, buf, 1);
        String result = new String(buf, 1, end - 1, StandardCharsets.ISO_8859_1);
        assertEquals("1.5", result);
    }

    @Test
    public void testWriteDoubleAtOffset()
    {
        byte[] buf = new byte[48];
        buf[0] = (byte) ',';
        int end = NumberOutput.outputDouble(1.5, buf, 1);
        String result = new String(buf, 1, end - 1, StandardCharsets.ISO_8859_1);
        assertEquals("1.5", result);
    }
}
