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

import com.fasterxml.jackson.core.io.ryu.RoundingMode;

/**
 * Implementation of Float and Double to String conversion.
 */
public final class SlowConversion {
  private static final BigInteger TWO = BigInteger.valueOf(2);

  private static boolean DEBUG = false;
  private static final boolean DEBUG_FLOAT = true;

  public static void main(String[] args) {
    DEBUG = true;
    if (DEBUG_FLOAT) {
      float f = 0.33007812f;
      String result = floatToString(f);
      System.out.println(result + " " + f);
    } else {
      double f = 1.1873267205539228E-308;
      String result = doubleToString(f);
      System.out.println(result + " " + f);
    }
  }

  public static String floatToString(float value) {
    return floatToString(value, RoundingMode.ROUND_EVEN);
  }

  public static String floatToString(float value, RoundingMode roundingMode) {
    if (DEBUG) System.out.println("VALUE="+value);
    long bits = Float.floatToIntBits(value) & 0xffffffffL;
    return asString(bits, FloatingPointFormat.FLOAT32, roundingMode);
  }

  public static String doubleToString(double value) {
    return doubleToString(value, RoundingMode.ROUND_EVEN);
  }

  public static String doubleToString(double value, RoundingMode roundingMode) {
    if (DEBUG) System.out.println("VALUE="+value);
    long bits = Double.doubleToLongBits(value);
    return asString(bits, FloatingPointFormat.FLOAT64, roundingMode);
  }

