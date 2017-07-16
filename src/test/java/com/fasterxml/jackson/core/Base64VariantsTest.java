package com.fasterxml.jackson.core;

import org.junit.Test;
import static org.junit.Assert.*;
import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.Base64Variants;

/**
 * Unit tests for class {@link Base64Variants}.
 *
 * @date 16.07.2017
 * @see Base64Variants
 *
 **/
public class Base64VariantsTest{


  @Test
  public void testValueOfWithNullThrowsIllegalArgumentException() {

      try { 
        Base64Variants.valueOf(null);
        fail("Expecting exception: IllegalArgumentException");
      } catch(IllegalArgumentException e) {
         assertEquals(Base64Variants.class.getName(), e.getStackTrace()[0].getClassName());
      }

  }


}