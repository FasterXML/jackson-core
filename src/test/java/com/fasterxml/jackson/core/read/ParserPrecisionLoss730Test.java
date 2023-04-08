package com.fasterxml.jackson.core.read;

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
        String input = "1E+999";
        StringWriter stringWriter = new StringWriter();

        try (JsonParser parser = JSON_F.createParser(input)) {
            parser.nextToken();
            try (JsonGenerator generator = JSON_F.createGenerator(stringWriter)) {
                generator.copyCurrentEventExact(parser);
            }
        }
        assertEquals(input, stringWriter.toString());
    }
}
