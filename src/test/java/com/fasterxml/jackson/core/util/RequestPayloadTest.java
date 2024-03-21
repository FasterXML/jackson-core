package com.fasterxml.jackson.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for class {@link RequestPayload}.
 *
 * @see RequestPayload
 **/
class RequestPayloadTest {
    @Test
    void failsToCreateTakingCharSequenceThrowsIllegalArgumentExceptionOne() {
      assertThrows(IllegalArgumentException.class, () -> {
          new RequestPayload(null);
      });
  }

    @Test
    void failsToCreateTakingCharSequenceThrowsIllegalArgumentExceptionTwo() {
      assertThrows(IllegalArgumentException.class, () -> {
          new RequestPayload(null, "UTF-8");
      });
  }

    @Test
    void createTakingCharSequenceAndCallsGetRawPayload() {
    CharSequence charSequence = new String();

    RequestPayload requestPayload = new RequestPayload(charSequence);
    assertEquals("", requestPayload.getRawPayload());
  }

    @Test
    void createTaking2ArgumentsAndCallsGetRawPayload() {
    byte[] byteArray = new byte[5];
    RequestPayload requestPayload = new RequestPayload(byteArray, "/ _ \" €");

    assertSame(byteArray, requestPayload.getRawPayload());
  }

}