package com.fasterxml.jackson.core.json.async;

import java.io.IOException;
import java.util.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;

@RunWith(Parameterized.class)
public class AsyncMissingValuesInObjectTest extends AsyncTestBase
{
    private JsonFactory factory;
    private HashSet<JsonParser.Feature> features;

    public AsyncMissingValuesInObjectTest(Collection<JsonParser.Feature> features) {
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

    @Test
    public void testObjectBasic() throws Exception {
        String json = "{\"a\": true, \"b\": false}";

    AsyncReaderWrapper p = createParser(factory, json);

    assertEquals(JsonToken.START_OBJECT, p.nextToken());

    assertToken(JsonToken.FIELD_NAME, p.nextToken());
    assertEquals("a", p.currentText());
    assertToken(JsonToken.VALUE_TRUE, p.nextToken());

    assertToken(JsonToken.FIELD_NAME, p.nextToken());
    assertEquals("b", p.currentText());
    assertToken(JsonToken.VALUE_FALSE, p.nextToken());

    assertEquals(JsonToken.END_OBJECT, p.nextToken());
    assertEnd(p);
    p.close();
  }

  @Test
  public void testObjectInnerComma() throws Exception {
    String json = "{\"a\": true,, \"b\": false}";

    AsyncReaderWrapper p = createParser(factory, json);

    assertEquals(JsonToken.START_OBJECT, p.nextToken());

    assertToken(JsonToken.FIELD_NAME, p.nextToken());
    assertEquals("a", p.currentText());
    assertToken(JsonToken.VALUE_TRUE, p.nextToken());

    assertUnexpected(p, ',');
    p.close();
  }

  @Test
  public void testObjectLeadingComma() throws Exception {
    String json = "{,\"a\": true, \"b\": false}";

    AsyncReaderWrapper p = createParser(factory, json);

    assertEquals(JsonToken.START_OBJECT, p.nextToken());

    assertUnexpected(p, ',');
    p.close();
  }

  @Test
  public void testObjectTrailingComma() throws Exception {
    String json = "{\"a\": true, \"b\": false,}";

    AsyncReaderWrapper p = createParser(factory, json);

    assertEquals(JsonToken.START_OBJECT, p.nextToken());

    assertToken(JsonToken.FIELD_NAME, p.nextToken());
    assertEquals("a", p.currentText());
    assertToken(JsonToken.VALUE_TRUE, p.nextToken());

    assertToken(JsonToken.FIELD_NAME, p.nextToken());
    assertEquals("b", p.currentText());
    assertToken(JsonToken.VALUE_FALSE, p.nextToken());

    if (features.contains(Feature.ALLOW_TRAILING_COMMA)) {
      assertToken(JsonToken.END_OBJECT, p.nextToken());
      assertEnd(p);
    } else {
      assertUnexpected(p, '}');
    }
    p.close();
  }

  @Test
  public void testObjectTrailingCommas() throws Exception {
    String json = "{\"a\": true, \"b\": false,,}";

    AsyncReaderWrapper p = createParser(factory, json);

    assertEquals(JsonToken.START_OBJECT, p.nextToken());

    assertToken(JsonToken.FIELD_NAME, p.nextToken());
    assertEquals("a", p.currentText());
    assertToken(JsonToken.VALUE_TRUE, p.nextToken());

    assertToken(JsonToken.FIELD_NAME, p.nextToken());
    assertEquals("b", p.currentText());
    assertToken(JsonToken.VALUE_FALSE, p.nextToken());

    assertUnexpected(p, ',');
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
