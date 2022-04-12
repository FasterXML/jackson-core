package com.fasterxml.jackson.core.io.ryu;

import static org.junit.Assert.assertEquals;
import java.math.BigInteger;
import java.util.Random;
import org.junit.Test;
import com.fasterxml.jackson.core.io.ryu.analysis.EuclidMinMax;

public class EuclidMinMaxTest {
  private static int min(int a, int b, int max) {
    return EuclidMinMax.min(BigInteger.valueOf(a), BigInteger.valueOf(b), BigInteger.valueOf(max)).intValueExact();
  }

  private static int max(int a, int b, int max) {
    return EuclidMinMax.max(BigInteger.valueOf(a), BigInteger.valueOf(b), BigInteger.valueOf(max)).intValueExact();
  }

  private static BigInteger minSlow(BigInteger multiplier, BigInteger modulo, BigInteger maximum) {
//    if (maximum.compareTo(modulo) >= 0) {
//      return BigInteger.ZERO;
//    }
    BigInteger result = multiplier.mod(modulo);
    for (long l = 2; l <= maximum.longValueExact(); l++) {
      BigInteger cand = BigInteger.valueOf(l).multiply(multiplier).mod(modulo);
      if (cand.compareTo(result) < 0) {
        result = cand;
      }
    }
    return result;
  }

  private static BigInteger maxSlow(BigInteger multiplier, BigInteger modulo, BigInteger maximum) {
//    if (maximum.compareTo(modulo) >= 0) {
//      return modulo.subtract(BigInteger.ONE);
//    }
    BigInteger result = multiplier.mod(modulo);
    for (long l = 2; l <= maximum.longValueExact(); l++) {
      BigInteger cand = BigInteger.valueOf(l).multiply(multiplier).mod(modulo);
      if (cand.compareTo(result) > 0) {
        result = cand;
      }
    }
    return result;
  }

  @Test
  public void simpleMin() {
    assertEquals(2, min(2, 10, 1));
    assertEquals(2, min(2, 10, 2));
    assertEquals(2, min(2, 10, 3));
    assertEquals(2, min(2, 10, 4));
    assertEquals(0, min(2, 10, 5));
  }

  @Test
  public void simpleMax() {
    assertEquals(2, max(2, 10, 1));
    assertEquals(4, max(2, 10, 2));
    assertEquals(6, max(2, 10, 3));
    assertEquals(8, max(2, 10, 4));
    assertEquals(8, max(2, 10, 5));
  }

  @Test
  public void minAgain() {
    assertEquals(7, min(7, 10, 1));
    assertEquals(4, min(7, 10, 2));
    assertEquals(1, min(7, 10, 3));
    assertEquals(1, min(7, 10, 4));
    assertEquals(1, min(7, 10, 5));
    assertEquals(1, min(7, 10, 6));
    assertEquals(1, min(7, 10, 7));
    assertEquals(1, min(7, 10, 8));
    assertEquals(1, min(7, 10, 9));
    assertEquals(0, min(7, 10, 10));
  }

  @Test
  public void maxAgain() {
    assertEquals(7, max(7, 10, 1));
    assertEquals(7, max(7, 10, 2));
    assertEquals(7, max(7, 10, 3));
    assertEquals(8, max(7, 10, 4));
    assertEquals(8, max(7, 10, 5));
    assertEquals(8, max(7, 10, 6));
    assertEquals(9, max(7, 10, 7));
    assertEquals(9, max(7, 10, 8));
    assertEquals(9, max(7, 10, 9));
    assertEquals(9, max(7, 10, 10));
  }

  @Test
  public void minAgain2() {
    assertEquals(7, min(7, 30, 1));
    assertEquals(7, min(7, 30, 2));
    assertEquals(7, min(7, 30, 3));
    assertEquals(7, min(7, 30, 4));
    assertEquals(5, min(7, 30, 5));
    assertEquals(5, min(7, 30, 6));
    assertEquals(5, min(7, 30, 7));
    assertEquals(5, min(7, 30, 8));
    assertEquals(3, min(7, 30, 9));
    assertEquals(3, min(7, 30, 10));
  }

  @Test
  public void compareMin() {
    Random rand = new Random(1234L);
    for (int i = 0; i < 10000; i++) {
      BigInteger a = BigInteger.valueOf(rand.nextInt(500) + 2);
      BigInteger b = BigInteger.valueOf(rand.nextInt(500) + 2);
      BigInteger max = BigInteger.valueOf(rand.nextInt(100) + 1);
//      System.out.println(a + " " + b + " " + max + " " + a.mod(b));
      BigInteger minFast = EuclidMinMax.min(a, b, max);
      BigInteger minSlow = minSlow(a, b, max);
      assertEquals(minSlow, minFast);
    }
  }

  @Test
  public void compareMax() {
    Random rand = new Random(1234L);
    for (int i = 0; i < 10000; i++) {
      BigInteger a = BigInteger.valueOf(rand.nextInt(500) + 2);
      BigInteger b = BigInteger.valueOf(rand.nextInt(500) + 2);
      BigInteger max = BigInteger.valueOf(rand.nextInt(100) + 1);
//      System.out.println(a + " " + b + " " + max);
      BigInteger maxFast = EuclidMinMax.max(a, b, max);
      BigInteger maxSlow = maxSlow(a, b, max);
      assertEquals(maxSlow, maxFast);
    }
  }
}
