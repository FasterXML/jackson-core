package com.fasterxml.jackson.core.io.schubfach;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public abstract class DoubleToStringTest {
  abstract String f(double f);

  private void assertD2sEquals(String expected, double f) {
    assertEquals(expected, f(f));
  }

  @Test
  public void simpleCases() {
    assertD2sEquals("0.0", 0);
    assertD2sEquals("-0.0", Double.longBitsToDouble(0x8000000000000000L));
    assertD2sEquals("1.0", 1.0d);
    assertD2sEquals("-1.0", -1.0d);
    assertD2sEquals("NaN", Double.NaN);
    assertD2sEquals("Infinity", Double.POSITIVE_INFINITY);
    assertD2sEquals("-Infinity", Double.NEGATIVE_INFINITY);
  }

  @Test
  public void switchToSubnormal() {
    assertD2sEquals("2.2250738585072014E-308", Double.longBitsToDouble(0x0010000000000000L));
  }

  /**
   * Floating point values in the range 1.0E-3 <= x < 1.0E7 have to be printed
   * without exponent. This test checks the values at those boundaries.
   */
  @Test
  public void boundaryConditions() {
    // x = 1.0E7
    assertD2sEquals("1.0E7", 1.0E7d);
    // x < 1.0E7
    assertD2sEquals("9999999.999999998", 9999999.999999998d);
    // x = 1.0E-3
    assertD2sEquals("0.001", 0.001d);
    // x < 1.0E-3
    assertD2sEquals("9.999999999999998E-4", 0.0009999999999999998d);
  }

  @Test
  public void minAndMax() {
    assertD2sEquals("1.7976931348623157E308", Double.longBitsToDouble(0x7fefffffffffffffL));
    assertD2sEquals("4.9E-324", Double.longBitsToDouble(1));
  }

  @Test
  public void roundingModeEven() {
    assertD2sEquals("-2.109808898695963E16", -2.109808898695963E16);
  }

  @Test
  public void regressionTest() {
    assertD2sEquals("4.940656E-318", 4.940656E-318d);
    assertD2sEquals("1.18575755E-316", 1.18575755E-316d);
    assertD2sEquals("2.989102097996E-312", 2.989102097996E-312d);
    assertD2sEquals("9.0608011534336E15", 9.0608011534336E15d);
    assertD2sEquals("4.708356024711512E18", 4.708356024711512E18);
    assertD2sEquals("9.409340012568248E18", 9.409340012568248E18);
    // This number naively requires 65 bit for the intermediate results if we reduce the lookup
    // table by half. This checks that we don't lose any information in that case.
    assertD2sEquals("1.8531501765868567E21", 1.8531501765868567E21);
    assertD2sEquals("-3.347727380279489E33", -3.347727380279489E33);
    // Discovered by Andriy Plokhotnyuk, see #29.
    assertD2sEquals("1.9430376160308388E16", 1.9430376160308388E16);
    assertD2sEquals("-6.9741824662760956E19", -6.9741824662760956E19);
    assertD2sEquals("4.3816050601147837E18", 4.3816050601147837E18);
  }
}
