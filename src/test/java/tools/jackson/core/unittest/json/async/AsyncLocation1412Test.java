package tools.jackson.core.unittest.json.async;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.async.ByteArrayFeeder;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.unittest.async.AsyncTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AsyncLocation1412Test extends AsyncTestBase
{
    private static final JsonFactory jsonFactory = new JsonFactory();
    private static final byte[] json = "[true]".getBytes(StandardCharsets.UTF_8);

    static class TokenAndByteOffsets {
        public final JsonToken token;
        public final long tokenByteOffset;
        public final long parserByteOffset;

        public TokenAndByteOffsets(JsonToken token,
                long tokenByteOffset,
                long parserByteOffset) {
            this.token = token;
            this.tokenByteOffset = tokenByteOffset;
            this.parserByteOffset = parserByteOffset;
        }

        @Override
        public boolean equals(Object o) {
             if (!(o instanceof TokenAndByteOffsets)) {
                 return false;
             }
             TokenAndByteOffsets other = (TokenAndByteOffsets) o;
             return Objects.equals(token, other.token)
                     && tokenByteOffset == other.tokenByteOffset
                     && parserByteOffset == other.parserByteOffset;
        }
    }

    /**
     * Returns the result of feeding the whole JSON document at once from array offset zero.
     */
    private static List<TokenAndByteOffsets> expected() throws Exception {
      try (JsonParser parser = jsonFactory.createNonBlockingByteArrayParser(ObjectReadContext.empty())) {
        ByteArrayFeeder feeder = (ByteArrayFeeder) parser.nonBlockingInputFeeder();
       feeder.feedInput(json, 0, json.length);
        feeder.endOfInput();
        return readAvailableTokens(parser);
      }
    }

    // This one passes:
    @Test
    @DisplayName("Feed one byte at a time from array offset zero")
    void feedByteByByteFromOffsetZero() throws Exception {
      try (JsonParser parser = jsonFactory.createNonBlockingByteArrayParser(ObjectReadContext.empty())) {
        ByteArrayFeeder feeder = (ByteArrayFeeder) parser.nonBlockingInputFeeder();
        List<TokenAndByteOffsets> actual = new ArrayList<>();

        for (int i = 0; i < json.length; i++) {
          byte[] temp = new byte[]{json[i]};
          feeder.feedInput(temp, 0, temp.length);
          if (i == json.length - 1) feeder.endOfInput();
          actual.addAll(readAvailableTokens(parser));
        }

       assertEquals(expected(), actual);
      }
    }

    @Test
    @DisplayName("Feed one byte at a time from non-zero array offset")
    void feedByteByByteFromNonZeroOffset() throws Exception {
      try (JsonParser parser = jsonFactory.createNonBlockingByteArrayParser(ObjectReadContext.empty())) {
        ByteArrayFeeder feeder = (ByteArrayFeeder) parser.nonBlockingInputFeeder();
        List<TokenAndByteOffsets> actual = new ArrayList<>();

        for (int i = 0; i < json.length; i++) {
          feeder.feedInput(json, i, i + 1);
          if (i == json.length - 1) feeder.endOfInput();
          actual.addAll(readAvailableTokens(parser));
        }

        assertEquals(expected(), actual);
      }
    }

    @Test
    @DisplayName("Feed whole document at once from non-zero array offset")
    void feedWholeDocumentFromNonZeroOffset() throws Exception {
      // Same document, but preceded by a 0 byte.
      byte[] jsonOffsetByOne = new byte[json.length + 1];
      System.arraycopy(json, 0, jsonOffsetByOne, 1, json.length);

      try (JsonParser parser = jsonFactory.createNonBlockingByteArrayParser(ObjectReadContext.empty())) {
        ByteArrayFeeder feeder = (ByteArrayFeeder) parser.nonBlockingInputFeeder();

        feeder.feedInput(jsonOffsetByOne, 1, jsonOffsetByOne.length);
        feeder.endOfInput();
        assertEquals(expected(), readAvailableTokens(parser));
      }
    }

    private static List<TokenAndByteOffsets> readAvailableTokens(JsonParser parser) throws Exception
    {
      List<TokenAndByteOffsets> result = new ArrayList<>();
      while (true) {
        JsonToken token = parser.nextToken();
        if (token == JsonToken.NOT_AVAILABLE || token == null) return result;
        result.add(new TokenAndByteOffsets(
          token,
          parser.currentTokenLocation().getByteOffset(),
          parser.currentLocation().getByteOffset())
        );
      }
    }
}
