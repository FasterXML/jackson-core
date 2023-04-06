package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.core.BaseTest;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import java.io.StringWriter;

// [jackson-core#730]
public class ParserPrecisionLoss730Test extends BaseTest
{
    private final JsonFactory JSON_F = newStreamFactory();

    // [jackson-core#730]
    /**
     * Attempt to pass a BigDecimal value through without losing precision,
     * e.g. for pretty printing a file.
     */
    public void testCopyCurrentEventBigDecimal() throws Exception {
        String input = "1e999";
        StringWriter stringWriter = new StringWriter();

        try (JsonParser parser = JSON_F.createParser(input)) {
            parser.nextToken();
            try (JsonGenerator generator = JSON_F.createGenerator(stringWriter)) {
                generator.copyCurrentEvent(parser);
            }
        }
        assertEquals(input, stringWriter.toString());
    }

    // [jackson-core#730]
    /**
     * Same as {@link #testCopyCurrentEventBigDecimal()} using copyCurrentStructure instead.
     */
    public void testCopyCurrentStructureBigDecimal() throws Exception {
        String input = "[1e999]";
        StringWriter stringWriter = new StringWriter();
        try (JsonParser parser = JSON_F.createParser(input)) {
            parser.nextToken();
            try (JsonGenerator generator = JSON_F.createGenerator(stringWriter)) {
                generator.copyCurrentStructure(parser);
            }
        }
        assertEquals(input, stringWriter.toString());
    }
}
