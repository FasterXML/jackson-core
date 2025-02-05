package tools.jackson.core.unittest.read;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.unittest.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NumberParsing1397Test extends JacksonCoreTestBase
{
    private TokenStreamFactory JSON_F = newStreamFactory();

    final String radiusValue = "179769313486231570000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";
    final String INPUT_JSON = a2q("{ 'results': [ { " +
        "'radius': " + radiusValue + ", " +
        "'type': 'center', " +
        "'center': { " +
        "'x': -11.0, " +
        "'y': -2.0 } } ] }");

    // [jackson-core#1397]
    @Test
    public void issue1397() throws Exception
    {
        for (int mode : ALL_MODES) {
            testIssue(JSON_F, mode, INPUT_JSON, true);
            testIssue(JSON_F, mode, INPUT_JSON, false);
        }
    }
    
    private void testIssue(final TokenStreamFactory jsonF,
                           final int mode,
                           final String json,
                           final boolean checkFirstNumValues) throws Exception
    {
        // checkFirstNumValues=false reproduces the issue in https://github.com/FasterXML/jackson-core/issues/1397
        try (JsonParser p = createParser(jsonF, mode, json)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertEquals("results", p.currentName());
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertEquals("radius", p.currentName());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(JsonParser.NumberType.BIG_INTEGER, p.getNumberType());
            assertEquals(radiusValue, p.getNumberValueDeferred());
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertEquals("type", p.currentName());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals("center", p.getString());
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertEquals("center", p.currentName());
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertEquals("x", p.currentName());
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            if (checkFirstNumValues) {
                assertEquals(JsonParser.NumberType.DOUBLE, p.getNumberType());
                assertEquals(Double.valueOf(-11.0d), p.getNumberValueDeferred());
            }
            assertEquals(Double.valueOf(-11.0d), p.getDoubleValue());
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertEquals("y", p.currentName());
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertEquals(JsonParser.NumberType.DOUBLE, p.getNumberType());
            assertEquals(Double.valueOf(-2.0d), p.getNumberValueDeferred());
            assertEquals(Double.valueOf(-2.0d), p.getDoubleValue());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
            assertToken(JsonToken.END_ARRAY, p.nextToken());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }
    }
}
