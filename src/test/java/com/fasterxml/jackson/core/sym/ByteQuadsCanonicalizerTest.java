package com.fasterxml.jackson.core.sym;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Unit tests for class {@link ByteQuadsCanonicalizer}.
 *
 * @date 16.07.2017
 * @see ByteQuadsCanonicalizer
 *
 **/
public class ByteQuadsCanonicalizerTest{


  @Test
  public void testCalcHashThrowsIllegalArgumentException() {

      ByteQuadsCanonicalizer byteQuadsCanonicalizer = ByteQuadsCanonicalizer.createRoot(8);
      int[] intArray = new int[3];

      try { 
        byteQuadsCanonicalizer.calcHash(intArray, (-2261));
        fail("Expecting exception: IllegalArgumentException");
      } catch(IllegalArgumentException e) {
         assertEquals(ByteQuadsCanonicalizer.class.getName(), e.getStackTrace()[0].getClassName());
      }

  }


  @Test
  public void testAddNameThrowsNullPointerException() {

      ByteQuadsCanonicalizer byteQuadsCanonicalizer = ByteQuadsCanonicalizer.createRoot(0);

      try { 
        byteQuadsCanonicalizer.addName("", 0, 0);
        fail("Expecting exception: NullPointerException");
      } catch(NullPointerException e) {
         assertEquals(ByteQuadsCanonicalizer.class.getName(), e.getStackTrace()[0].getClassName());
      }

  }


}