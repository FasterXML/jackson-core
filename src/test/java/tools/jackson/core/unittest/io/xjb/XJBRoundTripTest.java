package tools.jackson.core.unittest.io.xjb;

import org.junit.jupiter.api.Test;
import tools.jackson.core.io.xjb.XJBWriter;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Round-trip correctness tests for XJB float/double to string conversion.
 * Verifies that parsing the string back yields the exact same bit pattern.
 */
public class XJBRoundTripTest {

  @Test
  public void floatSpecials() {
    float[] floatSpecials = {
        0f, -0f, 1f, -1f, 0.1f, -0.1f, 100f, 123.45f, 1000f, 123000f,
        1.2e23f, 1e10f, 1e-10f, 0.0001f, 0.001f, 0.0123f, 0.000123f,
        Float.MIN_VALUE, Float.MAX_VALUE, Float.MIN_NORMAL,
        -Float.MIN_VALUE, -Float.MAX_VALUE,
        123456789f, 1.23456789E10f, 9999999f, 99999990f,
        Float.intBitsToFloat(1), Float.intBitsToFloat(2),
        Float.intBitsToFloat(0x7f7fffff),
    };
    for (float f : floatSpecials) {
      assertFloatRoundTrip(f);
    }
  }

  @Test
  public void doubleSpecials() {
    double[] doubleSpecials = {
        0d, -0d, 1d, -1d, 0.1d, -0.1d, 100d, 123.45d, 1000d, 123000d,
        1.2e23d, 1e10d, 1e-10d, 0.0001d, 0.001d, 0.0123d, 0.000123d,
        Double.MIN_VALUE, Double.MAX_VALUE, Double.MIN_NORMAL,
        -Double.MIN_VALUE, -Double.MAX_VALUE,
        123456789012345d, 1.23456789012345E100d, 9999999999999999d,
        Double.longBitsToDouble(1L), Double.longBitsToDouble(2L),
        Double.longBitsToDouble(0x7fefffffffffffffL),
        4.9E-324, 2.2250738585072014E-308,
    };
    for (double d : doubleSpecials) {
      assertDoubleRoundTrip(d);
    }
  }

  @Test
  public void floatRandomFinite() {
    Random r = new Random(42);
    for (int i = 0; i < 2_000_000; i++) {
      int bits = r.nextInt();
      float f = Float.intBitsToFloat(bits);
      if (Float.isFinite(f)) {
        assertFloatRoundTrip(f);
      }
    }
  }

  @Test
  public void doubleRandomFinite() {
    Random r = new Random(42);
    for (int i = 0; i < 2_000_000; i++) {
      long bits = r.nextLong();
      double d = Double.longBitsToDouble(bits);
      if (Double.isFinite(d)) {
        assertDoubleRoundTrip(d);
      }
    }
  }

  @Test
  public void randomNiceDecimals() {
    Random r = new Random(42);
    for (int i = 0; i < 500_000; i++) {
      double d = (r.nextDouble() - 0.5) * Math.pow(10, r.nextInt(60) - 30);
      assertDoubleRoundTrip(d);
      float f = (float) ((r.nextDouble() - 0.5) * Math.pow(10, r.nextInt(60) - 30));
      if (Float.isFinite(f)) {
        assertFloatRoundTrip(f);
      }
    }
  }

  private void assertFloatRoundTrip(float f) {
    String s = XJBWriter.toString(f);
    float roundTripped = Float.parseFloat(s);
    assertEquals(Float.floatToRawIntBits(f), Float.floatToRawIntBits(roundTripped),
        () -> "Round-trip failed for float bits=" + Integer.toHexString(Float.floatToRawIntBits(f))
            + " value=" + f + " string=\"" + s + "\"");
  }

  private void assertDoubleRoundTrip(double d) {
    String s = XJBWriter.toString(d);
    double roundTripped = Double.parseDouble(s);
    assertEquals(Double.doubleToRawLongBits(d), Double.doubleToRawLongBits(roundTripped),
        () -> "Round-trip failed for double bits=" + Long.toHexString(Double.doubleToRawLongBits(d))
            + " value=" + d + " string=\"" + s + "\"");
  }
}
