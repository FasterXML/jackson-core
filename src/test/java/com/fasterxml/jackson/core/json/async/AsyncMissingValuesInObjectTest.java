package com.fasterxml.jackson.core.json.async;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.*;

public class AsyncMissingValuesInObjectTest extends AsyncTestBase
{
    JsonFactory factory;
    HashSet<JsonReadFeature> features;

    public void initAsyncMissingValuesInObjectTest(Collection<JsonReadFeature> features) {
        this.features = new HashSet<>(features);
        JsonFactoryBuilder b = (JsonFactoryBuilder) JsonFactory.builder();
        for (JsonReadFeature feature : features) {
            b = b.enable(feature);
        }
        factory = b.build();
    }

    public static Collection<EnumSet<JsonReadFeature>> getTestCases()
    {
        List<EnumSet<JsonReadFeature>> cases = new ArrayList<>();
        cases.add(EnumSet.noneOf(JsonReadFeature.class));
        cases.add(EnumSet.of(JsonReadFeature.ALLOW_MISSING_VALUES));
        cases.add(EnumSet.of(JsonReadFeature.ALLOW_TRAILING_COMMA));
        cases.add(EnumSet.of(JsonReadFeature.ALLOW_MISSING_VALUES, JsonReadFeature.ALLOW_TRAILING_COMMA));
        return cases;
    }

    @MethodSource("getTestCases")
    @ParameterizedTest(name = "Features {0}")
    void objectBasic(Collection<JsonReadFeature> features) throws Exception {
        initAsyncMissingValuesInObjectTest(features);
        String json = "{\"a\": true, \"b\": false}";

        try (AsyncReaderWrapper p = createParser(factory, json)) {

            assertEquals(JsonToken.START_OBJECT, p.nextToken());

            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            assertEquals("a", p.currentText());
            assertToken(JsonToken.VALUE_TRUE, p.nextToken());

            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            assertEquals("b", p.currentText());
            assertToken(JsonToken.VALUE_FALSE, p.nextToken());

            assertEquals(JsonToken.END_OBJECT, p.nextToken());
            assertEnd(p);
        }
  }

    @MethodSource("getTestCases")
    @ParameterizedTest(name = "Features {0}")
    void objectInnerComma(Collection<JsonReadFeature> features) throws Exception {
        initAsyncMissingValuesInObjectTest(features);
    String json = "{\"a\": true,, \"b\": false}";

    try (AsyncReaderWrapper p = createParser(factory, json)) {

        assertEquals(JsonToken.START_OBJECT, p.nextToken());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("a", p.currentText());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());

        assertUnexpected(p, ',');
    }
  }

    @MethodSource("getTestCases")
    @ParameterizedTest(name = "Features {0}")
    void objectLeadingComma(Collection<JsonReadFeature> features) throws Exception {
        initAsyncMissingValuesInObjectTest(features);
    String json = "{,\"a\": true, \"b\": false}";

      try (AsyncReaderWrapper p = createParser(factory, json)) {

          assertEquals(JsonToken.START_OBJECT, p.nextToken());

          assertUnexpected(p, ',');
      }
  }

    @MethodSource("getTestCases")
    @ParameterizedTest(name = "Features {0}")
    void objectTrailingComma(Collection<JsonReadFeature> features) throws Exception {
        initAsyncMissingValuesInObjectTest(features);
    String json = "{\"a\": true, \"b\": false,}";

      try (AsyncReaderWrapper p = createParser(factory, json)) {

          assertEquals(JsonToken.START_OBJECT, p.nextToken());

          assertToken(JsonToken.FIELD_NAME, p.nextToken());
          assertEquals("a", p.currentText());
          assertToken(JsonToken.VALUE_TRUE, p.nextToken());

          assertToken(JsonToken.FIELD_NAME, p.nextToken());
          assertEquals("b", p.currentText());
          assertToken(JsonToken.VALUE_FALSE, p.nextToken());

          if (features.contains(JsonReadFeature.ALLOW_TRAILING_COMMA)) {
              assertToken(JsonToken.END_OBJECT, p.nextToken());
              assertEnd(p);
          } else {
              assertUnexpected(p, '}');
          }
      }
  }

    @MethodSource("getTestCases")
    @ParameterizedTest(name = "Features {0}")
    void objectTrailingCommas(Collection<JsonReadFeature> features) throws Exception {
        initAsyncMissingValuesInObjectTest(features);
    String json = "{\"a\": true, \"b\": false,,}";

      try (AsyncReaderWrapper p = createParser(factory, json)) {

          assertEquals(JsonToken.START_OBJECT, p.nextToken());

          assertToken(JsonToken.FIELD_NAME, p.nextToken());
          assertEquals("a", p.currentText());
          assertToken(JsonToken.VALUE_TRUE, p.nextToken());

          assertToken(JsonToken.FIELD_NAME, p.nextToken());
          assertEquals("b", p.currentText());
          assertToken(JsonToken.VALUE_FALSE, p.nextToken());

          assertUnexpected(p, ',');
      }
  }

  private void assertEnd(AsyncReaderWrapper p) throws IOException {
      JsonToken next = p.nextToken();
      assertNull(next, "expected end of stream but found " + next);
  }

  private void assertUnexpected(AsyncReaderWrapper p, char c) throws IOException {
      try {
          p.nextToken();
          fail("No exception thrown");
      } catch (JsonParseException e) {
          verifyException(e, String.format("Unexpected character ('%s' (code %d))", c, (int) c));
      }
  }

  private AsyncReaderWrapper createParser(JsonFactory f, String doc) throws IOException
  {
      int bytesPerRead = 3; // should vary but...
      AsyncReaderWrapper p = asyncForBytes(f, bytesPerRead, _jsonDoc(doc), 0);
      return p;
  }
}
