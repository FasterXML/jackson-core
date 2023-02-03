package com.fasterxml.jackson.core.read;

import com.fasterxml.jackson.core.*;

// Tests mostly for [core#229]
public class LocationInArrayTest extends com.fasterxml.jackson.core.BaseTest
{
    final JsonFactory JSON_F = new JsonFactory();

    // for [core#229]
    public void testOffsetInArraysBytes() throws Exception {
        _testOffsetInArrays(true);
    }

    // for [core#229]
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
        _assertLocation(useBytes, p.getTokenLocation(), 2L, 1, 3);
        _assertLocation(useBytes, p.getCurrentLocation(), 3L, 1, 4);

        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        _assertLocation(useBytes, p.getTokenLocation(), 3L, 1, 4);
        assertEquals(10, p.getIntValue()); // just to ensure read proceeds to end
        // 2-digits so
        _assertLocation(useBytes, p.getCurrentLocation(), 5L, 1, 6);

        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        _assertLocation(useBytes, p.getTokenLocation(), 7L, 1, 8);
        assertEquals(251, p.getIntValue()); // just to ensure read proceeds to end
        _assertLocation(useBytes, p.getCurrentLocation(), 10L, 1, 11);

        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        _assertLocation(useBytes, p.getTokenLocation(), 15L, 2, 4);
        assertEquals(3, p.getIntValue());
        _assertLocation(useBytes, p.getCurrentLocation(), 16L, 2, 5);

        assertToken(JsonToken.END_ARRAY, p.nextToken());
        _assertLocation(useBytes, p.getTokenLocation(), 18L, 2, 7);
        _assertLocation(useBytes, p.getCurrentLocation(), 19L, 2, 8);

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