  static String asString(long bits, FloatingPointFormat format, RoundingMode mode) {
    // Step 1: Decode the floating point number, and unify normalized and subnormal cases.
    //
    // The format of all IEEE numbers is S E* M*; we obtain M by masking the lower M bits, E by
    // shifting and masking, and S also by shifting and masking.
    int mantissaBits = format.mantissaBits();
    int exponentBits = format.exponentBits();

    int ieeeExponent = (int) ((bits >>> mantissaBits) & ((1 << exponentBits) - 1));
    long ieeeMantissa = bits & ((1L << mantissaBits) - 1);
    boolean sign = ((bits >>> (mantissaBits + exponentBits)) & 1) != 0;
    boolean even = (bits & 1) == 0;

    // Exit early if it's NaN, Infinity, or 0.
    if (ieeeExponent == ((1 << exponentBits) - 1)) {
      // Handle the special cases where the exponent is all 1s indicating NaN or Infinity: if the
      // mantissa is non-zero, it's a NaN, otherwise it's +/-infinity.
      return (ieeeMantissa != 0) ? "NaN" : sign ? "-Infinity" : "Infinity";
    } else if ((ieeeExponent == 0) && (ieeeMantissa == 0)) {
      // If the mantissa is 0, the code below would end up with a lower bound that is less than 0,
      // which throws off the char-by-char comparison. Instead, we exit here with the correct
      // string.
      return sign ? "-0.0" : "0.0";
    }

    // Compute the offset used by the IEEE format.
    int offset = (1 << (exponentBits - 1)) - 1;

    // Unify normalized and subnormal cases.
    int e2;
    long m2;
    if (ieeeExponent == 0) {
      e2 = 1 - offset - mantissaBits;
      m2 = ieeeMantissa;
    } else {
      e2 = ieeeExponent - offset - mantissaBits;
      m2 = ieeeMantissa | (1L << mantissaBits);
    }

    // Step 2: Determine the interval of legal decimal representations.
    long mv = 4 * m2;
    long mp = 4 * m2 + 2;
    long mm = 4 * m2 - (((m2 != (1L << mantissaBits)) || (ieeeExponent <= 1)) ? 2 : 1);
    e2 -= 2;

    // Step 3: Convert to a decimal power base using arbitrary-precision arithmetic.
    BigInteger vr, vp, vm;
    int e10;
    if (e2 >= 0) {
      vr = BigInteger.valueOf(mv).shiftLeft(e2);
      vp = BigInteger.valueOf(mp).shiftLeft(e2);
      vm = BigInteger.valueOf(mm).shiftLeft(e2);
      e10 = 0;
    } else {
      BigInteger factor = BigInteger.valueOf(5).pow(-e2);
      vr = BigInteger.valueOf(mv).multiply(factor);
      vp = BigInteger.valueOf(mp).multiply(factor);
      vm = BigInteger.valueOf(mm).multiply(factor);
      e10 = e2;
    }

    // Step 4: Find the shortest decimal representation in the interval of legal representations.
    //
    // We do some extra work here in order to follow Float/Double.toString semantics. In particular,
    // that requires printing in scientific format if and only if the exponent is between -3 and 7,
    // and it requires printing at least two decimal digits.
    //
    // Above, we moved the decimal dot all the way to the right, so now we need to count digits to
    // figure out the correct exponent for scientific notation.
    int vpLength = vp.toString().length();
    e10 += vpLength - 1;
    boolean scientificNotation = (e10 < -3) || (e10 >= 7);

    if (DEBUG) {
      System.out.println("IN=" + Long.toBinaryString(bits));
      System.out.println("   S=" + (sign ? "-" : "+") + " E=" + e2 + " M=" + m2);
      System.out.println("E ="+e10);
      System.out.println("V+="+vp);
      System.out.println("V ="+vr);
      System.out.println("V-="+vm);
    }

    if (!mode.acceptUpperBound(even)) {
      vp = vp.subtract(BigInteger.ONE);
    }
    boolean vmIsTrailingZeros = true;
    // Track if vr is tailing zeroes _after_ lastRemovedDigit.
    boolean vrIsTrailingZeros = true;
    int removed = 0;
    int lastRemovedDigit = 0;
    while (!vp.divide(BigInteger.TEN).equals(vm.divide(BigInteger.TEN))) {
      if (scientificNotation && vp.compareTo(BigInteger.valueOf(100)) < 0) {
        // Float/Double.toString semantics requires printing at least two digits.
        break;
      }
      vmIsTrailingZeros &= vm.mod(BigInteger.TEN).intValueExact() == 0;
      vrIsTrailingZeros &= lastRemovedDigit == 0;
      lastRemovedDigit = vr.mod(BigInteger.TEN).intValueExact();
      vp = vp.divide(BigInteger.TEN);
      vr = vr.divide(BigInteger.TEN);
      vm = vm.divide(BigInteger.TEN);
      removed++;
    }
    if (vmIsTrailingZeros && mode.acceptLowerBound(even)) {
      while (vm.mod(BigInteger.TEN).intValueExact() == 0) {
        if (scientificNotation && vp.compareTo(BigInteger.valueOf(100)) < 0) {
          // Float/Double.toString semantics requires printing at least two digits.
          break;
        }
        vrIsTrailingZeros &= lastRemovedDigit == 0;
        lastRemovedDigit = vr.mod(BigInteger.TEN).intValueExact();
        vp = vp.divide(BigInteger.TEN);
        vr = vr.divide(BigInteger.TEN);
        vm = vm.divide(BigInteger.TEN);
        removed++;
      }
    }
    if (vrIsTrailingZeros && (lastRemovedDigit == 5) && (vr.mod(TWO).intValueExact() == 0)) {
      // Round down not up if the number ends in X50000 and the number is even.
      lastRemovedDigit = 4;
    }
    String output = ((vr.compareTo(vm) > 0) ? (lastRemovedDigit >= 5 ? vr.add(BigInteger.ONE) : vr) : vp).toString();
    int olength = vpLength - removed;

    if (DEBUG) {
      System.out.println("LRD=" + lastRemovedDigit);
      System.out.println("VP=" + vp);
      System.out.println("VR=" + vr);
      System.out.println("VM=" + vm);
      System.out.println("O=" + output);
      System.out.println("OLEN=" + olength);
      System.out.println("EXP=" + e10);
    }

    // Step 5: Print the decimal representation.
    // We follow Float/Double.toString semantics here.
    StringBuilder result = new StringBuilder();
    // Add the minus sign if the number is negative.
    if (sign) {
      result.append('-');
    }

    if (scientificNotation) {
      result.append(output.charAt(0));
      result.append('.');
      for (int i = 1; i < olength; i++) {
        result.append(output.charAt(i));
      }
      if (olength == 1) {
        result.append('0');
      }
      result.append('E');
      result.append(e10);
      return result.toString();
    } else {
      // Print leading 0s and '.' if applicable.
      for (int i = 0; i > e10; i--) {
        result.append('0');
        if (i == 0) {
          result.append(".");
        }
      }
      // Print number and '.' if applicable.
      for (int i = 0; i < olength; i++) {
        result.append(output.charAt(i));
        if (e10 == 0) {
          result.append('.');
        }
        e10--;
      }
      // Print trailing 0s and '.' if applicable.
      for (; e10 >= -1; e10--) {
        result.append('0');
        if (e10 == 0) {
          result.append('.');
        }
      }
      return result.toString();
    }
  }
}
