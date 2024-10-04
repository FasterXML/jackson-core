package com.fasterxml.jackson.core.format;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for class {@link DataFormatMatcher}.
 */
class DataFormatMatcherTest extends com.fasterxml.jackson.core.JUnit5TestBase
{
    private final JsonFactory JSON_F = new JsonFactory();

    @Test
    void getDataStream() throws IOException {
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

    @Test
    void createsDataFormatMatcherTwo() throws IOException {
    try {
        @SuppressWarnings("unused")
        DataFormatMatcher dataFormatMatcher = new DataFormatMatcher(null,
                new byte[0], 2, 1,
                JSON_F, MatchStrength.NO_MATCH);
    } catch (IllegalArgumentException e) {
        verifyException(e, "Illegal start/length");
    }
  }

    @Test
    void getMatchedFormatNameReturnsNameWhenMatches() {
      DataFormatMatcher dataFormatMatcher = new DataFormatMatcher(null,
              new byte[2],
              1,
              0,
              JSON_F,
              MatchStrength.SOLID_MATCH);
      assertEquals(JsonFactory.FORMAT_NAME_JSON, dataFormatMatcher.getMatchedFormatName());
  }

    @Test
    void detectorConfiguration() {
      DataFormatDetector df0 = new DataFormatDetector(JSON_F);

      // Defaults are: SOLID for optimal, WEAK for minimum, so:
      assertSame(df0, df0.withOptimalMatch(MatchStrength.SOLID_MATCH));
      assertSame(df0, df0.withMinimalMatch(MatchStrength.WEAK_MATCH));
      assertSame(df0, df0.withMaxInputLookahead(DataFormatDetector.DEFAULT_MAX_INPUT_LOOKAHEAD));

      // but will change
      assertNotSame(df0, df0.withOptimalMatch(MatchStrength.FULL_MATCH));
      assertNotSame(df0, df0.withMinimalMatch(MatchStrength.SOLID_MATCH));
      assertNotSame(df0, df0.withMaxInputLookahead(DataFormatDetector.DEFAULT_MAX_INPUT_LOOKAHEAD + 5));

      // regardless, we should be able to use `toString()`
      assertNotNull(df0.toString());
  }
}
