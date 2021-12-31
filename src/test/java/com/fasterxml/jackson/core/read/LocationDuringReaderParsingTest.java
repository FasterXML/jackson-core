package com.fasterxml.jackson.core.read;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.BaseTest;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;

/**
 * Set of tests that checks getCurrentLocation() and getTokenLocation() are as expected during
 * parsing.
 */
public class LocationDuringReaderParsingTest extends BaseTest
{
    public void testLocationAtEndOfParse() throws Exception
    {
        for (LocationTestCase test : LocationTestCase.values()) {
            //System.out.println(test.name());
            testLocationAtEndOfParse(test);
        }
    }

    public void testInitialLocation() throws Exception
    {
        for (LocationTestCase test : LocationTestCase.values()) {
            //System.out.println(test.name());
            testInitialLocation(test);
        }
    }

    public void testTokenLocations() throws Exception
    {
        for (LocationTestCase test : LocationTestCase.values()) {
            //System.out.println(test.name());
            testTokenLocations(test);
        }
    }

    private void testLocationAtEndOfParse(LocationTestCase test) throws Exception
    {
        JsonParser p = createParserUsingReader(test.json);
        while (p.nextToken() != null) {
            p.nextToken();
        }
        assertCurrentLocation(p, test.getFinalLocation());
        p.close();
    }

    private void testInitialLocation(LocationTestCase test) throws Exception
    {
        JsonParser p = createParserUsingReader(test.json);
        JsonLocation loc = p.getCurrentLocation();
        p.close();

        assertLocation(loc, at(1, 1, 0));
    }

    private void testTokenLocations(LocationTestCase test) throws Exception
    {
        JsonParser p = createParserUsingReader(test.json);
        int i = 0;
        while (p.nextToken() != null) {
            assertTokenLocation(p, test.locations.get(i));
            i++;
        }
        assertEquals(test.locations.size(), i + 1); // last LocData is end of stream
        p.close();
    }

    private void assertCurrentLocation(JsonParser p, LocData loc)
    {
        assertLocation(p.getCurrentLocation(), loc);
    }

    private void assertTokenLocation(JsonParser p, LocData loc)
    {
        assertLocation(p.getTokenLocation(), loc);
    }

    private void assertLocation(JsonLocation pLoc, LocData loc)
    {
        String expected = String.format("(%d, %d, %d)",
                loc.lineNumber, loc.columnNumber, loc.offset);
        String actual = String.format("(%d, %d, %d)", pLoc.getLineNr(), pLoc.getColumnNr(),
                pLoc.getByteOffset() == -1 ? pLoc.getCharOffset() : pLoc.getByteOffset());
        assertEquals(expected, actual);
    }

    static class LocData
    {
        long lineNumber;
        long columnNumber;
        long offset;

        LocData(long lineNumber, long columnNumber, long offset)
        {
            this.lineNumber = lineNumber;
            this.columnNumber = columnNumber;
            this.offset = offset;
        }
    }

    static LocData at(long lineNumber, long columnNumber, long offset)
    {
        return new LocData(lineNumber, columnNumber, offset);
    }

    /**
     * Adapted liberally from https://github.com/leadpony/jsonp-test-suite, also
     * released under the Apache License v2.
     */
    enum LocationTestCase
    {
        SIMPLE_VALUE("42", at(1, 1, 0), at(1, 3, 2)),

        SIMPLE_VALUE_WITH_PADDING("   1337  ", at(1, 4, 3), at(1, 10, 9)),

        SIMPLE_VALUE_WITH_MULTIBYTE_CHARS("\"Правда\"",
                at(1, 1, 0),
                at(1, 9, 8)
        ),

        SIMPLE_VALUE_INCLUDING_SURROGATE_PAIR_CHARS("\"a П \uD83D\uDE01\"",
                at(1, 1, 0),
                at(1, 9, 8) // reader counts surrogate pairs as two chars
        ),

        ARRAY_IN_ONE_LINE("[\"hello\",42,true]",
                at(1, 1, 0), // [
                at(1, 2, 1), // "hello"
                at(1, 10, 9), // 42
                at(1, 13, 12), // true
                at(1, 17, 16), // ]
                at(1, 18, 17) // end of input
        ),

        ARRAY_IN_ONE_LINE_WITH_PADDING("  [ \"hello\" ,   42   ,   true   ]   ",
                at(1, 3, 2), // [
                at(1, 5, 4), // "hello"
                at(1, 17, 16), // 42
                at(1, 26, 25), // true
                at(1, 33, 32), // ]
                at(1, 37, 36) // end of input
        ),

        ARRAY_IN_MULTIPLE_LINES("[\n" + "    \"hello\",\n" + "    42,\n" + "    true\n" + "]",
                at(1, 1, 0), // [
                at(2, 5, 6), // "hello"
                at(3, 5, 19), // 42
                at(4, 5, 27), // true
                at(5, 1, 32), // ]
                at(5, 2, 33) // end of input
        ),

        ARRAY_IN_MULTIPLE_LINES_WITH_WEIRD_SPACING(" [\n" + "  \"hello\" ,  \n" + " 42   ,\n" + "      true\n" + " ]",
                at(1, 2, 1), // [
                at(2, 3, 5), // "hello"
                at(3, 2, 18), // 42
                at(4, 7, 31), // true
                at(5, 2, 37), // ]
                at(5, 3, 38) // end of input
        ),

        ARRAY_IN_MULTIPLE_LINES_CRLF("[\r\n" + "    \"hello\",\r\n" + "    42,\r\n" + "    true\r\n" + "]",
                at(1, 1, 0), // [
                at(2, 5, 7), // "hello"
                at(3, 5, 21), // 42
                at(4, 5, 30), // true
                at(5, 1, 36), // ]
                at(5, 2, 37) // end of input
        ),

        OBJECT_IN_ONE_LINE("{\"first\":\"hello\",\"second\":42}",
                at(1, 1, 0), // {
                at(1, 2, 1), // "first"
                at(1, 10, 9), // "hello"
                at(1, 18, 17), // "second"
                at(1, 27, 26), // 42
                at(1, 29, 28), // }
                at(1, 30, 29) // end of input
        ),

        OBJECT_IN_MULTIPLE_LINES("{\n" + "    \"first\":\"hello\",\n" + "    \"second\":42\n" + "}",
                at(1, 1, 0), // {
                at(2, 5, 6), // "first"
                at(2, 13, 14), // "hello"
                at(3, 5, 27), // "second"
                at(3, 14, 36), // 42
                at(4, 1, 39), // }
                at(4, 2, 40) // end of input
        ),

        OBJECT_IN_MULTIPLE_LINES_CRLF("{\r\n" + "    \"first\":\"hello\",\r\n" + "    \"second\":42\r\n" + "}",
                at(1, 1, 0), // {
                at(2, 5, 7), // "first"
                at(2, 13, 15), // "hello"
                at(3, 5, 29), // "second"
                at(3, 14, 38), // 42
                at(4, 1, 42), // }
                at(4, 2, 43) // end of input
        ),
        ;

        final String json;
        final List<LocData> locations;

        LocationTestCase(String json, LocData... locations)
        {
            this.json = json;
            this.locations = Arrays.asList(locations);
        }

        LocData getFinalLocation()
        {
            return locations.get(locations.size() - 1);
        }
    }
}
