package com.fasterxml.jackson.core.read;

import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [jackson-core#730]
class ParserPrecisionLoss730Test extends JUnit5TestBase
{
    private final JsonFactory JSON_F = newStreamFactory();

    // [jackson-core#730]
    /**
     * Attempt to pass a BigDecimal value through without losing precision,
     * e.g. for pretty printing a file.
     */
    @Test
    void copyCurrentEventBigDecimal() throws Exception {
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
