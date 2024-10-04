package com.fasterxml.jackson.core.read;

import java.io.IOException;
import java.util.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.json.UTF8DataInputJsonParser;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("resource")
public class TrailingCommasTest extends JUnit5TestBase {

    JsonFactory factory;
    Set<JsonReadFeature> features;
    int mode;

    public void initTrailingCommasTest(int mode, List<JsonReadFeature> features) {
      this.features = new HashSet<>(features);
      JsonFactoryBuilder b = (JsonFactoryBuilder) JsonFactory.builder();
      for (JsonReadFeature feature : features) {
          b = b.enable(feature);
      }
      factory = b.build();
      this.mode = mode;
  }

  public static Collection<Object[]> getTestCases() {
    ArrayList<Object[]> cases = new ArrayList<>();

    for (int mode : ALL_MODES) {
      cases.add(new Object[]{mode, Collections.emptyList()});
      cases.add(new Object[]{mode, Arrays.asList(JsonReadFeature.ALLOW_MISSING_VALUES)});
      cases.add(new Object[]{mode, Arrays.asList(JsonReadFeature.ALLOW_TRAILING_COMMA)});
      cases.add(new Object[]{mode, Arrays.asList(JsonReadFeature.ALLOW_MISSING_VALUES,
              JsonReadFeature.ALLOW_TRAILING_COMMA)});
    }

    return cases;
  }

    @MethodSource("getTestCases")
    @ParameterizedTest(name = "Mode {0}, Features {1}")
    void arrayBasic(int mode, List<JsonReadFeature> features) throws Exception {
        initTrailingCommasTest(mode, features);
    String json = "[\"a\", \"b\"]";

    try (JsonParser p = createParser(factory, mode, json)) {

      assertEquals(JsonToken.START_ARRAY, p.nextToken());

      assertToken(JsonToken.VALUE_STRING, p.nextToken());
      assertEquals("a", p.getText());

      assertToken(JsonToken.VALUE_STRING, p.nextToken());
      assertEquals("b", p.getText());

      assertEquals(JsonToken.END_ARRAY, p.nextToken());
      assertEnd(p);
    }
  }

    @MethodSource("getTestCases")
    @ParameterizedTest(name = "Mode {0}, Features {1}")
    void arrayInnerComma(int mode, List<JsonReadFeature> features) throws Exception {
        initTrailingCommasTest(mode, features);
    String json = "[\"a\",, \"b\"]";

    try (JsonParser p = createParser(factory, mode, json)) {

      assertEquals(JsonToken.START_ARRAY, p.nextToken());

      assertToken(JsonToken.VALUE_STRING, p.nextToken());
      assertEquals("a", p.getText());

      if (!features.contains(JsonReadFeature.ALLOW_MISSING_VALUES)) {
        assertUnexpected(p, ',');
        return;
      }

      assertToken(JsonToken.VALUE_NULL, p.nextToken());

      assertToken(JsonToken.VALUE_STRING, p.nextToken());
      assertEquals("b", p.getText());

      assertEquals(JsonToken.END_ARRAY, p.nextToken());
      assertEnd(p);
    }
  }

    @MethodSource("getTestCases")
    @ParameterizedTest(name = "Mode {0}, Features {1}")
    void arrayLeadingComma(int mode, List<JsonReadFeature> features) throws Exception {
        initTrailingCommasTest(mode, features);
    String json = "[,\"a\", \"b\"]";

    try (JsonParser p = createParser(factory, mode, json)) {

      assertEquals(JsonToken.START_ARRAY, p.nextToken());

      if (!features.contains(JsonReadFeature.ALLOW_MISSING_VALUES)) {
        assertUnexpected(p, ',');
        return;
      }

      assertToken(JsonToken.VALUE_NULL, p.nextToken());

      assertToken(JsonToken.VALUE_STRING, p.nextToken());
      assertEquals("a", p.getText());

      assertToken(JsonToken.VALUE_STRING, p.nextToken());
      assertEquals("b", p.getText());

      assertEquals(JsonToken.END_ARRAY, p.nextToken());
      assertEnd(p);
    }
  }

