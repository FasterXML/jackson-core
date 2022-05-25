package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.core.BaseTest;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import java.io.StringWriter;

public class ParserPrecisionLoss733Test extends BaseTest {
    final JsonFactory JSON_F = newStreamFactory();

    /**
     * Attempt to pass a BigDecimal value through without losing precision,
     * e.g. for pretty printing a file.
     */
    public void testCopyCurrentEventBigDecimal() throws Exception {
        String input = "1e999";
        JsonParser parser = JSON_F.createParser(input);
        parser.nextToken();
        StringWriter stringWriter = new StringWriter();
        JsonGenerator generator = JSON_F.createGenerator(stringWriter);
        generator.copyCurrentEvent(parser);
        parser.close();
        generator.close();
        String actual = stringWriter.toString();
        assertEquals(input, actual);
    }

    /**
     * Same as {@link #testCopyCurrentEventBigDecimal()} using copyCurrentStructure instead.
     */
    public void testCopyCurrentStructureBigDecimal() throws Exception {
        String input = "[1e999]";
        JsonParser parser = JSON_F.createParser(input);
        parser.nextToken();
        StringWriter stringWriter = new StringWriter();
        JsonGenerator generator = JSON_F.createGenerator(stringWriter);
        generator.copyCurrentStructure(parser);
        parser.close();
        generator.close();
        String actual = stringWriter.toString();
        assertEquals(input, actual);
    }

}
