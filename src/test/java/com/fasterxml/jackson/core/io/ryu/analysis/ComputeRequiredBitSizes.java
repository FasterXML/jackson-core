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

import java.math.BigInteger;
import java.util.EnumSet;

/**
 * Computes appropriate values for B_0 and B_1 for a given floating point type.
 */
public final class ComputeRequiredBitSizes {
  private static final BigInteger FIVE = BigInteger.valueOf(5);
  private static final BigInteger TWO = BigInteger.valueOf(2);

  private static final long LOG10_5_DENOMINATOR = 10000000L;
  private static final long LOG10_5_NUMERATOR = (long) (LOG10_5_DENOMINATOR * Math.log10(5));

  private static final long LOG10_2_DENOMINATOR = 10000000L;
  private static final long LOG10_2_NUMERATOR = (long) (LOG10_2_DENOMINATOR * Math.log10(2));

  public static void main(String[] args) {
    boolean verbose = false;
    EnumSet<FloatingPointFormat> formats = EnumSet.noneOf(FloatingPointFormat.class);
    formats.add(FloatingPointFormat.FLOAT16);
    formats.add(FloatingPointFormat.FLOAT32);
    formats.add(FloatingPointFormat.FLOAT64);
    for (String s : args) {
      if ("-80".equals(s)) {
        formats.add(FloatingPointFormat.FLOAT80);
      } else if ("-128".equals(s)) {
        formats.add(FloatingPointFormat.FLOAT128);
      } else if ("-256".equals(s)) {
        formats.add(FloatingPointFormat.FLOAT256);
      } else if ("-v".equals(s)) {
        verbose = true;
      }
    }

    for (FloatingPointFormat format : formats) {
      compute(format, verbose);
    }
  }

  private static void compute(FloatingPointFormat format, boolean verbose) {
    int mbits = format.mantissaBits() + 3;

    int minE2 = 0;
    int maxE2 = -(1 - format.bias() - format.mantissaBits() - 2);
    int b1 = 0;
    for (int e2 = minE2; e2 < maxE2 + 1; e2++) {
      int q = Math.max(0, (int) (e2 * LOG10_5_NUMERATOR / LOG10_5_DENOMINATOR) - 1);
      int i = e2 - q;
      BigInteger pow5 = FIVE.pow(i);
      BigInteger pow2 = BigInteger.ONE.shiftLeft(q);

      // max(w) = 4 * ((1 << format.mantissaBits()) * 2 - 1) + 2
      //        = (1 << (format.mantissaBits() + 3)) - 2
      BigInteger mxM = BigInteger.ONE.shiftLeft(mbits).subtract(TWO);
      BigInteger min = EuclidMinMax.min(pow5, pow2, mxM.subtract(BigInteger.ONE));
//      BigInteger min2 = minSlow(pow5, pow2, mxM);
//      if (!min.equals(min2)) {
//        new IllegalStateException(min + " " + min2).printStackTrace(System.out);
//      }

      int bits = min.divide(mxM).bitLength();
      int reqn = pow5.bitLength() - bits;
      b1 = Math.max(b1, reqn);
      if (verbose) {
        System.out.printf("%s,%s,%s,%s,%s,%s,%.2f%%\n",
            Integer.valueOf(e2), Integer.valueOf(q), Integer.valueOf(i), Integer.valueOf(bits),
            Integer.valueOf(reqn), Integer.valueOf(b1), Double.valueOf((100.0 * e2) / maxE2));
      }
    }
    if (verbose) {
      System.out.println("B_1 = " + b1);
    }

    minE2 = 0;
    maxE2 = ((1 << format.exponentBits()) - 2) - format.bias() - format.mantissaBits() - 2;
    int b0 = 0;
    for (int e2 = minE2; e2 < maxE2 + 1; e2++) {
      int q = Math.max(0, (int) (e2 * LOG10_2_NUMERATOR / LOG10_2_DENOMINATOR) - 1);
      BigInteger pow5 = FIVE.pow(q);
      BigInteger pow2 = BigInteger.ONE.shiftLeft(e2 - q);

      // max(w) = 4 * ((1 << format.mantissaBits()) * 2 - 1) + 2
      //        = (1 << (format.mantissaBits() + 3)) - 2
      BigInteger mxM = BigInteger.ONE.shiftLeft(mbits).subtract(TWO);
      BigInteger max = EuclidMinMax.max(pow2, pow5, mxM.subtract(BigInteger.ONE));
//      BigInteger max2 = maxSlow(pow2, pow5, mxM);
//      if (!max.equals(max2)) {
//        new IllegalStateException(max + " " + max2).printStackTrace(System.out);
//      }

      BigInteger num = mxM.multiply(pow5).multiply(pow2);
      BigInteger den = pow5.subtract(max);
      int bits = num.divide(den).bitLength();
      int reqn = bits - pow5.bitLength();
      b0 = Math.max(b0, reqn);
      if (verbose) {
        System.out.printf("%s,%s,%s,%s,%s,%s,%.2f%%\n",
            Integer.valueOf(e2), Integer.valueOf(q), Integer.valueOf(e2 - q), Integer.valueOf(bits),
            Integer.valueOf(reqn), Integer.valueOf(b0), Double.valueOf((100.0 * e2) / maxE2));
      }
    }
    if (verbose) {
      System.out.println("B_0 = " + b0);
      System.out.println();
    }
    System.out.printf("%s,%d,%d\n", format, Integer.valueOf(b0), Integer.valueOf(b1));
  }
}
