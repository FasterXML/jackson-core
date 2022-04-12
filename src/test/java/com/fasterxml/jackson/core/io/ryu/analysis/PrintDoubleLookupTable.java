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
public final class PrintDoubleLookupTable {
  private static final int POS_TABLE_SIZE = 326;
  // The C version has two code paths, one of which requires an additional entry here.
  private static final int NEG_TABLE_SIZE = 342;

  // We intentionally choose these to be larger than or equal to the float equivalent + 64.
  private static final int POW5_BITCOUNT = 125; // max 127
  private static final int POW5_INV_BITCOUNT = 125; // max 127

  private static final boolean PRINT_LARGE_TABLES = true;

  private static final BigInteger MASK64 = BigInteger.valueOf(1).shiftLeft(64).subtract(BigInteger.ONE);

  public static void main(String[] args) {
    BigInteger[] largeInvTable = new BigInteger[NEG_TABLE_SIZE];
    for (int i = 0; i < largeInvTable.length; i++) {
      largeInvTable[i] = invMultiplier(i);
    }

    int mulTableSize = 26;
    BigInteger[] smallInvTable = new BigInteger[(NEG_TABLE_SIZE + mulTableSize - 1) / mulTableSize + 1];
    for (int i = 0; i < smallInvTable.length; i++) {
      smallInvTable[i] = invMultiplier(mulTableSize * i);
    }

    int[] invErrorTable = new int[NEG_TABLE_SIZE];
    for (int i = 0; i < NEG_TABLE_SIZE; i++) {
      int base = (i + mulTableSize - 1) / mulTableSize;
      int base2 = base * mulTableSize;
      int offset = base2 - i;
      BigInteger mul = BigInteger.valueOf(5).pow(offset);
      BigInteger result = smallInvTable[base].subtract(BigInteger.ONE).multiply(mul).shiftRight(pow5bits(base2) - pow5bits(i)).add(BigInteger.ONE);
      BigInteger error = invMultiplier(i).subtract(result);
      if ((error.signum() < 0) || (error.compareTo(BigInteger.valueOf(2)) > 0)) {
        throw new IllegalStateException("That went wrong!");
      }
      invErrorTable[i] = error.intValueExact();
    }

    BigInteger[] largeTable = new BigInteger[POS_TABLE_SIZE];
    for (int i = 0; i < largeTable.length; i++) {
      largeTable[i] = multiplier(i);
    }
    BigInteger[] smallTable = new BigInteger[(POS_TABLE_SIZE + mulTableSize - 1) / mulTableSize];
    for (int i = 0; i < smallTable.length; i++) {
      smallTable[i] = multiplier(mulTableSize * i);
    }

    int[] errorTable = new int[POS_TABLE_SIZE];
    for (int i = 0; i < POS_TABLE_SIZE; i++) {
      int base = i / mulTableSize;
      int base2 = base * mulTableSize;
      int offset = i - base2;
      BigInteger mul = BigInteger.valueOf(5).pow(offset);
      BigInteger result = smallTable[base].multiply(mul).shiftRight(pow5bits(i) - pow5bits(base2));
      BigInteger error = multiplier(i).subtract(result);
      if ((error.signum() < 0) || (error.compareTo(BigInteger.valueOf(2)) > 0)) {
        throw new IllegalStateException("That went wrong: " + error);
      }
      errorTable[i] = error.intValueExact();
    }
//    System.out.println(Arrays.toString(invErrorTable));
//    System.out.println(Arrays.toString(errorTable));

    System.out.println("#define DOUBLE_POW5_INV_BITCOUNT " + POW5_INV_BITCOUNT);
    System.out.println("#define DOUBLE_POW5_BITCOUNT " + POW5_BITCOUNT);
    System.out.println();
    if (PRINT_LARGE_TABLES) {
      printTable("DOUBLE_POW5_INV_SPLIT", largeInvTable, 2);
      System.out.println();
      printTable("DOUBLE_POW5_SPLIT", largeTable, 2);
    } else {
      printTable("DOUBLE_POW5_INV_SPLIT2", smallInvTable, 1);
      printOffsets("POW5_INV_OFFSETS", invErrorTable, 6);
      System.out.println();
      printTable("DOUBLE_POW5_SPLIT2", smallTable, 1);
      printOffsets("POW5_OFFSETS", errorTable, 6);
    }
  }