    @MethodSource("getTestCases")
    @ParameterizedTest(name = "Mode {0}, Features {1}")
    void arrayTrailingComma(int mode, List<JsonReadFeature> features) throws Exception {
        initTrailingCommasTest(mode, features);
    String json = "[\"a\", \"b\",]";

    try (JsonParser p = createParser(factory, mode, json)) {

      assertEquals(JsonToken.START_ARRAY, p.nextToken());

      assertToken(JsonToken.VALUE_STRING, p.nextToken());
      assertEquals("a", p.getText());

      assertToken(JsonToken.VALUE_STRING, p.nextToken());
      assertEquals("b", p.getText());

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
    }
  }

    @MethodSource("getTestCases")
    @ParameterizedTest(name = "Mode {0}, Features {1}")
    void arrayTrailingCommas(int mode, List<JsonReadFeature> features) throws Exception {
        initTrailingCommasTest(mode, features);
    String json = "[\"a\", \"b\",,]";

    try (JsonParser p = createParser(factory, mode, json)) {

      assertEquals(JsonToken.START_ARRAY, p.nextToken());

      assertToken(JsonToken.VALUE_STRING, p.nextToken());
      assertEquals("a", p.getText());

      assertToken(JsonToken.VALUE_STRING, p.nextToken());
      assertEquals("b", p.getText());

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
    }
  }

    @MethodSource("getTestCases")
    @ParameterizedTest(name = "Mode {0}, Features {1}")
    void arrayTrailingCommasTriple(int mode, List<JsonReadFeature> features) throws Exception {
        initTrailingCommasTest(mode, features);
    String json = "[\"a\", \"b\",,,]";

    try (JsonParser p = createParser(factory, mode, json)) {

      assertEquals(JsonToken.START_ARRAY, p.nextToken());

      assertToken(JsonToken.VALUE_STRING, p.nextToken());
      assertEquals("a", p.getText());

      assertToken(JsonToken.VALUE_STRING, p.nextToken());
      assertEquals("b", p.getText());

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
    }
  }

    @MethodSource("getTestCases")
    @ParameterizedTest(name = "Mode {0}, Features {1}")
    void objectBasic(int mode, List<JsonReadFeature> features) throws Exception {
        initTrailingCommasTest(mode, features);
    String json = "{\"a\": true, \"b\": false}";

    try (JsonParser p = createParser(factory, mode, json)) {

      assertEquals(JsonToken.START_OBJECT, p.nextToken());

      assertToken(JsonToken.FIELD_NAME, p.nextToken());
      assertEquals("a", p.getText());
      assertToken(JsonToken.VALUE_TRUE, p.nextToken());

      assertToken(JsonToken.FIELD_NAME, p.nextToken());
      assertEquals("b", p.getText());
      assertToken(JsonToken.VALUE_FALSE, p.nextToken());

      assertEquals(JsonToken.END_OBJECT, p.nextToken());
      assertEnd(p);
    }
  }

    @MethodSource("getTestCases")
    @ParameterizedTest(name = "Mode {0}, Features {1}")
    void objectInnerComma(int mode, List<JsonReadFeature> features) throws Exception {
        initTrailingCommasTest(mode, features);
    String json = "{\"a\": true,, \"b\": false}";

    try (JsonParser p = createParser(factory, mode, json)) {

      assertEquals(JsonToken.START_OBJECT, p.nextToken());

      assertToken(JsonToken.FIELD_NAME, p.nextToken());
      assertEquals("a", p.getText());
      assertToken(JsonToken.VALUE_TRUE, p.nextToken());

      assertUnexpected(p, ',');
    }
  }

    @MethodSource("getTestCases")
    @ParameterizedTest(name = "Mode {0}, Features {1}")
    void objectLeadingComma(int mode, List<JsonReadFeature> features) throws Exception {
        initTrailingCommasTest(mode, features);
    String json = "{,\"a\": true, \"b\": false}";

    try (JsonParser p = createParser(factory, mode, json)) {

      assertEquals(JsonToken.START_OBJECT, p.nextToken());

      assertUnexpected(p, ',');
    }
  }

