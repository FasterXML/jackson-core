package com.fasterxml.jackson.core.jsonptr;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamReadException;

public class JsonPointerOOME736Test extends BaseTest
{
    // such as https://github.com/nst/JSONTestSuite/blob/master/test_parsing/n_structure_100000_opening_arrays.json
    public void testDeepJsonPointer() throws Exception {
        int MAX_DEPTH = 120_000;
        // Create nesting of 120k arrays
        String INPUT = new String(new char[MAX_DEPTH]).replace("\0", "[");
        JsonParser parser = createParser(MODE_READER, INPUT);
        try {
            while (true) {
                parser.nextToken();
            }
        } catch (StreamReadException e) {
            verifyException(e, "Unexpected end");
            JsonStreamContext parsingContext = parser.getParsingContext();
            JsonPointer jsonPointer = parsingContext.pathAsPointer(); // OOME
            String pointer = jsonPointer.toString();
            String expected = new String(new char[MAX_DEPTH - 1]).replace("\0", "/0");
            assertEquals(expected, pointer);
        }
        parser.close();
    }
}
