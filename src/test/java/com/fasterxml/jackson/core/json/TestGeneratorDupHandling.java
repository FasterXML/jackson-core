package com.fasterxml.jackson.core.json;

import java.io.*;

import com.fasterxml.jackson.core.*;

public class TestGeneratorDupHandling
    extends com.fasterxml.jackson.core.BaseTest
{
    public void testSimpleDups() throws Exception
    {
        _testSimpleDups(false);
        _testSimpleDups(true);
    }
    
    protected void _testSimpleDups(boolean useStream) throws Exception
    {
        JsonFactory f = new JsonFactory();
        assertFalse(f.isEnabled(JsonGenerator.Feature.STRICT_DUPLICATE_DETECTION));

        // First: fine, when not checking
        _writeSimple0(_generator(f, useStream), "a");
        _writeSimple1(_generator(f, useStream), "b");

        // but not when checking
        f.enable(JsonGenerator.Feature.STRICT_DUPLICATE_DETECTION);
        try {
            _writeSimple0( _generator(f, useStream), "a");
            fail("Should have gotten exception");
        } catch (JsonGenerationException e) {
            verifyException(e, "duplicate field 'a'");
        }

        try {
            _writeSimple1( _generator(f, useStream), "x");
            fail("Should have gotten exception");
        } catch (JsonGenerationException e) {
            verifyException(e, "duplicate field 'x'");
        }
    }

    protected JsonGenerator _generator(JsonFactory f, boolean useStream) throws IOException
    {
        return useStream ?
                f.createGenerator(new ByteArrayOutputStream())
                : f.createGenerator(new StringWriter());
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
