package tools.jackson.core.unittest.io.xjb;

import tools.jackson.core.io.xjb.XjbDtoa;

import java.util.Random;

public class XjbDtoaTest {
    static int floatFail = 0, doubleFail = 0;
    static long floatCount = 0, doubleCount = 0;

    public static void main(String[] args) {
        // Special / edge values
        float[] floatSpecials = {
                0f, -0f, 1f, -1f, 0.1f, -0.1f, 100f, 123.45f, 1000f, 123000f,
                1.2e23f, 1e10f, 1e-10f, 0.0001f, 0.001f, 0.0123f, 0.000123f,
                Float.MIN_VALUE, Float.MAX_VALUE, Float.MIN_NORMAL,
                -Float.MIN_VALUE, -Float.MAX_VALUE,
                123456789f, 1.23456789E10f, 9999999f, 99999990f,
                Float.intBitsToFloat(1), Float.intBitsToFloat(2),
                Float.intBitsToFloat(0x7f7fffff), // max finite
        };
        for (float f : floatSpecials) checkFloat(f);

        double[] doubleSpecials = {
                0d, -0d, 1d, -1d, 0.1d, -0.1d, 100d, 123.45d, 1000d, 123000d,
                1.2e23d, 1e10d, 1e-10d, 0.0001d, 0.001d, 0.0123d, 0.000123d,
                Double.MIN_VALUE, Double.MAX_VALUE, Double.MIN_NORMAL,
                -Double.MIN_VALUE, -Double.MAX_VALUE,
                123456789012345d, 1.23456789012345E100d, 9999999999999999d,
                Double.longBitsToDouble(1L), Double.longBitsToDouble(2L),
                Double.longBitsToDouble(0x7fefffffffffffffL), // max finite
                4.9E-324, 2.2250738585072014E-308,
        };
        for (double d : doubleSpecials) checkDouble(d);

        // NaN / Infinity should throw
        checkThrows(Float.NaN);
        checkThrows(Float.POSITIVE_INFINITY);
        checkThrows(Float.NEGATIVE_INFINITY);
        checkThrowsD(Double.NaN);
        checkThrowsD(Double.POSITIVE_INFINITY);
        checkThrowsD(Double.NEGATIVE_INFINITY);

        // Random floats (all bit patterns, finite only)
        Random r = new Random(42);
        for (int i = 0; i < 2_000_000; i++) {
            int bits = r.nextInt();
            float f = Float.intBitsToFloat(bits);
            if (Float.isFinite(f)) checkFloat(f);
        }

        // Random doubles
        for (int i = 0; i < 2_000_000; i++) {
            long bits = r.nextLong();
            double d = Double.longBitsToDouble(bits);
            if (Double.isFinite(d)) checkDouble(d);
        }

        // Random "nice" decimal-ish doubles/floats (smaller magnitude, more realistic)
        for (int i = 0; i < 500_000; i++) {
            double d = (r.nextDouble() - 0.5) * Math.pow(10, r.nextInt(60) - 30);
            checkDouble(d);
            float f = (float) ((r.nextDouble() - 0.5) * Math.pow(10, r.nextInt(60) - 30));
            if (Float.isFinite(f)) checkFloat(f);
        }

        System.out.println("floatCount=" + floatCount + " floatFail(roundtrip)=" + floatFail + " floatStrMismatch(non-canonical)=" + floatStrMismatch);
        System.out.println("doubleCount=" + doubleCount + " doubleFail(roundtrip)=" + doubleFail + " doubleStrMismatch(non-canonical)=" + doubleStrMismatch);
        if (floatFail == 0 && doubleFail == 0) {
            System.out.println("ALL PASSED");
        } else {
            System.out.println("FAILURES FOUND");
            System.exit(1);
        }
    }

    static int floatStrMismatch = 0, doubleStrMismatch = 0;

    static void checkFloat(float f) {
        floatCount++;
        String expected = Float.toString(f);
        String actual;
        try {
            actual = XjbDtoa.toString(f);
        } catch (Exception e) {
            floatFail++;
            if (floatFail < 20) System.out.println("EXCEPTION for float " + f + " bits=" + Integer.toHexString(Float.floatToRawIntBits(f)) + ": " + e);
            return;
        }
        // Primary correctness bar: round-trips to the exact same bit pattern.
        float roundTripped = Float.parseFloat(actual);
        if (Float.floatToRawIntBits(roundTripped) != Float.floatToRawIntBits(f)) {
            floatFail++;
            if (floatFail < 40) {
                System.out.println("ROUND-TRIP FAIL float bits=" + Integer.toHexString(Float.floatToRawIntBits(f))
                        + " value=" + f + " expected=" + expected + " actual=" + actual);
            }
        } else if (!expected.equals(actual)) {
            floatStrMismatch++;
            if (floatStrMismatch < 20) {
                System.out.println("(non-canonical but round-trips) float bits=" + Integer.toHexString(Float.floatToRawIntBits(f))
                        + " value=" + f + " expected=" + expected + " actual=" + actual);
            }
        }
    }

    static void checkDouble(double d) {
        doubleCount++;
        String expected = Double.toString(d);
        String actual;
        try {
            actual = XjbDtoa.toString(d);
        } catch (Exception e) {
            doubleFail++;
            if (doubleFail < 20) System.out.println("EXCEPTION for double " + d + " bits=" + Long.toHexString(Double.doubleToRawLongBits(d)) + ": " + e);
            return;
        }
        double roundTripped = Double.parseDouble(actual);
        if (Double.doubleToRawLongBits(roundTripped) != Double.doubleToRawLongBits(d)) {
            doubleFail++;
            if (doubleFail < 40) {
                System.out.println("ROUND-TRIP FAIL double bits=" + Long.toHexString(Double.doubleToRawLongBits(d))
                        + " value=" + d + " expected=" + expected + " actual=" + actual);
            }
        } else if (!expected.equals(actual)) {
            doubleStrMismatch++;
            if (doubleStrMismatch < 20) {
                System.out.println("(non-canonical but round-trips) double bits=" + Long.toHexString(Double.doubleToRawLongBits(d))
                        + " value=" + d + " expected=" + expected + " actual=" + actual);
            }
        }
    }

    // Float.toString/Double.toString always use exponent form like "E23" or scientific with 'E',
    // and always include a decimal point even for "1.0". jsoniter-scala's xjb port also always
    // includes a decimal point and uses 'E' for exponent (per code: 0x2D45 = "E-", 0x2E00 via '.').
    // We compare numerically to be robust to trivial formatting differences (e.g. exponent digit count),
    // by parsing both back into the same type and comparing bits, AND we also compare the raw strings
    // when styles should match exactly.
    static String normalizeFloat(String s) {
        return s;
    }
    static String normalizeDouble(String s) {
        return s;
    }

    static void checkThrows(float f) {
        try {
            XjbDtoa.toString(f);
            System.out.println("FAIL: expected exception for float " + f);
            floatFail++;
        } catch (Exception e) {
            // expected
        }
    }

    static void checkThrowsD(double d) {
        try {
            XjbDtoa.toString(d);
            System.out.println("FAIL: expected exception for double " + d);
            doubleFail++;
        } catch (Exception e) {
            // expected
        }
    }
}
