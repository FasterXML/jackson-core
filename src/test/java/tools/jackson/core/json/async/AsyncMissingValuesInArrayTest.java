package tools.jackson.core.json.async;

import java.util.*;

import tools.jackson.core.*;
import tools.jackson.core.async.AsyncTestBase;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonFactoryBuilder;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.testsupport.AsyncReaderWrapper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class AsyncMissingValuesInArrayTest extends AsyncTestBase
{
    private final JsonFactory factory;
    private final HashSet<JsonReadFeature> features;

    public AsyncMissingValuesInArrayTest(Collection<JsonReadFeature> features) {
        this.features = new HashSet<JsonReadFeature>(features);
        JsonFactoryBuilder b = JsonFactory.builder();
        for (JsonReadFeature feature : features) {
            b = b.enable(feature);
        }
        factory = b.build();
    }

    @Parameterized.Parameters(name = "Features {0}")
    public static Collection<EnumSet<JsonReadFeature>> getTestCases()
    {
        List<EnumSet<JsonReadFeature>> cases = new ArrayList<EnumSet<JsonReadFeature>>();
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

    @Test
    public void testArrayInnerComma() throws Exception {
    String json = "[\"a\",, \"b\"]";

    AsyncReaderWrapper p = createParser(factory, json);

    assertEquals(JsonToken.START_ARRAY, p.nextToken());

    assertToken(JsonToken.VALUE_STRING, p.nextToken());
    assertEquals("a", p.currentText());

    if (!features.contains(JsonReadFeature.ALLOW_MISSING_VALUES)) {
      assertUnexpected(p, ',');
      return;
    }

    assertToken(JsonToken.VALUE_NULL, p.nextToken());

    assertToken(JsonToken.VALUE_STRING, p.nextToken());
    assertEquals("b", p.currentText());

    assertEquals(JsonToken.END_ARRAY, p.nextToken());
    assertEnd(p);
  }

  @Test
  public void testArrayLeadingComma() throws Exception {
    String json = "[,\"a\", \"b\"]";

    AsyncReaderWrapper p = createParser(factory, json);

    assertEquals(JsonToken.START_ARRAY, p.nextToken());

    if (!features.contains(JsonReadFeature.ALLOW_MISSING_VALUES)) {
      assertUnexpected(p, ',');
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

  @Test
  public void testArrayTrailingComma() throws Exception {
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

  @Test
  public void testArrayTrailingCommas() throws Exception {
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

  @Test
  public void testArrayTrailingCommasTriple() throws Exception {
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

  private void assertEnd(AsyncReaderWrapper p) {
      JsonToken next = p.nextToken();
      assertNull("expected end of stream but found " + next, next);
  }

  private void assertUnexpected(AsyncReaderWrapper p, char c) {
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
