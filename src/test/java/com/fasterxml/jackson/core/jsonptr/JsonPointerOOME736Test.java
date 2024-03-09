package com.fasterxml.jackson.core.jsonptr;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamReadException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JsonPointerOOME736Test extends JUnit5TestBase
{
    // such as https://github.com/nst/JSONTestSuite/blob/master/test_parsing/n_structure_100000_opening_arrays.json
    @Test
    public void testDeepJsonPointer() throws Exception {
        int MAX_DEPTH = 120_000;
        // Create nesting of 120k arrays
        String INPUT = new String(new char[MAX_DEPTH]).replace("\0", "[");
        final JsonFactory f = JsonFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(Integer.MAX_VALUE).build())
                .build();
        JsonParser parser = createParser(f, MODE_READER, INPUT);
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
