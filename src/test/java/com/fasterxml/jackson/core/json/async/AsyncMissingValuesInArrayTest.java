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

public class AsyncMissingValuesInArrayTest extends AsyncTestBase
{
    JsonFactory factory;
    HashSet<JsonReadFeature> features;

    public void initAsyncMissingValuesInArrayTest(Collection<JsonReadFeature> features) {
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

    // Could test, but this case is covered by other tests anyway
    /*
    @Test
    public void testArrayBasic() throws Exception {
        String json = "[\"a\", \"b\"]";

        AsyncReaderWrapper p = createParser(factory, json);

    assertEquals(JsonToken.START_ARRAY, p.nextToken());

    assertToken(JsonToken.VALUE_STRING, p.nextToken());
    assertEquals("a", p.currentText());

    assertToken(JsonToken.VALUE_STRING, p.nextToken());
    assertEquals("b", p.currentText());

    assertEquals(JsonToken.END_ARRAY, p.nextToken());
    assertEnd(p);
  }
  */

    @MethodSource("getTestCases")
    @ParameterizedTest(name = "Features {0}")
    void arrayInnerComma(Collection<JsonReadFeature> features) throws Exception {
        initAsyncMissingValuesInArrayTest(features);
    String json = "[\"a\",, \"b\"]";

    AsyncReaderWrapper p = createParser(factory, json);

    assertEquals(JsonToken.START_ARRAY, p.nextToken());

    assertToken(JsonToken.VALUE_STRING, p.nextToken());
    assertEquals("a", p.currentText());

    if (!features.contains(JsonReadFeature.ALLOW_MISSING_VALUES)) {
      assertUnexpected(p, ',');
      p.close();
      return;
    }

    assertToken(JsonToken.VALUE_NULL, p.nextToken());

    assertToken(JsonToken.VALUE_STRING, p.nextToken());
    assertEquals("b", p.currentText());

    assertEquals(JsonToken.END_ARRAY, p.nextToken());
    assertEnd(p);
    p.close();
  }

    @MethodSource("getTestCases")
    @ParameterizedTest(name = "Features {0}")
    void arrayLeadingComma(Collection<JsonReadFeature> features) throws Exception {
        initAsyncMissingValuesInArrayTest(features);
    String json = "[,\"a\", \"b\"]";

    AsyncReaderWrapper p = createParser(factory, json);

    assertEquals(JsonToken.START_ARRAY, p.nextToken());

    if (!features.contains(JsonReadFeature.ALLOW_MISSING_VALUES)) {
      assertUnexpected(p, ',');
      p.close();
      return;
    }

    assertToken(JsonToken.VALUE_NULL, p.nextToken());

    assertToken(JsonToken.VALUE_STRING, p.nextToken());
    assertEquals("a", p.currentText());

    assertToken(JsonToken.VALUE_STRING, p.nextToken());
    assertEquals("b", p.currentText());

    assertEquals(JsonToken.END_ARRAY, p.nextToken());
    assertEnd(p);
    p.close();
  }

    @MethodSource("getTestCases")
    @ParameterizedTest(name = "Features {0}")
    void arrayTrailingComma(Collection<JsonReadFeature> features) throws Exception {
        initAsyncMissingValuesInArrayTest(features);
    String json = "[\"a\", \"b\",]";

    AsyncReaderWrapper p = createParser(factory, json);

    assertEquals(JsonToken.START_ARRAY, p.nextToken());

    assertToken(JsonToken.VALUE_STRING, p.nextToken());
    assertEquals("a", p.currentText());

    assertToken(JsonToken.VALUE_STRING, p.nextToken());
    assertEquals("b", p.currentText());

    // ALLOW_TRAILING_COMMA takes priority over ALLOW_MISSING_VALUES
    if (features.contains(JsonReadFeature.ALLOW_TRAILING_COMMA)) {
      assertToken(JsonToken.END_ARRAY, p.nextToken());
      assertEnd(p);
    } else if (features.contains(JsonReadFeature.ALLOW_MISSING_VALUES)) {
      assertToken(JsonToken.VALUE_NULL, p.nextToken());
      assertToken(JsonToken.END_ARRAY, p.nextToken());
      assertEnd(p);
    } else {
      assertUnexpected(p, ']');
    }
    p.close();
  }

    @MethodSource("getTestCases")
    @ParameterizedTest(name = "Features {0}")
    void arrayTrailingCommas(Collection<JsonReadFeature> features) throws Exception {
        initAsyncMissingValuesInArrayTest(features);
    String json = "[\"a\", \"b\",,]";

    AsyncReaderWrapper p = createParser(factory, json);

    assertEquals(JsonToken.START_ARRAY, p.nextToken());

    assertToken(JsonToken.VALUE_STRING, p.nextToken());
    assertEquals("a", p.currentText());

    assertToken(JsonToken.VALUE_STRING, p.nextToken());
    assertEquals("b", p.currentText());

    // ALLOW_TRAILING_COMMA takes priority over ALLOW_MISSING_VALUES
    if (features.contains(JsonReadFeature.ALLOW_MISSING_VALUES) &&
        features.contains(JsonReadFeature.ALLOW_TRAILING_COMMA)) {
      assertToken(JsonToken.VALUE_NULL, p.nextToken());
      assertToken(JsonToken.END_ARRAY, p.nextToken());
      assertEnd(p);
    } else if (features.contains(JsonReadFeature.ALLOW_MISSING_VALUES)) {
      assertToken(JsonToken.VALUE_NULL, p.nextToken());
      assertToken(JsonToken.VALUE_NULL, p.nextToken());
      assertToken(JsonToken.END_ARRAY, p.nextToken());
      assertEnd(p);
    } else {
      assertUnexpected(p, ',');
    }
    p.close();
  }

    @MethodSource("getTestCases")
    @ParameterizedTest(name = "Features {0}")
    void arrayTrailingCommasTriple(Collection<JsonReadFeature> features) throws Exception {
        initAsyncMissingValuesInArrayTest(features);
    String json = "[\"a\", \"b\",,,]";

    AsyncReaderWrapper p = createParser(factory, json);

    assertEquals(JsonToken.START_ARRAY, p.nextToken());

    assertToken(JsonToken.VALUE_STRING, p.nextToken());
    assertEquals("a", p.currentText());

    assertToken(JsonToken.VALUE_STRING, p.nextToken());
    assertEquals("b", p.currentText());

    // ALLOW_TRAILING_COMMA takes priority over ALLOW_MISSING_VALUES
    if (features.contains(JsonReadFeature.ALLOW_MISSING_VALUES) &&
        features.contains(JsonReadFeature.ALLOW_TRAILING_COMMA)) {
      assertToken(JsonToken.VALUE_NULL, p.nextToken());
      assertToken(JsonToken.VALUE_NULL, p.nextToken());
      assertToken(JsonToken.END_ARRAY, p.nextToken());
      assertEnd(p);
    } else if (features.contains(JsonReadFeature.ALLOW_MISSING_VALUES)) {
      assertToken(JsonToken.VALUE_NULL, p.nextToken());
      assertToken(JsonToken.VALUE_NULL, p.nextToken());
      assertToken(JsonToken.VALUE_NULL, p.nextToken());
      assertToken(JsonToken.END_ARRAY, p.nextToken());
      assertEnd(p);
    } else {
      assertUnexpected(p, ',');
    }
    p.close();
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
