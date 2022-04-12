// Copyright 2018 Ulf Adams
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.fasterxml.jackson.core.io.ryu;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * A few tests to check that {@link DoubleUtils} isn't completely broken.
 *
 * <p>Note that the implementation does not follow Java {@link Double#toString} semantics, so we
 * can't run the existing double-to-string tests against it.
 */
@RunWith(JUnit4.class)
public class JafferDoubleTest {
  private String f(double f) {
    return DoubleUtils.toString(f);
  }

  @Test
  public void simpleCases() {
    assertEquals("0", f(0));
    assertEquals("0", f(Double.longBitsToDouble(0x8000000000000000L)));
    assertEquals("1", f(1.0d));
    assertEquals("-1", f(-1.0d));
    assertEquals("NaN", f(Double.NaN));
    assertEquals("Infinity", f(Double.POSITIVE_INFINITY));
    assertEquals("-Infinity", f(Double.NEGATIVE_INFINITY));
  }

  @Test
  public void switchToSubnormal() {
    assertEquals("2.2250738585072014e-308", f(Double.longBitsToDouble(0x0010000000000000L)));
  }

  @Test
  public void boundaryConditions() {
    // x = 1.0E7
    assertEquals("10000000", f(1.0E7d));
    // x < 1.0E7
    assertEquals("9999999", f(9999999d));
    // x = 1.0E-3
    assertEquals("0.001", f(0.001d));
    // x < 1.0E-3
    assertEquals("0.0009", f(0.0009d));
  }

  @Test
  public void regressionTest() {
    assertEquals("4.940656e-318", f(4.940656E-318d));
    assertEquals("1.18575755e-316", f(1.18575755E-316d));
    assertEquals("2.989102097996e-312", f(2.989102097996E-312d));
    assertEquals("9.0608011534336e15", f(9.0608011534336E15d));
    assertEquals("4.708356024711512e18", f(4.708356024711512E18));
    assertEquals("9.409340012568248e18", f(9.409340012568248E18));
  }
}
