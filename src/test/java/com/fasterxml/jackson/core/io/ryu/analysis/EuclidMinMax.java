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
 * Computes the modular min and max using a modified version of Euclid's algorithm.
 */
public final class EuclidMinMax {
  private static final boolean DEBUG = false;

  public static BigInteger min(BigInteger multiplier, BigInteger modulo, BigInteger maximum) {
    if (DEBUG) {
      System.out.println();
    }
    BigInteger c = multiplier.gcd(modulo);
    BigInteger b = modulo.divide(c);
    BigInteger a = multiplier.divide(c).mod(b);
    if (maximum.compareTo(b) >= 0) {
      return BigInteger.ZERO;
    }
    if (DEBUG) {
      System.out.println("C=" + c);
    }
    BigInteger s = BigInteger.ONE;
    BigInteger t = BigInteger.ZERO;
    BigInteger u = BigInteger.ZERO;
    BigInteger v = BigInteger.ONE;
    while (true) {
      if (DEBUG) {
        System.out.printf("A=%s\nB=%s\n", a, b);
      }
      while (b.compareTo(a) >= 0) {
        b = b.subtract(a);
        u = u.subtract(s);
        v = v.subtract(t);
        if (DEBUG) {
          System.out.printf("U=%s (A=%s)\n", u, a);
        }
        if (u.negate().compareTo(maximum) >= 0) {
          return a.multiply(c);
        }
      }
      if (b.equals(BigInteger.ZERO)) {
        return BigInteger.ZERO;
      }
      while (a.compareTo(b) >= 0) {
        BigInteger oldA = a;
        a = a.subtract(b);
        s = s.subtract(u);
        t = t.subtract(v);
        if (DEBUG) {
          System.out.printf("S=%s (A=%s)\n", s, a);
        }
        int cmp = s.compareTo(maximum);
        if (cmp >= 0) {
          if (cmp > 0) {
            return oldA.multiply(c);
          } else {
            return a.multiply(c);
          }
        }
      }
      if (a.equals(BigInteger.ZERO)) {
        return BigInteger.ZERO;
      }
    }
  }

  public static BigInteger max(BigInteger multiplier, BigInteger modulo, BigInteger maximum) {
    if (DEBUG) {
      System.out.println();
    }
    BigInteger c = multiplier.gcd(modulo);
    BigInteger b = modulo.divide(c);
    BigInteger a = multiplier.divide(c).mod(b);
    if (DEBUG) {
      System.out.printf("A=%s B=%s C=%s MAX=%s\n", a, b, c, maximum);
    }
    if (maximum.compareTo(b) >= 0) {
      return modulo.subtract(c);
    }
    BigInteger s = BigInteger.ONE;
    BigInteger t = BigInteger.ZERO;
    BigInteger u = BigInteger.ZERO;
    BigInteger v = BigInteger.ONE;
    while (true) {
      if (DEBUG) {
        System.out.printf("A=%s\nB=%s\n", a, b);
      }
      while (b.compareTo(a) >= 0) {
        BigInteger q = b.divide(a);
        q = q.min(maximum.subtract(u.negate()).divide(s).subtract(BigInteger.ONE));
        q = q.max(BigInteger.ONE);
        BigInteger oldB = b;
        b = b.subtract(a.multiply(q));
        u = u.subtract(s.multiply(q));
        v = v.subtract(t.multiply(q));
        if (DEBUG) {
          System.out.printf("U=%s (B=%s)\n", u, b);
        }
        int cmp = u.negate().compareTo(maximum);
        if (cmp >= 0) {
          if (cmp > 0) {
            return modulo.subtract(oldB.multiply(c));
          } else {
            return modulo.subtract(b.multiply(c));
          }
        }
      }
      if (b.equals(BigInteger.ZERO)) {
        return modulo.subtract(c);
      }
      while (a.compareTo(b) >= 0) {
        BigInteger q = BigInteger.ONE; //a.divide(b);
        if (!u.equals(BigInteger.ZERO)) {
          q = q.min(maximum.subtract(s).divide(u.negate().add(BigInteger.ONE)));
        }
        q = q.max(BigInteger.ONE);
        a = a.subtract(b.multiply(q));
        s = s.subtract(u.multiply(q));
        t = t.subtract(v.multiply(q));
        if (DEBUG) {
          System.out.printf("S=%s (B=%s)\n", s, b);
        }
        if (s.compareTo(maximum) >= 0) {
          return modulo.subtract(b.multiply(c));
        }
      }
      if (a.equals(BigInteger.ZERO)) {
        return modulo.subtract(c);
      }
    }
  }
}
