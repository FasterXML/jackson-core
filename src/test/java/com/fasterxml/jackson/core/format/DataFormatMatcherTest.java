package com.fasterxml.jackson.core.format;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.core.JsonFactory;

/**
 * Unit tests for class {@link DataFormatMatcher}.
 */
public class DataFormatMatcherTest extends com.fasterxml.jackson.core.BaseTest
{
  public void testGetDataStream() throws IOException {
    byte[] byteArray = new byte[2];
    MatchStrength matchStrength = MatchStrength.WEAK_MATCH;
    DataFormatMatcher dataFormatMatcher = new DataFormatMatcher(null,
            byteArray,
            1,
            0,
            null,
            matchStrength);
    InputStream inputStream = dataFormatMatcher.getDataStream();
    assertEquals(0, inputStream.available());
    inputStream.close();
  }

  public void testCreatesDataFormatMatcherTwo() throws IOException {
    JsonFactory jsonFactory = new JsonFactory();
    try {
        @SuppressWarnings("unused")
        DataFormatMatcher dataFormatMatcher = new DataFormatMatcher(null,
                new byte[0], 2, 1,
                jsonFactory, MatchStrength.NO_MATCH);
    } catch (IllegalArgumentException e) {
        verifyException(e, "Illegal start/length");
    }
  }
}
