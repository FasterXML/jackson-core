package com.fasterxml.jackson.core.json;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.util.JsonParserSequence;

public class ParserSequenceTest
    extends com.fasterxml.jackson.core.BaseTest
{
    public void testSimple() throws Exception
    {
        JsonParser p1 = JSON_FACTORY.createParser("[ 1 ]");
        JsonParser p2 = JSON_FACTORY.createParser("[ 2 ]");
        JsonParserSequence seq = JsonParserSequence.createFlattened(p1, p2);
        assertEquals(2, seq.containedParsersCount());

        assertFalse(p1.isClosed());
        assertFalse(p2.isClosed());
        assertFalse(seq.isClosed());
        assertToken(JsonToken.START_ARRAY, seq.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, seq.nextToken());
        assertEquals(1, seq.getIntValue());
        assertToken(JsonToken.END_ARRAY, seq.nextToken());
        assertFalse(p1.isClosed());
        assertFalse(p2.isClosed());
        assertFalse(seq.isClosed());
        assertToken(JsonToken.START_ARRAY, seq.nextToken());

        // first parser ought to be closed now
        assertTrue(p1.isClosed());
        assertFalse(p2.isClosed());
        assertFalse(seq.isClosed());

        assertToken(JsonToken.VALUE_NUMBER_INT, seq.nextToken());
        assertEquals(2, seq.getIntValue());
        assertToken(JsonToken.END_ARRAY, seq.nextToken());
        assertTrue(p1.isClosed());
        assertFalse(p2.isClosed());
        assertFalse(seq.isClosed());

        assertNull(seq.nextToken());
        assertTrue(p1.isClosed());
        assertTrue(p2.isClosed());
        assertTrue(seq.isClosed());

        seq.close();
        // redundant, but call to remove IDE warnings
        p1.close();
        p2.close();
    }

    public void testNotNullParserCurrentTokenInParserSequence() throws Exception
    {
        JsonParser p1 = JSON_FACTORY.createParser("[ 1 ]");
        JsonParser p2 = JSON_FACTORY.createParser("[ 2 ]");

        assertToken(p1.getCurrentToken(), (JsonToken)null);
        assertToken(p2.getCurrentToken(), (JsonToken)null);

        assertToken(p1.nextToken(), JsonToken.START_ARRAY);
        assertToken(p2.nextToken(), JsonToken.START_ARRAY);

        JsonParserSequence seq = JsonParserSequence.createFlattened(p1, p2);

        assertToken(seq.getCurrentToken(), JsonToken.START_ARRAY);
        assertToken(JsonToken.VALUE_NUMBER_INT, seq.nextToken());
        assertEquals(seq.getIntValue(), 1);
        assertToken(JsonToken.END_ARRAY, seq.nextToken());

        // Next token should be the current token of the newly switched parser.
        assertToken(seq.nextToken(), JsonToken.START_ARRAY);
        assertToken(JsonToken.VALUE_NUMBER_INT, seq.nextToken());
        assertEquals(seq.getIntValue(), 2);
        assertToken(JsonToken.END_ARRAY, seq.nextToken());

        assertNull(seq.nextToken());
        assertTrue(p1.isClosed());
        assertTrue(p2.isClosed());
        assertTrue(seq.isClosed());

        seq.close();
        // redundant, but call to remove IDE warnings
        p1.close();
        p2.close();
    }
}
