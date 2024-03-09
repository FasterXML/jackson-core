package com.fasterxml.jackson.core.read;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.json.UTF8DataInputJsonParser;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("resource")
public class TrailingCommasTest extends BaseTest {

    private JsonFactory factory;
    private Set<JsonReadFeature> features;
    private int mode;

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
    public void testArrayBasic(int mode, List<JsonReadFeature> features) throws Exception {
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
    public void testArrayInnerComma(int mode, List<JsonReadFeature> features) throws Exception {
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
    public void testArrayLeadingComma(int mode, List<JsonReadFeature> features) throws Exception {
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
    public void testArrayTrailingComma(int mode, List<JsonReadFeature> features) throws Exception {
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
    public void testArrayTrailingCommas(int mode, List<JsonReadFeature> features) throws Exception {
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
    public void testArrayTrailingCommasTriple(int mode, List<JsonReadFeature> features) throws Exception {
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
    public void testObjectBasic(int mode, List<JsonReadFeature> features) throws Exception {
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
    public void testObjectInnerComma(int mode, List<JsonReadFeature> features) throws Exception {
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
    public void testObjectLeadingComma(int mode, List<JsonReadFeature> features) throws Exception {
        initTrailingCommasTest(mode, features);
    String json = "{,\"a\": true, \"b\": false}";

    try (JsonParser p = createParser(factory, mode, json)) {

      assertEquals(JsonToken.START_OBJECT, p.nextToken());

      assertUnexpected(p, ',');
    }
  }

    @MethodSource("getTestCases")
    @ParameterizedTest(name = "Mode {0}, Features {1}")
    public void testObjectTrailingComma(int mode, List<JsonReadFeature> features) throws Exception {
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
    public void testObjectTrailingCommaWithNextFieldName(int mode, List<JsonReadFeature> features) throws Exception {
        initTrailingCommasTest(mode, features);
    String json = "{\"a\": true, \"b\": false,}";

    try (JsonParser p = createParser(factory, mode, json)) {

      assertEquals(JsonToken.START_OBJECT, p.nextToken());
      assertEquals("a", p.nextFieldName());
      assertToken(JsonToken.VALUE_TRUE, p.nextToken());

      assertEquals("b", p.nextFieldName());
      assertToken(JsonToken.VALUE_FALSE, p.nextToken());

      if (features.contains(JsonReadFeature.ALLOW_TRAILING_COMMA)) {
        assertEquals(null, p.nextFieldName());
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
    public void testObjectTrailingCommaWithNextFieldNameStr(int mode, List<JsonReadFeature> features) throws Exception {
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
    public void testObjectTrailingCommas(int mode, List<JsonReadFeature> features) throws Exception {
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
