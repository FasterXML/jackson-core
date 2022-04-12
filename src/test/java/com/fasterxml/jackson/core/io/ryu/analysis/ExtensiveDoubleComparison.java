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

package com.fasterxml.jackson.core.io.ryu.analysis;

import com.fasterxml.jackson.core.io.ryu.RyuDouble;

/**
 * Extensively tests the fast implementation of Ryu against the slow one.
 */
public class ExtensiveDoubleComparison {
  public static void main(String[] args) {
    checkDoubleFastAgainstSlow();
  }

  private static void checkDoubleFastAgainstSlow() {
    System.out.println("This checks every possible 64-bit floating point value - interrupt when you are satisfied.");
    long stride = 1000000000000000L;
    for (long base = 0; base < stride; base++) {
      for (long l = base; l <= 0x7fffffffffffffffL - stride + 1; l += stride) {
        double d = Double.longBitsToDouble(l);
        String expected = RyuDouble.doubleToString(d);
        String actual = SlowConversion.doubleToString(d);
        if (!expected.equals(actual)) {
          System.out.println(String.format("expected %s, but was %s", expected, actual));
          throw new RuntimeException(String.format("expected %s, but was %s", expected, actual));
        }
      }
      double frac = (base + 1) / (double) stride;
      System.out.printf("(%16.12f%%)\n", Double.valueOf(100 * frac));
    }
  }
}
