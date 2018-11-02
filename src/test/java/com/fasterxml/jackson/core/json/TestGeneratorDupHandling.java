package com.fasterxml.jackson.core.json;

import java.io.*;

import com.fasterxml.jackson.core.*;

public class TestGeneratorDupHandling
    extends com.fasterxml.jackson.core.BaseTest
{
    public void testSimpleDupsEagerlyBytes() throws Exception {
        _testSimpleDups(true, new JsonFactory());
    }
    public void testSimpleDupsEagerlyChars() throws Exception {
        _testSimpleDups(false, new JsonFactory());
    }

    @SuppressWarnings("resource")
    protected void _testSimpleDups(boolean useStream, JsonFactory f)
            throws Exception
    {
        // First: fine, when not checking
        _writeSimple0(_generator(f, useStream), "a");
        _writeSimple1(_generator(f, useStream), "b");

        // but not when checking
        JsonGenerator g1;

        f = f.rebuild().enable(StreamWriteFeature.STRICT_DUPLICATE_DETECTION).build();
        g1 = _generator(f, useStream);            
        try {
            _writeSimple0(g1, "a");
            fail("Should have gotten exception");
        } catch (JsonGenerationException e) {
            verifyException(e, "duplicate field 'a'");
        }

        JsonGenerator g2;
        g2 = _generator(f, useStream);            
        try {
            _writeSimple1(g2, "x");
            fail("Should have gotten exception");
        } catch (JsonGenerationException e) {
            verifyException(e, "duplicate field 'x'");
        }
    }

    protected JsonGenerator _generator(JsonFactory f, boolean useStream) throws IOException
    {
        return useStream ?
                f.createGenerator(ObjectWriteContext.empty(), new ByteArrayOutputStream())
                : f.createGenerator(ObjectWriteContext.empty(), new StringWriter());
    }

    protected void _writeSimple0(JsonGenerator g, String name) throws IOException
    {
        g.writeStartObject();
        g.writeNumberField(name, 1);
        g.writeNumberField(name, 2);
        g.writeEndObject();
        g.close();
    }

    protected void _writeSimple1(JsonGenerator g, String name) throws IOException
    {
        g.writeStartArray();
        g.writeNumber(3);
        g.writeStartObject();
        g.writeNumberField("foo", 1);
        g.writeNumberField("bar", 1);
        g.writeNumberField(name, 1);
        g.writeNumberField("bar2", 1);
        g.writeNumberField(name, 2);
        g.writeEndObject();
        g.writeEndArray();
        g.close();
    }
}