    @MethodSource("getTestCases")
    @ParameterizedTest(name = "Mode {0}, Features {1}")
    void objectTrailingComma(int mode, List<JsonReadFeature> features) throws Exception {
        initTrailingCommasTest(mode, features);
    String json = "{\"a\": true, \"b\": false,}";

    try (JsonParser p = createParser(factory, mode, json)) {

      assertEquals(JsonToken.START_OBJECT, p.nextToken());

      assertToken(JsonToken.FIELD_NAME, p.nextToken());
      assertEquals("a", p.getText());
      assertToken(JsonToken.VALUE_TRUE, p.nextToken());

      assertToken(JsonToken.FIELD_NAME, p.nextToken());
      assertEquals("b", p.getText());
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
    @ParameterizedTest(name = "Mode {0}, Features {1}")
    void objectTrailingCommaWithNextFieldName(int mode, List<JsonReadFeature> features) throws Exception {
        initTrailingCommasTest(mode, features);
    String json = "{\"a\": true, \"b\": false,}";

    try (JsonParser p = createParser(factory, mode, json)) {

      assertEquals(JsonToken.START_OBJECT, p.nextToken());
      assertEquals("a", p.nextFieldName());
      assertToken(JsonToken.VALUE_TRUE, p.nextToken());

      assertEquals("b", p.nextFieldName());
      assertToken(JsonToken.VALUE_FALSE, p.nextToken());

      if (features.contains(JsonReadFeature.ALLOW_TRAILING_COMMA)) {
          assertNull(p.nextFieldName());
        assertToken(JsonToken.END_OBJECT, p.currentToken());
        assertEnd(p);
      } else {
        try {
          p.nextFieldName();
          fail("No exception thrown");
        } catch (Exception e) {
          verifyException(e, "Unexpected character ('}' (code 125))");
        }
      }
    }
  }

    @MethodSource("getTestCases")
    @ParameterizedTest(name = "Mode {0}, Features {1}")
    void objectTrailingCommaWithNextFieldNameStr(int mode, List<JsonReadFeature> features) throws Exception {
        initTrailingCommasTest(mode, features);
    String json = "{\"a\": true, \"b\": false,}";

    try (JsonParser p = createParser(factory, mode, json)) {

      assertEquals(JsonToken.START_OBJECT, p.nextToken());

      assertTrue(p.nextFieldName(new SerializedString("a")));
      assertToken(JsonToken.VALUE_TRUE, p.nextToken());

      assertTrue(p.nextFieldName(new SerializedString("b")));
      assertToken(JsonToken.VALUE_FALSE, p.nextToken());

      if (features.contains(JsonReadFeature.ALLOW_TRAILING_COMMA)) {
        assertFalse(p.nextFieldName(new SerializedString("c")));
        assertToken(JsonToken.END_OBJECT, p.currentToken());
        assertEnd(p);
      } else {
        try {
          p.nextFieldName(new SerializedString("c"));
          fail("No exception thrown");
        } catch (Exception e) {
          verifyException(e, "Unexpected character ('}' (code 125))");
        }
      }
    }
  }

    @MethodSource("getTestCases")
    @ParameterizedTest(name = "Mode {0}, Features {1}")
    void objectTrailingCommas(int mode, List<JsonReadFeature> features) throws Exception {
        initTrailingCommasTest(mode, features);
    String json = "{\"a\": true, \"b\": false,,}";

    try (JsonParser p = createParser(factory, mode, json)) {

      assertEquals(JsonToken.START_OBJECT, p.nextToken());

      assertToken(JsonToken.FIELD_NAME, p.nextToken());
      assertEquals("a", p.getText());
      assertToken(JsonToken.VALUE_TRUE, p.nextToken());

      assertToken(JsonToken.FIELD_NAME, p.nextToken());
      assertEquals("b", p.getText());
      assertToken(JsonToken.VALUE_FALSE, p.nextToken());

      assertUnexpected(p, ',');
    }
  }

  private void assertEnd(JsonParser p) throws IOException {
    // Issue #325
    if (!(p instanceof UTF8DataInputJsonParser)) {
      JsonToken next = p.nextToken();
      assertNull(next, "expected end of stream but found " + next);
    }
  }

  private void assertUnexpected(JsonParser p, char c) throws IOException {
    try {
      p.nextToken();
      fail("No exception thrown");
    } catch (Exception e) {
      verifyException(e, String.format("Unexpected character ('%s' (code %d))", c, (int) c));
    }
  }
}
