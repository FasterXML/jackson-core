package com.fasterxml.jackson.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for class {@link RequestPayload}.
 *
 * @see RequestPayload
 **/
public class RequestPayloadTest {
  @Test
  public void testFailsToCreateTakingCharSequenceThrowsIllegalArgumentExceptionOne() {
      assertThrows(IllegalArgumentException.class, () -> {
          new RequestPayload(null);
      });
  }

  @Test
  public void testFailsToCreateTakingCharSequenceThrowsIllegalArgumentExceptionTwo() {
      assertThrows(IllegalArgumentException.class, () -> {
          new RequestPayload(null, "UTF-8");
      });
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