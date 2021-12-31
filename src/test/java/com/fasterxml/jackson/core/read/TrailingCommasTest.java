package com.fasterxml.jackson.core.read;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.json.UTF8DataInputJsonParser;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
@SuppressWarnings("resource")
public class TrailingCommasTest extends BaseTest {

  private final JsonFactory factory;
  private final Set<JsonReadFeature> features;
  private final int mode;

  public TrailingCommasTest(int mode, List<JsonReadFeature> features) {
      this.features = new HashSet<JsonReadFeature>(features);
      JsonFactoryBuilder b = (JsonFactoryBuilder) JsonFactory.builder();
      for (JsonReadFeature feature : features) {
          b = b.enable(feature);
      }
      factory = b.build();
      this.mode = mode;
  }

  @Parameterized.Parameters(name = "Mode {0}, Features {1}")
  public static Collection<Object[]> getTestCases() {
    ArrayList<Object[]> cases = new ArrayList<Object[]>();

    for (int mode : ALL_MODES) {
      cases.add(new Object[]{mode, Collections.emptyList()});
      cases.add(new Object[]{mode, Arrays.asList(JsonReadFeature.ALLOW_MISSING_VALUES)});
      cases.add(new Object[]{mode, Arrays.asList(JsonReadFeature.ALLOW_TRAILING_COMMA)});
      cases.add(new Object[]{mode, Arrays.asList(JsonReadFeature.ALLOW_MISSING_VALUES,
              JsonReadFeature.ALLOW_TRAILING_COMMA)});
    }

    return cases;
  }

  @Test
  public void testArrayBasic() throws Exception {
    String json = "[\"a\", \"b\"]";

    JsonParser p = createParser(factory, mode, json);

    assertEquals(JsonToken.START_ARRAY, p.nextToken());

    assertToken(JsonToken.VALUE_STRING, p.nextToken());
    assertEquals("a", p.getText());

    assertToken(JsonToken.VALUE_STRING, p.nextToken());
    assertEquals("b", p.getText());

    assertEquals(JsonToken.END_ARRAY, p.nextToken());
    assertEnd(p);
  }

  @Test
  public void testArrayInnerComma() throws Exception {
    String json = "[\"a\",, \"b\"]";

    JsonParser p = createParser(factory, mode, json);

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

  @Test
  public void testArrayLeadingComma() throws Exception {
    String json = "[,\"a\", \"b\"]";

    JsonParser p = createParser(factory, mode, json);

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
    p.close();
  }

  @Test
  public void testArrayTrailingComma() throws Exception {
    String json = "[\"a\", \"b\",]";

    JsonParser p = createParser(factory, mode, json);

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
    p.close();
  }

  @Test
  public void testArrayTrailingCommas() throws Exception {
    String json = "[\"a\", \"b\",,]";

    JsonParser p = createParser(factory, mode, json);

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
    p.close();
  }

  @Test
  public void testArrayTrailingCommasTriple() throws Exception {
    String json = "[\"a\", \"b\",,,]";

    JsonParser p = createParser(factory, mode, json);

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
    p.close();
  }

  @Test
  public void testObjectBasic() throws Exception {
    String json = "{\"a\": true, \"b\": false}";

    JsonParser p = createParser(factory, mode, json);

    assertEquals(JsonToken.START_OBJECT, p.nextToken());

    assertToken(JsonToken.FIELD_NAME, p.nextToken());
    assertEquals("a", p.getText());
    assertToken(JsonToken.VALUE_TRUE, p.nextToken());

    assertToken(JsonToken.FIELD_NAME, p.nextToken());
    assertEquals("b", p.getText());
    assertToken(JsonToken.VALUE_FALSE, p.nextToken());

    assertEquals(JsonToken.END_OBJECT, p.nextToken());
    assertEnd(p);
    p.close();
  }

  @Test
  public void testObjectInnerComma() throws Exception {
    String json = "{\"a\": true,, \"b\": false}";

    JsonParser p = createParser(factory, mode, json);

    assertEquals(JsonToken.START_OBJECT, p.nextToken());

    assertToken(JsonToken.FIELD_NAME, p.nextToken());
    assertEquals("a", p.getText());
    assertToken(JsonToken.VALUE_TRUE, p.nextToken());

    assertUnexpected(p, ',');
    p.close();
  }

  @Test
  public void testObjectLeadingComma() throws Exception {
    String json = "{,\"a\": true, \"b\": false}";

    JsonParser p = createParser(factory, mode, json);

    assertEquals(JsonToken.START_OBJECT, p.nextToken());

    assertUnexpected(p, ',');
    p.close();
  }

  @Test
  public void testObjectTrailingComma() throws Exception {
    String json = "{\"a\": true, \"b\": false,}";

    JsonParser p = createParser(factory, mode, json);

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
    p.close();
  }

  @Test
  public void testObjectTrailingCommaWithNextFieldName() throws Exception {
    String json = "{\"a\": true, \"b\": false,}";

    JsonParser p = createParser(factory, mode, json);

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
    p.close();
  }

  @Test
  public void testObjectTrailingCommaWithNextFieldNameStr() throws Exception {
    String json = "{\"a\": true, \"b\": false,}";

    JsonParser p = createParser(factory, mode, json);

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
    p.close();
  }

  @Test
  public void testObjectTrailingCommas() throws Exception {
    String json = "{\"a\": true, \"b\": false,,}";

    JsonParser p = createParser(factory, mode, json);

    assertEquals(JsonToken.START_OBJECT, p.nextToken());

    assertToken(JsonToken.FIELD_NAME, p.nextToken());
    assertEquals("a", p.getText());
    assertToken(JsonToken.VALUE_TRUE, p.nextToken());

    assertToken(JsonToken.FIELD_NAME, p.nextToken());
    assertEquals("b", p.getText());
    assertToken(JsonToken.VALUE_FALSE, p.nextToken());

    assertUnexpected(p, ',');
    p.close();
  }

  private void assertEnd(JsonParser p) throws IOException {
    // Issue #325
    if (!(p instanceof UTF8DataInputJsonParser)) {
      JsonToken next = p.nextToken();
      assertNull("expected end of stream but found " + next, next);
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
