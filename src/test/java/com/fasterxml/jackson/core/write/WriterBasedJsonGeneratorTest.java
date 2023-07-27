package com.fasterxml.jackson.core.write;

import com.fasterxml.jackson.core.BaseTest;
import com.fasterxml.jackson.core.ErrorReportConfiguration;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.StreamWriteConstraints;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.core.io.ContentReference;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.json.WriterBasedJsonGenerator;
import com.fasterxml.jackson.core.util.BufferRecycler;

import java.io.StringWriter;

public class WriterBasedJsonGeneratorTest extends BaseTest
{
    private final JsonFactory JSON_F = new JsonFactory();

    public void testNestingDepthWithSmallLimit() throws Exception
    {
        StringWriter sw = new StringWriter();
        IOContext ioc = new IOContext(null,
                StreamWriteConstraints.builder().maxNestingDepth(1).build(),
                new BufferRecycler(),
                ContentReference.rawReference(sw), true,
                ErrorReportConfiguration.defaults());
        try (JsonGenerator gen = new WriterBasedJsonGenerator(ioc, 0, null, sw, '"')) {
            gen.writeStartObject();
            gen.writeFieldName("array");
            gen.writeStartArray();
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException sce) {
            String expected = "Document nesting depth (2) exceeds the maximum allowed (1, from `StreamWriteConstraints.getMaxNestingDepth()`)";
            assertEquals(expected, sce.getMessage());
        }
    }

    public void testNestingDepthWithSmallLimitNestedObject() throws Exception
    {
        StringWriter sw = new StringWriter();
        IOContext ioc = new IOContext(null,
                StreamWriteConstraints.builder().maxNestingDepth(1).build(),
                new BufferRecycler(),
                ContentReference.rawReference(sw), true,
                ErrorReportConfiguration.defaults());
        try (JsonGenerator gen = new WriterBasedJsonGenerator(ioc, 0, null, sw, '"')) {
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
