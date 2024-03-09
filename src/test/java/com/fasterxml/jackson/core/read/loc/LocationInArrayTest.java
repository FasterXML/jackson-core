package com.fasterxml.jackson.core.read.loc;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Tests mostly for [core#229]
public class LocationInArrayTest extends TestBase
{
    final JsonFactory JSON_F = new JsonFactory();

    // for [core#229]
    @Test
    public void testOffsetInArraysBytes() throws Exception {
        _testOffsetInArrays(true);
    }

    // for [core#229]
    @Test
    public void testOffsetInArraysChars() throws Exception {
        _testOffsetInArrays(false);
    }

    private void _testOffsetInArrays(boolean useBytes) throws Exception
    {
        JsonParser p;
        final String DOC = "  [10, 251,\n   3  ]";

        // first, char based:
        p = useBytes ? JSON_F.createParser(DOC.getBytes("UTF-8"))
                : JSON_F.createParser(DOC.toCharArray());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        _assertLocation(useBytes, p.currentTokenLocation(), 2L, 1, 3);
        _assertLocation(useBytes, p.currentLocation(), 3L, 1, 4);

        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        _assertLocation(useBytes, p.currentTokenLocation(), 3L, 1, 4);
        assertEquals(10, p.getIntValue()); // just to ensure read proceeds to end
        // 2-digits so
        _assertLocation(useBytes, p.currentLocation(), 5L, 1, 6);

        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        _assertLocation(useBytes, p.currentTokenLocation(), 7L, 1, 8);
        assertEquals(251, p.getIntValue()); // just to ensure read proceeds to end
        _assertLocation(useBytes, p.currentLocation(), 10L, 1, 11);

        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        _assertLocation(useBytes, p.currentTokenLocation(), 15L, 2, 4);
        assertEquals(3, p.getIntValue());
        _assertLocation(useBytes, p.currentLocation(), 16L, 2, 5);

        assertToken(JsonToken.END_ARRAY, p.nextToken());
        _assertLocation(useBytes, p.currentTokenLocation(), 18L, 2, 7);
        _assertLocation(useBytes, p.currentLocation(), 19L, 2, 8);

        p.close();
    }

    private void _assertLocation(boolean useBytes, JsonLocation loc, long offset, int row, int col)
    {
        assertEquals(row, loc.getLineNr());
        assertEquals(col, loc.getColumnNr());

        if (useBytes) {
            assertEquals(offset, loc.getByteOffset());
        } else {
            assertEquals(offset, loc.getCharOffset());
        }
    }
}
