package com.fasterxml.jackson.core.json;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.util.JsonParserSequence;

public class ParserSequenceTest
    extends com.fasterxml.jackson.core.BaseTest
{
    private final JsonFactory JSON_FACTORY = new JsonFactory();

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
}
