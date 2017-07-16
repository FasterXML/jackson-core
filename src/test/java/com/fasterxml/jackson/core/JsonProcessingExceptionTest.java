package com.fasterxml.jackson.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for class {@link JsonProcessingException}.
 *
 * @date 16.07.2017
 * @see JsonProcessingException
 *
 **/
public class JsonProcessingExceptionTest{


  @Test
  public void testGetProcessorReturnsNull() {

      JsonProcessingException jsonProcessingException = new JsonProcessingException("com.fasterxml.jackson.core.JsonLocation");
      Object object = jsonProcessingException.getProcessor();

      assertNull(object);

  }


  @Test
  public void testCreatesJsonProcessingExceptionTakingThreeArgumentsAndCallsToString() {

      JsonProcessingException jsonProcessingException = new JsonProcessingException("", (Throwable) null);
      String string = jsonProcessingException.toString();

      assertEquals("com.fasterxml.jackson.core.JsonProcessingException: ", string);

  }


}