  private static BigInteger invMultiplier(int i) {
    // 5^i
    BigInteger pow = BigInteger.valueOf(5).pow(i);
    // length of 5^i in binary = ceil(log_2(5^i))
    int pow5len = pow.bitLength();
    // We want floor(log_2(5^i)) here, which is pow5len - 1 (no power of 5 is a power of 2).
    int j = pow5len - 1 + POW5_INV_BITCOUNT;
    // [2^j / 5^i] + 1 = [2^(floor(log_2(5^i)) + POW5_INV_BITCOUNT) / 5^i] + 1
    // By construction, this will have approximately POW5_INV_BITCOUNT + 1 bits.
    BigInteger inv = BigInteger.ONE.shiftLeft(j).divide(pow).add(BigInteger.ONE);
    if (inv.bitLength() > POW5_INV_BITCOUNT + 1) {
      throw new IllegalStateException("Result is longer than expected: " + inv.bitLength() + " > " + (POW5_INV_BITCOUNT + 1));
    }
    return inv;
  }

  private static BigInteger multiplier(int i) {
    // 5^i
    BigInteger pow = BigInteger.valueOf(5).pow(i);
    int pow5len = pow.bitLength();
    // [5^i / 2^j] = [5^i / 2^(ceil(log_2(5^i)) - POW5_BITCOUNT)]
    // By construction, this will have exactly POW5_BITCOUNT bits. Note that this can shift left if j is negative!
    BigInteger pow5DivPow2 = pow.shiftRight(pow5len - POW5_BITCOUNT);
    if (pow5DivPow2.bitLength() != POW5_BITCOUNT) {
      throw new IllegalStateException("Unexpected result length: " + pow5DivPow2.bitLength() + " != " + POW5_BITCOUNT);
    }
    return pow5DivPow2;
  }

  private static int pow5bits(int e) {
    return ((e * 1217359) >> 19) + 1;
  }

  private static void printTable(String name, BigInteger[] table, int entriesPerLine) {
    System.out.println("static const uint64_t " + name + "[" + table.length + "][2] = {");
    for (int i = 0; i < table.length; i++) {
      BigInteger pow5High = table[i].shiftRight(64);
      BigInteger pow5Low = table[i].and(MASK64);
      if (i % entriesPerLine == 0) {
        System.out.print("  ");
      } else {
        System.out.print(" ");
      }
      System.out.printf("{ %20su, %18su }", pow5Low, pow5High);
      if (i != table.length - 1) {
        System.out.print(",");
      }
      if (i % entriesPerLine == entriesPerLine - 1) {
        System.out.println();
      }
    }
    System.out.println("};");
  }

  private static void printOffsets(String name, int[] table, int entriesPerLine) {
    int length = (table.length + 15) / 16;
    System.out.println("static const uint32_t " + name + "[" + length + "] = {");
    for (int i = 0; i < length; i++) {
      int value = 0;
      for (int j = 0; j < 16; j++) {
        int offset = i * 16 + j < table.length ? table[i * 16 + j] : 0;
        value |= offset << (j << 1);
      }
      if (i % entriesPerLine == 0) {
        System.out.print("  ");
      } else {
        System.out.print(" ");
      }
      System.out.printf("0x%08x", Integer.valueOf(value));
      if (i != length - 1) {
        System.out.print(",");
      }
      if ((i % entriesPerLine == entriesPerLine - 1) || (i == length - 1)) {
        System.out.println();
      }
    }
    System.out.println("};");
  }
}
