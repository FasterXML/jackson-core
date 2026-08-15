package tools.jackson.core.unittest.io.xjb;

import java.util.Random;

import org.junit.jupiter.api.Test;

import tools.jackson.core.io.xjb.XJBWriter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the char[] overloads of {@link XJBWriter#writeFloat(float, char[], int)}
 * and {@link XJBWriter#writeDouble(double, char[], int)}.
 *
 * Verifies that the char[] path produces identical output to the byte[] path (toString),
 * and that the returned position is correct.
 */
public class XJBWriterCharTest
{
    // ------------------------------------------------------------------
    // Float: char[] output matches toString
    // ------------------------------------------------------------------

    @Test
    public void floatSimpleCases() {
        assertFloatCharMatchesToString(0f);
        assertFloatCharMatchesToString(-0f);
        assertFloatCharMatchesToString(1f);
        assertFloatCharMatchesToString(-1f);
        // NaN and Infinity throw ArithmeticException in write methods;
        // they are handled by toString() separately
        assertThrows(ArithmeticException.class, () -> XJBWriter.writeFloat(Float.NaN, new char[48], 0));
        assertThrows(ArithmeticException.class, () -> XJBWriter.writeFloat(Float.POSITIVE_INFINITY, new char[48], 0));
        assertThrows(ArithmeticException.class, () -> XJBWriter.writeFloat(Float.NEGATIVE_INFINITY, new char[48], 0));
    }

    @Test
    public void floatBoundaryConditions() {
        // x = 1.0E7
        assertFloatCharMatchesToString(1.0E7f);
        // x < 1.0E7
        assertFloatCharMatchesToString(9999999.0f);
        // x = 1.0E-3
        assertFloatCharMatchesToString(0.001f);
        // x < 1.0E-3
        assertFloatCharMatchesToString(0.0009999999f);
    }

    @Test
    public void floatMinMax() {
        assertFloatCharMatchesToString(Float.MAX_VALUE);
        assertFloatCharMatchesToString(Float.MIN_VALUE);
        assertFloatCharMatchesToString(Float.MIN_NORMAL);
        assertFloatCharMatchesToString(-Float.MAX_VALUE);
        assertFloatCharMatchesToString(-Float.MIN_VALUE);
    }

    @Test
    public void floatSubnormals() {
        assertFloatCharMatchesToString(Float.intBitsToFloat(0x00800000));
        assertFloatCharMatchesToString(Float.intBitsToFloat(0x00000001));
        assertFloatCharMatchesToString(Float.intBitsToFloat(0x00000002));
        assertFloatCharMatchesToString(Float.intBitsToFloat(0x007fffff));
    }

    @Test
    public void floatRegression() {
        assertFloatCharMatchesToString(4.7223665E21f);
        assertFloatCharMatchesToString(8388608.0f);
        assertFloatCharMatchesToString(1.6777216E7f);
        assertFloatCharMatchesToString(3.3554436E7f);
        assertFloatCharMatchesToString(6.7131496E7f);
        assertFloatCharMatchesToString(1.9310392E-38f);
        assertFloatCharMatchesToString(-2.47E-43f);
        assertFloatCharMatchesToString(1.993244E-38f);
        assertFloatCharMatchesToString(4103.9004f);
        assertFloatCharMatchesToString(5.3399997E9f);
        assertFloatCharMatchesToString(6.0898E-39f);
        assertFloatCharMatchesToString(0.0010310042f);
        assertFloatCharMatchesToString(2.8823261E17f);
        assertFloatCharMatchesToString(7.038531E-26f);
        assertFloatCharMatchesToString(9.2234038E17f);
        assertFloatCharMatchesToString(6.7108872E7f);
        assertFloatCharMatchesToString(1.0E-44f);
        assertFloatCharMatchesToString(2.816025E14f);
        assertFloatCharMatchesToString(9.223372E18f);
        assertFloatCharMatchesToString(1.5846085E29f);
        assertFloatCharMatchesToString(1.1811161E19f);
        assertFloatCharMatchesToString(5.368709E18f);
        assertFloatCharMatchesToString(4.6143165E18f);
        assertFloatCharMatchesToString(0.007812537f);
        assertFloatCharMatchesToString(1.4E-45f);
        assertFloatCharMatchesToString(1.18697724E20f);
        assertFloatCharMatchesToString(1.00014165E-36f);
        assertFloatCharMatchesToString(200f);
        assertFloatCharMatchesToString(3.3554432E7f);
    }

    @Test
    public void floatRounding() {
        assertFloatCharMatchesToString(3.3554448E7f);
        assertFloatCharMatchesToString(8.999999E9f);
        assertFloatCharMatchesToString(3.4366717E10f);
        assertFloatCharMatchesToString(0.33007812f);
    }

    @Test
    public void floatLooksLikePow5() {
        assertFloatCharMatchesToString(Float.intBitsToFloat(0x5D1502F9));
        assertFloatCharMatchesToString(Float.intBitsToFloat(0x5D9502F9));
        assertFloatCharMatchesToString(Float.intBitsToFloat(0x5E1502F9));
    }

    @Test
    public void floatWithOffset() {
        float[] values = {0f, -0f, 1f, -1f, 3.14f, -3.14f, 1.0E7f, 0.001f,
                Float.MAX_VALUE, Float.MIN_VALUE};
        for (float f : values) {
            for (int offset = 0; offset <= 4; offset++) {
                assertFloatCharWithOffset(f, offset);
            }
        }
        // NaN and Infinity throw in write methods
        assertThrows(ArithmeticException.class, () -> XJBWriter.writeFloat(Float.NaN, new char[52], 4));
        assertThrows(ArithmeticException.class, () -> XJBWriter.writeFloat(Float.POSITIVE_INFINITY, new char[52], 4));
    }

    @Test
    public void floatRoundTripViaCharArray() {
        float[] specials = {
            0f, -0f, 1f, -1f, 0.1f, -0.1f, 100f, 123.45f, 1000f, 123000f,
            1.2e23f, 1e10f, 1e-10f, 0.0001f, 0.001f, 0.0123f, 0.000123f,
            Float.MIN_VALUE, Float.MAX_VALUE, Float.MIN_NORMAL,
            -Float.MIN_VALUE, -Float.MAX_VALUE,
            123456789f, 1.23456789E10f, 9999999f, 99999990f,
            Float.intBitsToFloat(1), Float.intBitsToFloat(2),
            Float.intBitsToFloat(0x7f7fffff),
        };
        for (float f : specials) {
            assertFloatRoundTrip(f);
        }
    }

    @Test
    public void floatRoundTripRandom() {
        Random r = new Random(42);
        for (int i = 0; i < 500_000; i++) {
            int bits = r.nextInt();
            float f = Float.intBitsToFloat(bits);
            if (Float.isFinite(f)) {
                assertFloatRoundTrip(f);
            }
        }
    }

    // ------------------------------------------------------------------
    // Double: char[] output matches toString
    // ------------------------------------------------------------------

    @Test
    public void doubleSimpleCases() {
        assertDoubleCharMatchesToString(0d);
        assertDoubleCharMatchesToString(-0d);
        assertDoubleCharMatchesToString(1d);
        assertDoubleCharMatchesToString(-1d);
        // NaN and Infinity throw ArithmeticException in write methods
        assertThrows(ArithmeticException.class, () -> XJBWriter.writeDouble(Double.NaN, new char[48], 0));
        assertThrows(ArithmeticException.class, () -> XJBWriter.writeDouble(Double.POSITIVE_INFINITY, new char[48], 0));
        assertThrows(ArithmeticException.class, () -> XJBWriter.writeDouble(Double.NEGATIVE_INFINITY, new char[48], 0));
    }

    @Test
    public void doubleBoundaryConditions() {
        assertDoubleCharMatchesToString(1.0E7d);
        assertDoubleCharMatchesToString(9999999.999999998d);
        assertDoubleCharMatchesToString(0.001d);
        assertDoubleCharMatchesToString(0.0009999999999999998d);
    }

    @Test
    public void doubleMinMax() {
        assertDoubleCharMatchesToString(Double.MAX_VALUE);
        assertDoubleCharMatchesToString(Double.MIN_VALUE);
        assertDoubleCharMatchesToString(Double.MIN_NORMAL);
        assertDoubleCharMatchesToString(-Double.MAX_VALUE);
        assertDoubleCharMatchesToString(-Double.MIN_VALUE);
    }

    @Test
    public void doubleSubnormals() {
        assertDoubleCharMatchesToString(Double.longBitsToDouble(0x0010000000000000L));
        assertDoubleCharMatchesToString(Double.longBitsToDouble(1L));
        assertDoubleCharMatchesToString(Double.longBitsToDouble(2L));
        assertDoubleCharMatchesToString(Double.longBitsToDouble(0x000FFFFFFFFFFFFFL));
    }

    @Test
    public void doubleRegression() {
        assertDoubleCharMatchesToString(4.940656E-318d);
        assertDoubleCharMatchesToString(1.18575755E-316d);
        assertDoubleCharMatchesToString(2.989102097996E-312d);
        assertDoubleCharMatchesToString(9.0608011534336E15d);
        assertDoubleCharMatchesToString(4.708356024711512E18);
        assertDoubleCharMatchesToString(9.409340012568248E18);
        assertDoubleCharMatchesToString(1.8531501765868567E21);
        assertDoubleCharMatchesToString(-3.347727380279489E33);
        assertDoubleCharMatchesToString(1.9430376160308388E16);
        assertDoubleCharMatchesToString(-6.9741824662760956E19);
        assertDoubleCharMatchesToString(4.3816050601147837E18);
    }

    @Test
    public void doubleRounding() {
        assertDoubleCharMatchesToString(-2.109808898695963E16);
    }

    @Test
    public void doubleWithOffset() {
        double[] values = {0d, -0d, 1d, -1d, 3.14d, -3.14d, 1.0E7d, 0.001d,
                Double.MAX_VALUE, Double.MIN_VALUE, 4.9E-324, 2.2250738585072014E-308};
        for (double d : values) {
            for (int offset = 0; offset <= 4; offset++) {
                assertDoubleCharWithOffset(d, offset);
            }
        }
        // NaN and Infinity throw in write methods
        assertThrows(ArithmeticException.class, () -> XJBWriter.writeDouble(Double.NaN, new char[52], 4));
        assertThrows(ArithmeticException.class, () -> XJBWriter.writeDouble(Double.POSITIVE_INFINITY, new char[52], 4));
    }

    @Test
    public void doubleRoundTripViaCharArray() {
        double[] specials = {
            0d, -0d, 1d, -1d, 0.1d, -0.1d, 100d, 123.45d, 1000d, 123000d,
            1.2e23d, 1e10d, 1e-10d, 0.0001d, 0.001d, 0.0123d, 0.000123d,
            Double.MIN_VALUE, Double.MAX_VALUE, Double.MIN_NORMAL,
            -Double.MIN_VALUE, -Double.MAX_VALUE,
            123456789012345d, 1.23456789012345E100d, 9999999999999999d,
            Double.longBitsToDouble(1L), Double.longBitsToDouble(2L),
            Double.longBitsToDouble(0x7fefffffffffffffL),
            4.9E-324, 2.2250738585072014E-308,
        };
        for (double d : specials) {
            assertDoubleRoundTrip(d);
        }
    }

    @Test
    public void doubleRoundTripRandom() {
        Random r = new Random(42);
        for (int i = 0; i < 500_000; i++) {
            long bits = r.nextLong();
            double d = Double.longBitsToDouble(bits);
            if (Double.isFinite(d)) {
                assertDoubleRoundTrip(d);
            }
        }
    }

    @Test
    public void doubleRoundTripNiceDecimals() {
        Random r = new Random(42);
        for (int i = 0; i < 200_000; i++) {
            double d = (r.nextDouble() - 0.5) * Math.pow(10, r.nextInt(60) - 30);
            assertDoubleRoundTrip(d);
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void assertFloatCharMatchesToString(float f) {
        String expected = XJBWriter.toString(f);
        char[] buf = new char[48];
        int pos = XJBWriter.writeFloat(f, buf, 0);
        String actual = new String(buf, 0, pos);
        assertEquals(expected, actual,
            () -> String.format("float %s (bits=0x%08X): toString='%s', char[]='%s'",
                f, Float.floatToRawIntBits(f), expected, actual));
    }

    private void assertDoubleCharMatchesToString(double d) {
        String expected = XJBWriter.toString(d);
        char[] buf = new char[48];
        int pos = XJBWriter.writeDouble(d, buf, 0);
        String actual = new String(buf, 0, pos);
        assertEquals(expected, actual,
            () -> String.format("double %s (bits=0x%016X): toString='%s', char[]='%s'",
                d, Double.doubleToRawLongBits(d), expected, actual));
    }

    private void assertFloatCharWithOffset(float f, int offset) {
        String expected = XJBWriter.toString(f);
        char[] buf = new char[offset + 48];
        // Fill offset area with sentinel to detect overwrites
        for (int i = 0; i < offset; i++) {
            buf[i] = '$';
        }
        int pos = XJBWriter.writeFloat(f, buf, offset);
        String actual = new String(buf, offset, pos - offset);
        assertEquals(expected, actual,
            () -> String.format("float %s with offset %d: expected='%s', got='%s'", f, offset, expected, actual));
        // Verify offset area not corrupted
        for (int i = 0; i < offset; i++) {
            assertEquals('$', buf[i], "Offset area corrupted at position " + i);
        }
    }

    private void assertDoubleCharWithOffset(double d, int offset) {
        String expected = XJBWriter.toString(d);
        char[] buf = new char[offset + 48];
        for (int i = 0; i < offset; i++) {
            buf[i] = '$';
        }
        int pos = XJBWriter.writeDouble(d, buf, offset);
        String actual = new String(buf, offset, pos - offset);
        assertEquals(expected, actual,
            () -> String.format("double %s with offset %d: expected='%s', got='%s'", d, offset, expected, actual));
        for (int i = 0; i < offset; i++) {
            assertEquals('$', buf[i], "Offset area corrupted at position " + i);
        }
    }

    private void assertFloatRoundTrip(float f) {
        char[] buf = new char[48];
        int pos = XJBWriter.writeFloat(f, buf, 0);
        String s = new String(buf, 0, pos);
        float roundTripped = Float.parseFloat(s);
        assertEquals(Float.floatToRawIntBits(f), Float.floatToRawIntBits(roundTripped),
            () -> "Round-trip failed for float bits=" + Integer.toHexString(Float.floatToRawIntBits(f))
                + " value=" + f + " string=\"" + s + "\"");
    }

    private void assertDoubleRoundTrip(double d) {
        char[] buf = new char[48];
        int pos = XJBWriter.writeDouble(d, buf, 0);
        String s = new String(buf, 0, pos);
        double roundTripped = Double.parseDouble(s);
        assertEquals(Double.doubleToRawLongBits(d), Double.doubleToRawLongBits(roundTripped),
            () -> "Round-trip failed for double bits=" + Long.toHexString(Double.doubleToRawLongBits(d))
                + " value=" + d + " string=\"" + s + "\"");
    }
}
