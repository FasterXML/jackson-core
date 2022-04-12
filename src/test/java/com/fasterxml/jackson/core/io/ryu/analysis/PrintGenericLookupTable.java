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
import java.util.Arrays;
import java.util.BitSet;

/**
 * Prints a lookup table for the C version of Ryu.
 */
public final class PrintGenericLookupTable {
  // Bit sizes: FLOAT128,249,246
  // Table sizes: FLOAT128,4968,4897,9865
  // 5^55 < 2^128
  private static final int POS_TABLE_SIZE = 4968;
  private static final int NEG_TABLE_SIZE = 4897;

  // We use the same size for simplicity.
  private static final int POW5_BITCOUNT = 249;
  private static final int POW5_INV_BITCOUNT = 249;

  public static void main(String[] args) {
    BigInteger mask = BigInteger.valueOf(1).shiftLeft(64).subtract(BigInteger.ONE);

    int pow5TableSize = 56; // log_5(2^128) + 1
    System.out.println("#define POW5_INV_BITCOUNT " + POW5_INV_BITCOUNT);
    System.out.println("#define POW5_BITCOUNT " + POW5_BITCOUNT);
    System.out.println("#define POW5_TABLE_SIZE " + pow5TableSize);
    System.out.println();
    System.out.println("static uint64_t GENERIC_POW5_TABLE[POW5_TABLE_SIZE][2] = {");
    for (int i = 0; i < pow5TableSize; i++) {
      if (i != 0) {
        System.out.println(",");
      }
      BigInteger pow = BigInteger.valueOf(5).pow(i);
      System.out.print(" { ");
      for (int j = 0; j < 2; j++) {
        if (j != 0) {
          System.out.print(", ");
        }
        System.out.printf("%20su", pow.and(mask));
        pow = pow.shiftRight(64);
      }
      if (!pow.equals(BigInteger.ZERO)) {
        throw new IllegalStateException();
      }
      System.out.print(" }");
    }
    System.out.println();
    System.out.println("};");

    int posPartialTableSize = (POS_TABLE_SIZE / pow5TableSize) + 1;
    System.out.println("static uint64_t GENERIC_POW5_SPLIT[" + posPartialTableSize + "][4] = {");
    for (int i = 0; i < posPartialTableSize; i++) {
      if (i != 0) {
        System.out.println(",");
      }
      BigInteger pow = pos(pow5TableSize * i);
      System.out.print(" { ");
      for (int j = 0; j < 4; j++) {
        if (j != 0) {
          System.out.print(", ");
        }
        System.out.printf("%20su", pow.and(mask));
        pow = pow.shiftRight(64);
      }
      if (!pow.equals(BigInteger.ZERO)) {
        throw new IllegalStateException();
      }
      System.out.print(" }");
    }
    System.out.println();
    System.out.println("};");

    BitSet errorBits = new BitSet();
    for (int i = 0; i < POS_TABLE_SIZE; i++) {
      int base = i / pow5TableSize;
      int base2 = base * pow5TableSize;
      int offset = i - base2;
      BigInteger pow5base = pos(base2);
      BigInteger pow5offset = BigInteger.valueOf(5).pow(offset);
      BigInteger product = pow5base.multiply(pow5offset).shiftRight(pow5bits(i) - pow5bits(base2));

      BigInteger exact = pos(i);

      int diff = exact.subtract(product).intValueExact();
//      System.out.print((diff != 0 ? diff : " ") + " ");
//      if (i % pow5TableSize == pow5TableSize - 1) {
//        System.out.println();
//      }
      errorBits.set(2 * i + 0, (diff & 1) != 0);
      errorBits.set(2 * i + 1, (diff & 2) != 0);
    }
    long[] error = Arrays.copyOf(errorBits.toLongArray(), POS_TABLE_SIZE / 32 + 1);
    System.out.println("static uint64_t POW5_ERRORS[" + error.length + "] = {");
    for (int i = 0; i < error.length; i++) {
      if (i % 4 == 0) {
        if (i != 0) {
          System.out.println();
        }
        System.out.print(" ");
      }
      System.out.printf("0x%016xu, ", Long.valueOf(error[i]));
    }
    System.out.println();
    System.out.println("};");

    int negPartialTableSize = (NEG_TABLE_SIZE + pow5TableSize - 1) / pow5TableSize + 1;
    System.out.println("static uint64_t GENERIC_POW5_INV_SPLIT[" + negPartialTableSize + "][4] = {");
    for (int i = 0; i < negPartialTableSize; i++) {
      if (i != 0) {
        System.out.println(",");
      }
      BigInteger inv = neg(i * pow5TableSize);
      System.out.print(" { ");
      for (int j = 0; j < 4; j++) {
        if (j != 0) {
          System.out.print(", ");
        }
        System.out.printf("%20su", inv.and(mask));
        inv = inv.shiftRight(64);
      }
      if (!inv.equals(BigInteger.ZERO)) {
        throw new IllegalStateException();
      }
      System.out.print(" }");
    }
    System.out.println();
    System.out.println("};");
    errorBits.clear();
    for (int i = 0; i < NEG_TABLE_SIZE; i++) {
      int base = (i + pow5TableSize - 1) / pow5TableSize;
      int base2 = base * pow5TableSize;
      int offset = base2 - i;
      BigInteger pow5base = neg(base2);
      BigInteger pow5offset = BigInteger.valueOf(5).pow(offset);
      BigInteger product = pow5base.multiply(pow5offset).shiftRight(pow5bits(base2) - pow5bits(i));

      BigInteger exact = neg(i);

      int diff = exact.subtract(product).intValueExact();
//      System.out.print((diff != 0 ? diff : " ") + " ");
//      if (i % pow5TableSize == pow5TableSize - 1) {
//        System.out.println();
//      }
      errorBits.set(2 * i + 0, (diff & 1) != 0);
      errorBits.set(2 * i + 1, (diff & 2) != 0);
    }
    error = Arrays.copyOf(errorBits.toLongArray(), NEG_TABLE_SIZE / 32 + 1);
    System.out.println("static uint64_t POW5_INV_ERRORS[" + error.length + "] = {");
    for (int i = 0; i < error.length; i++) {
      if (i % 4 == 0) {
        System.out.print(" ");
      }
      System.out.printf("0x%016xu, ", Long.valueOf(error[i]));
      if (i % 4 == 3) {
        System.out.println();
      }
    }
    System.out.println();
    System.out.println("};");

    {
      int[] exps = new int[] { 1, 10, 55, 56, 300, 1000, 2345, 3210, POS_TABLE_SIZE - 3, POS_TABLE_SIZE - 1 };
      System.out.println("static uint64_t EXACT_POW5[" + exps.length + "][4] = {");
      for (int i = 0; i < exps.length; i++) {
        if (i != 0) {
          System.out.println(",");
        }
        BigInteger exact = pos(exps[i]);
        System.out.print(" { ");
        for (int j = 0; j < 4; j++) {
          if (j != 0) {
            System.out.print(", ");
          }
          System.out.printf("%20su", exact.and(mask));
          exact = exact.shiftRight(64);
        }
        if (!exact.equals(BigInteger.ZERO)) {
          throw new IllegalStateException();
        }
        System.out.print(" }");
      }
      System.out.println();
      System.out.println("};");
    }

    {
      int[] exps = new int[] { 1, 10, 55, 56, 300, 1000, 2345, 3210, NEG_TABLE_SIZE - 3, NEG_TABLE_SIZE - 1 };
      System.out.println("static uint64_t EXACT_INV_POW5[" + exps.length + "][4] = {");
      for (int i = 0; i < exps.length; i++) {
        if (i != 0) {
          System.out.println(",");
        }
        BigInteger exact = neg(exps[i]).add(BigInteger.ONE);
        System.out.print(" { ");
        for (int j = 0; j < 4; j++) {
          if (j != 0) {
            System.out.print(", ");
          }
          System.out.printf("%20su", exact.and(mask));
          exact = exact.shiftRight(64);
        }
        if (!exact.equals(BigInteger.ZERO)) {
          throw new IllegalStateException();
        }
        System.out.print(" }");
      }
      System.out.println();
      System.out.println("};");
    }
  }

  private static BigInteger pos(int i) {
    BigInteger pow = BigInteger.valueOf(5).pow(i);
    return pow.shiftRight(pow.bitLength() - POW5_BITCOUNT);
  }

  private static BigInteger neg(int i) {
    BigInteger pow = BigInteger.valueOf(5).pow(i);
    // We want floor(log_2 5^q) here, which is pow5len - 1.
    int shift = pow.bitLength() - 1 + POW5_INV_BITCOUNT;
    return BigInteger.ONE.shiftLeft(shift).divide(pow); //.add(BigInteger.ONE);
  }

  private static int pow5bits(int e) {
    return (int) ((e * 163391164108059L) >> 46) + 1;
  }
}
