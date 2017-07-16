package com.fasterxml.jackson.core.sym;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for class {@link CharsToNameCanonicalizer}.
 *
 * @date 16.07.2017
 * @see CharsToNameCanonicalizer
 *
 **/
public class CharsToNameCanonicalizerTest{


  @Test
  public void testCalcHashTakingStringReturningPositive() {

      CharsToNameCanonicalizer charsToNameCanonicalizer = CharsToNameCanonicalizer.createRoot(0);

      assertEquals(1, charsToNameCanonicalizer.calcHash("") );

  }


  @Test
  public void testCalcHashTakingThreeArgumentsReturningPositive() {

      CharsToNameCanonicalizer charsToNameCanonicalizer = CharsToNameCanonicalizer.createRoot(0);
      char[] charArray = new char[0];

      assertEquals( 1, charsToNameCanonicalizer.calcHash(charArray, 0, 0) );

  }


    @Test
    public void testCalcHashTakingThreeArguments() {

        CharsToNameCanonicalizer charsToNameCanonicalizer = CharsToNameCanonicalizer.createRoot(0);
        char[] charArray = new char[4];

        charArray[0] = 1;
        charArray[1] = 2;
        charArray[2] = 3;
        charArray[3] = 4;

        assertEquals( 103, charsToNameCanonicalizer.calcHash(charArray, 2, 2) );

    }


    @Test(expected = ArrayIndexOutOfBoundsException.class)  //I consider this to be a defect.
    public void testCalcHashTakingThreeArgumentsAndRaisingArrayIndexOutOfBoundsException() {

        CharsToNameCanonicalizer charsToNameCanonicalizer = CharsToNameCanonicalizer.createRoot(0);
        char[] charArray = new char[0];

        assertEquals( 1, charsToNameCanonicalizer.calcHash(charArray, 2, 2) );

    }


}