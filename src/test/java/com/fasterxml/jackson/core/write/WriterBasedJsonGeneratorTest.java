package com.fasterxml.jackson.core.write;

import java.io.StringWriter;

import com.fasterxml.jackson.core.BaseTest;
import com.fasterxml.jackson.core.ErrorReportConfiguration;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamWriteConstraints;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.core.io.ContentReference;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.json.WriterBasedJsonGenerator;
import com.fasterxml.jackson.core.util.BufferRecycler;

public class WriterBasedJsonGeneratorTest extends BaseTest
{
    public void testNestingDepthWithSmallLimit() throws Exception
    {
        StringWriter sw = new StringWriter();
        IOContext ioc = _ioContext(StreamWriteConstraints.builder().maxNestingDepth(1).build());
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
        IOContext ioc = _ioContext(StreamWriteConstraints.builder().maxNestingDepth(1).build());
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

    private IOContext _ioContext(StreamWriteConstraints swc) {
        return new IOContext(StreamReadConstraints.defaults(),
                swc,
                ErrorReportConfiguration.defaults(),
                new BufferRecycler(),
                ContentReference.unknown(), true);
    }
}
