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

/**
 * Computes and outputs the lookup table sizes.
 */
public class ComputeTableSizes {
  private static final long LOG10_2_DENOMINATOR = 10000000L;
  private static final long LOG10_2_NUMERATOR = (long) (LOG10_2_DENOMINATOR * Math.log10(2));

  private static final long LOG10_5_DENOMINATOR = 10000000L;
  private static final long LOG10_5_NUMERATOR = (long) (LOG10_5_DENOMINATOR * Math.log10(5));

  public static void main(String[] args) {
    boolean verbose = false;
    for (String s : args) {
      if ("-v".equals(s)) {
        verbose = true;
      }
    }

    if (verbose) {
      System.out.println("These are the lookup table sizes required for each floating point type:");
    } else {
      System.out.println("floating_point_type,table_size_1,table_size_2,total_size");
    }
    for (FloatingPointFormat format : FloatingPointFormat.values()) {
      int minE2 = 1 - format.bias() - format.mantissaBits() - 2;
      int maxE2 = ((1 << format.exponentBits()) - 2) - format.bias() - format.mantissaBits() - 2;
      long minQ, maxQ;
      if (format == FloatingPointFormat.FLOAT32) {
        // Float32 is special; using -1 as below is problematic as it would require 33 bit
        // integers. Instead, we compute the additionally required digit separately.
        minQ = (-minE2 * LOG10_5_NUMERATOR) / LOG10_5_DENOMINATOR;
        maxQ = (maxE2 * LOG10_2_NUMERATOR) / LOG10_2_DENOMINATOR;
      } else {
        minQ = Math.max(0, (-minE2 * LOG10_5_NUMERATOR) / LOG10_5_DENOMINATOR - 1);
        maxQ = Math.max(0, (maxE2 * LOG10_2_NUMERATOR) / LOG10_2_DENOMINATOR - 1);
      }
      long negTableSize = (-minE2 - minQ) + 1;
      long posTableSize = maxQ + 1;
      if (verbose) {
        System.out.println(format);
        System.out.println("  " + minE2 + " <= e_2 <= " + maxE2);
        System.out.println("  " + (-minQ) + " <= q <= " + maxQ);
        System.out.println("  Total table size = " + negTableSize + " + " + posTableSize + " = " + (negTableSize + posTableSize));
      } else {
        System.out.println(format + "," + negTableSize + "," + posTableSize + "," + (negTableSize + posTableSize));
      }
    }
  }
}
