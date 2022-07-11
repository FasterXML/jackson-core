package tools.jackson.core.json.async;

import java.util.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import tools.jackson.core.*;
import tools.jackson.core.async.AsyncTestBase;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonFactoryBuilder;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.testsupport.AsyncReaderWrapper;

@RunWith(Parameterized.class)
public class AsyncMissingValuesInObjectTest extends AsyncTestBase
{
    private JsonFactory factory;
    private HashSet<JsonReadFeature> features;

    public AsyncMissingValuesInObjectTest(Collection<JsonReadFeature> features) {
        this.features = new HashSet<JsonReadFeature>(features);
        JsonFactoryBuilder b = JsonFactory.builder();
        for (JsonReadFeature feature : features) {
            b = b.enable(feature);
        }
        this.factory = b.build();
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

    @Test
    public void testObjectBasic() {
        String json = "{\"a\": true, \"b\": false}";

    AsyncReaderWrapper p = createParser(factory, json);

    assertEquals(JsonToken.START_OBJECT, p.nextToken());

    assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
    assertEquals("a", p.currentText());
    assertToken(JsonToken.VALUE_TRUE, p.nextToken());

    assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
    assertEquals("b", p.currentText());
    assertToken(JsonToken.VALUE_FALSE, p.nextToken());

    assertEquals(JsonToken.END_OBJECT, p.nextToken());
    assertEnd(p);
    p.close();
  }

  @Test
  public void testObjectInnerComma() {
    String json = "{\"a\": true,, \"b\": false}";

    AsyncReaderWrapper p = createParser(factory, json);

    assertEquals(JsonToken.START_OBJECT, p.nextToken());

    assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
    assertEquals("a", p.currentText());
    assertToken(JsonToken.VALUE_TRUE, p.nextToken());

    assertUnexpected(p, ',');
    p.close();
  }

  @Test
  public void testObjectLeadingComma() {
    String json = "{,\"a\": true, \"b\": false}";

    AsyncReaderWrapper p = createParser(factory, json);

    assertEquals(JsonToken.START_OBJECT, p.nextToken());

    assertUnexpected(p, ',');
    p.close();
  }

  @Test
  public void testObjectTrailingComma() {
    String json = "{\"a\": true, \"b\": false,}";

    AsyncReaderWrapper p = createParser(factory, json);

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
    p.close();
  }

  @Test
  public void testObjectTrailingCommas() {
    String json = "{\"a\": true, \"b\": false,,}";

    AsyncReaderWrapper p = createParser(factory, json);

    assertEquals(JsonToken.START_OBJECT, p.nextToken());

    assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
    assertEquals("a", p.currentText());
    assertToken(JsonToken.VALUE_TRUE, p.nextToken());

    assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
    assertEquals("b", p.currentText());
    assertToken(JsonToken.VALUE_FALSE, p.nextToken());

    assertUnexpected(p, ',');
    p.close();
  }

  private void assertEnd(AsyncReaderWrapper p) {
      JsonToken next = p.nextToken();
      assertNull("expected end of stream but found " + next, next);
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
