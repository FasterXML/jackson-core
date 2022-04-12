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

import com.fasterxml.jackson.core.io.ryu.RyuFloat;

/**
 * Exhaustively tests the fast implementation of Ryu against the slow one.
 */
public class ExhaustiveFloatComparison {
  public static void main(String[] args) {
    System.out.println("This checks every possible 32-bit floating point value, which takes ~60 hours.");
    System.out.println("We report progress approximately every ~10 seconds.");
    long stride = 10007L; // 10007 is prime
    // We go through the set of all possible values in stride-sized steps, and go through all
    // possible [0,stride) offsets. This gives us good coverage across the whole range of floating
    // point numbers even in the first loop, which quickly finds systematic errors. We use a prime
    // to avoid bit patterns in the floating point number.
    //
    // We also don't check any negative numbers; we assume that the code behaves identically.
    for (long base = 0; base < stride; base++) {
      for (long l = base; l <= 0x7fffffffL; l += stride) {
        float f = Float.intBitsToFloat((int) l);
        String expected = SlowConversion.floatToString(f);
        String actual = RyuFloat.floatToString(f);
        if (!expected.equals(actual)) {
          System.out.println(String.format("expected %s, but was %s", expected, actual));
          throw new RuntimeException(String.format("expected %s, but was %s", expected, actual));
        }

        // Also check round-trip safety.
        long g = Float.floatToRawIntBits(Float.parseFloat(actual)) & 0xffffffffL;
        if (!Float.isNaN(f) && g != l) {
          String message = String.format("expected %d, but was %d", Long.valueOf(l), Long.valueOf(g));
          System.out.println(message);
          throw new RuntimeException(message);
        }
      }
      double frac = (base + 1) / (double) stride;
      System.out.printf("(%6.2f%%)\n", Double.valueOf(100 * frac));
    }
  }
}
