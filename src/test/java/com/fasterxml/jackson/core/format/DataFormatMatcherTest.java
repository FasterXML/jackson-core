package com.fasterxml.jackson.core.format;

import com.fasterxml.jackson.core.JsonFactory;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for class {@link DataFormatMatcher}.
 *
 * @date 2017-07-31
 * @see DataFormatMatcher
 **/
public class DataFormatMatcherTest {

  @Test
  public void testGetDataStream() throws IOException {
    byte[] byteArray = new byte[2];
    MatchStrength matchStrength = MatchStrength.WEAK_MATCH;
    DataFormatMatcher dataFormatMatcher = new DataFormatMatcher(null,
            byteArray,
            0,
            (-2),
            null,
            matchStrength);
    InputStream inputStream = dataFormatMatcher.getDataStream();

    assertEquals((-2), inputStream.available());
  }

  @Test(expected = ArrayIndexOutOfBoundsException.class)
  public void testCreatesDataFormatMatcherTwo() throws IOException {
    byte[] byteArray = new byte[0];
    JsonFactory jsonFactory = new JsonFactory();
    DataFormatMatcher dataFormatMatcher = new DataFormatMatcher(null,
            byteArray, 2,
            2,
            jsonFactory,
            MatchStrength.NO_MATCH);
    dataFormatMatcher.createParserWithMatch();
  }

}