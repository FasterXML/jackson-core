package com.fasterxml.jackson.core.read.loc;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JUnit5TestBase;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LocationOffsets455Test extends JUnit5TestBase
{
    // for [jackson-core#455]
    @Test
    void eofLocationViaReader() throws Exception
    {
        JsonParser p = createParserUsingReader("42");
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(42, p.getIntValue());
        JsonLocation loc = p.currentLocation();
        assertEquals(1, loc.getLineNr());
        assertEquals(3, loc.getColumnNr());
        assertEquals(2, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertNull(p.nextToken());

        loc = p.currentLocation();
        assertEquals(1, loc.getLineNr());
        assertEquals(3, loc.getColumnNr());
        assertEquals(2, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());
        p.close();
    }

    // for [jackson-core#455]
    @Test
    void eofLocationViaStream() throws Exception
    {
        JsonParser p = createParserUsingStream("42", "UTF-8");
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(42, p.getIntValue());
        JsonLocation loc = p.currentLocation();
        assertEquals(1, loc.getLineNr());
        assertEquals(3, loc.getColumnNr());
        assertEquals(2, loc.getByteOffset());

        assertNull(p.nextToken());
        loc = p.currentLocation();
        assertEquals(1, loc.getLineNr());
        assertEquals(3, loc.getColumnNr());
        assertEquals(2, loc.getByteOffset());
        assertEquals(-1, loc.getCharOffset());
        p.close();
    }

}
