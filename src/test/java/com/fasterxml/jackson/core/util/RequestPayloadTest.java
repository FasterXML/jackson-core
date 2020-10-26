package com.fasterxml.jackson.core.util;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for class {@link RequestPayload}.
 *
 * @see RequestPayload
 **/
public class RequestPayloadTest {
  @Test(expected = IllegalArgumentException.class)
  public void testFailsToCreateTakingCharSequenceThrowsIllegalArgumentExceptionOne() {
      new RequestPayload(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFailsToCreateTakingCharSequenceThrowsIllegalArgumentExceptionTwo() {
      new RequestPayload(null, "UTF-8");
  }

  @Test
  public void testCreateTakingCharSequenceAndCallsGetRawPayload() {
    CharSequence charSequence = new String();

    RequestPayload requestPayload = new RequestPayload(charSequence);
    assertEquals("", requestPayload.getRawPayload());
  }

  @Test
  public void testCreateTaking2ArgumentsAndCallsGetRawPayload() {
    byte[] byteArray = new byte[5];
    RequestPayload requestPayload = new RequestPayload(byteArray, "/ _ \" €");

    assertSame(byteArray, requestPayload.getRawPayload());
  }

}