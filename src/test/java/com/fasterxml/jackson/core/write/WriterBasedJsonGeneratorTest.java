package com.fasterxml.jackson.core.write;

import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class WriterBasedJsonGeneratorTest extends JUnit5TestBase
{
    private final JsonFactory JSON_MAX_NESTING_1 = JsonFactory.builder()
            .streamWriteConstraints(StreamWriteConstraints.builder().maxNestingDepth(1).build())
            .build();

    @Test
    void nestingDepthWithSmallLimit() throws Exception
    {
        StringWriter sw = new StringWriter();
        try (JsonGenerator gen = JSON_MAX_NESTING_1.createGenerator(sw)) {
            gen.writeStartObject();
            gen.writeFieldName("array");
            gen.writeStartArray();
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException sce) {
            String expected = "Document nesting depth (2) exceeds the maximum allowed (1, from `StreamWriteConstraints.getMaxNestingDepth()`)";
            assertEquals(expected, sce.getMessage());
        }
    }

    @Test
    void nestingDepthWithSmallLimitNestedObject() throws Exception
    {
        StringWriter sw = new StringWriter();
        try (JsonGenerator gen = JSON_MAX_NESTING_1.createGenerator(sw)) {
            gen.writeStartObject();
            gen.writeFieldName("object");
            gen.writeStartObject();
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException sce) {
            String expected = "Document nesting depth (2) exceeds the maximum allowed (1, from `StreamWriteConstraints.getMaxNestingDepth()`)";
            assertEquals(expected, sce.getMessage());
        }
    }
}
