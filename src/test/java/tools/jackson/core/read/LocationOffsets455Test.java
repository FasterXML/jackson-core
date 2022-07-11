package tools.jackson.core.read;

import tools.jackson.core.JsonLocation;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;

public class LocationOffsets455Test extends tools.jackson.core.BaseTest
{
    // for [jackson-core#455]
    public void testEOFLocationViaReader() throws Exception
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
    public void testEOFLocationViaStream() throws Exception
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
