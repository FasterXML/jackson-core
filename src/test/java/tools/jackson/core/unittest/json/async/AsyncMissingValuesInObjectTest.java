package tools.jackson.core.unittest.json.async;

import java.util.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import tools.jackson.core.JsonToken;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonFactoryBuilder;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.testutil.AsyncReaderWrapper;
import tools.jackson.core.unittest.async.AsyncTestBase;

import static org.junit.jupiter.api.Assertions.*;

public class AsyncMissingValuesInObjectTest extends AsyncTestBase
{
    JsonFactory factory;
    HashSet<JsonReadFeature> features;

    public void initAsyncMissingValuesInObjectTest(Collection<JsonReadFeature> features) {
        this.features = new HashSet<>(features);
        JsonFactoryBuilder b = JsonFactory.builder();
        for (JsonReadFeature feature : features) {
            b = b.enable(feature);
        }
        this.factory = b.build();
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

            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertEquals("a", p.currentText());
            assertToken(JsonToken.VALUE_TRUE, p.nextToken());

            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
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

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
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

          assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
          assertEquals("a", p.currentText());
          assertToken(JsonToken.VALUE_TRUE, p.nextToken());

          assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
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

          assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
          assertEquals("a", p.currentText());
          assertToken(JsonToken.VALUE_TRUE, p.nextToken());

          assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
          assertEquals("b", p.currentText());
          assertToken(JsonToken.VALUE_FALSE, p.nextToken());

          assertUnexpected(p, ',');
      }
  }

  private void assertEnd(AsyncReaderWrapper p) {
      JsonToken next = p.nextToken();
      assertNull(next, "expected end of stream but found " + next);
  }

  private void assertUnexpected(AsyncReaderWrapper p, char c){
      try {
          p.nextToken();
          fail("No exception thrown");
      } catch (StreamReadException e) {
          verifyException(e, String.format("Unexpected character ('%s' (code %d))", c, (int) c));
      }
  }

  private AsyncReaderWrapper createParser(JsonFactory f, String doc)
  {
      int bytesPerRead = 3; // should vary but...
      AsyncReaderWrapper p = asyncForBytes(f, bytesPerRead, _jsonDoc(doc), 0);
      return p;
  }
}
