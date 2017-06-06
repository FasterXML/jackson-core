package com.fasterxml.jackson.core.json.async;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class AsyncMissingValuesInArrayTest extends AsyncTestBase
{
    private final JsonFactory factory;
    private final HashSet<JsonParser.Feature> features;

    public AsyncMissingValuesInArrayTest(Collection<JsonParser.Feature> features) {
        this.factory = new JsonFactory();
        this.features = new HashSet<JsonParser.Feature>(features);

        for (JsonParser.Feature feature : features) {
            factory.enable(feature);
        }
    }

    @Parameterized.Parameters(name = "Features {0}")
    public static Collection<EnumSet<JsonParser.Feature>> getTestCases()
    {
        List<EnumSet<JsonParser.Feature>> cases = new ArrayList<EnumSet<JsonParser.Feature>>();
        cases.add(EnumSet.noneOf(JsonParser.Feature.class));
        cases.add(EnumSet.of(Feature.ALLOW_MISSING_VALUES));
        cases.add(EnumSet.of(Feature.ALLOW_TRAILING_COMMA));
        cases.add(EnumSet.of(Feature.ALLOW_MISSING_VALUES, Feature.ALLOW_TRAILING_COMMA));
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

    if (!features.contains(Feature.ALLOW_MISSING_VALUES)) {
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

    if (!features.contains(Feature.ALLOW_MISSING_VALUES)) {
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
    if (features.contains(Feature.ALLOW_TRAILING_COMMA)) {
      assertToken(JsonToken.END_ARRAY, p.nextToken());
      assertEnd(p);
    } else if (features.contains(Feature.ALLOW_MISSING_VALUES)) {
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
    if (features.contains(Feature.ALLOW_MISSING_VALUES) &&
        features.contains(Feature.ALLOW_TRAILING_COMMA)) {
      assertToken(JsonToken.VALUE_NULL, p.nextToken());
      assertToken(JsonToken.END_ARRAY, p.nextToken());
      assertEnd(p);
    } else if (features.contains(Feature.ALLOW_MISSING_VALUES)) {
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
    if (features.contains(Feature.ALLOW_MISSING_VALUES) &&
        features.contains(Feature.ALLOW_TRAILING_COMMA)) {
      assertToken(JsonToken.VALUE_NULL, p.nextToken());
      assertToken(JsonToken.VALUE_NULL, p.nextToken());
      assertToken(JsonToken.END_ARRAY, p.nextToken());
      assertEnd(p);
    } else if (features.contains(Feature.ALLOW_MISSING_VALUES)) {
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
      assertNull("expected end of stream but found " + next, next);
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
