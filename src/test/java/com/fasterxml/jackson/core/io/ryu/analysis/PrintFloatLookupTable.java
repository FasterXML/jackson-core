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

/**
 * Prints a lookup table for the C version of Ryu.
 */
public final class PrintFloatLookupTable {
  private static final int POS_TABLE_SIZE = 47;
  private static final int INV_TABLE_SIZE = 55;

  private static final int POW5_BITCOUNT = 61; // max 63
  private static final int POW5_INV_BITCOUNT = 59; // max 63

  public static void main(String[] args) {
    System.out.println("#define POW5_INV_BITCOUNT " + POW5_INV_BITCOUNT);
    System.out.println("static uint64_t FLOAT_POW5_INV_SPLIT[" + INV_TABLE_SIZE + "] = {");
    for (int i = 0; i < INV_TABLE_SIZE; i++) {
      BigInteger pow = BigInteger.valueOf(5).pow(i);
      int pow5len = pow.bitLength();
      int j = pow5len - 1 + POW5_INV_BITCOUNT;
      BigInteger pow5inv = BigInteger.ONE.shiftLeft(j).divide(pow).add(BigInteger.ONE);

      long v = pow5inv.longValueExact();
      System.out.printf(" %19su", Long.toUnsignedString(v));
      if (i < INV_TABLE_SIZE - 1) {
        System.out.print(",");
      }
      if (i % 4 == 3) {
        System.out.println();
      }
    }
    System.out.println();
    System.out.println("};");

    System.out.println("#define POW5_BITCOUNT " + POW5_BITCOUNT);
    System.out.println("static uint64_t FLOAT_POW5_SPLIT[" + POS_TABLE_SIZE + "] = {");
    for (int i = 0; i < POS_TABLE_SIZE; i++) {
      BigInteger pow = BigInteger.valueOf(5).pow(i);
      int pow5len = pow.bitLength();
      BigInteger pow5 = pow.shiftRight(pow5len - POW5_BITCOUNT);

      long v = pow5.longValueExact();
      System.out.printf(" %19su", Long.toUnsignedString(v));
      if (i < POS_TABLE_SIZE - 1) {
        System.out.print(",");
      }
      if (i % 4 == 3) {
        System.out.println();
      }
    }
    System.out.println();
    System.out.println("};");
  }
}
