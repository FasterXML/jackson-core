package tools.jackson.core.unittest.io.schubfach;

import java.nio.charset.StandardCharsets;
import java.util.Random;

import org.junit.jupiter.api.Test;

import tools.jackson.core.io.NumberOutput;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link NumberOutput#outputFloat(float, char[], int)} and
 * {@link NumberOutput#outputDouble(double, char[], int)} which write directly
 * to a char buffer, avoiding String allocation.
 */
public class SchubfachWriteCharBufferTest
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
            _verifyFloat(v);
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
            _verifyDouble(v);
        }
    }

    @Test
    public void testWriteRandom()
    {
        Random rnd = new Random(2468);
        for (int i = 0; i < 10_000; ++i) {
            _verifyFloat(Float.intBitsToFloat(rnd.nextInt()));
            _verifyDouble(Double.longBitsToDouble(rnd.nextLong()));
        }
    }

    @Test
    public void testWriteFloatAtOffset()
    {
        char[] buf = new char[32];
        buf[0] = ',';
        int end = NumberOutput.outputFloat(1.5f, buf, 1);
        assertEquals("1.5", new String(buf, 1, end - 1));
        assertEquals(',', buf[0]);
    }

    @Test
    public void testWriteDoubleAtOffset()
    {
        char[] buf = new char[48];
        buf[0] = ',';
        int end = NumberOutput.outputDouble(1.5, buf, 1);
        assertEquals("1.5", new String(buf, 1, end - 1));
        assertEquals(',', buf[0]);
    }

    @Test
    public void testMaxLengthFitsInBuffer()
    {
        // Longest forms: exactly MAX_*_BYTES chars, must not overflow a tight buffer
        char[] buf = new char[NumberOutput.MAX_DOUBLE_BYTES];
        int end = NumberOutput.outputDouble(-Double.MIN_NORMAL, buf, 0);
        assertTrue(end <= NumberOutput.MAX_DOUBLE_BYTES);
        assertEquals(NumberOutput.toString(-Double.MIN_NORMAL, true), new String(buf, 0, end));

        buf = new char[NumberOutput.MAX_FLOAT_BYTES];
        end = NumberOutput.outputFloat(-Float.MIN_NORMAL, buf, 0);
        assertTrue(end <= NumberOutput.MAX_FLOAT_BYTES);
        assertEquals(NumberOutput.toString(-Float.MIN_NORMAL, true), new String(buf, 0, end));
    }

    private void _verifyFloat(float v)
    {
        String expected = NumberOutput.toString(v, true);
        char[] buf = new char[32];
        int end = NumberOutput.outputFloat(v, buf, 0);
        assertEquals(expected, new String(buf, 0, end), "mismatch for float " + v);
        // and must agree with the byte[] variant
        byte[] bbuf = new byte[32];
        int bend = NumberOutput.outputFloat(v, bbuf, 0);
        assertEquals(expected, new String(bbuf, 0, bend, StandardCharsets.US_ASCII),
                "byte[] mismatch for float " + v);
    }

    private void _verifyDouble(double v)
    {
        String expected = NumberOutput.toString(v, true);
        char[] buf = new char[48];
        int end = NumberOutput.outputDouble(v, buf, 0);
        assertEquals(expected, new String(buf, 0, end), "mismatch for double " + v);
        byte[] bbuf = new byte[48];
        int bend = NumberOutput.outputDouble(v, bbuf, 0);
        assertEquals(expected, new String(bbuf, 0, bend, StandardCharsets.US_ASCII),
                "byte[] mismatch for double " + v);
    }
}
