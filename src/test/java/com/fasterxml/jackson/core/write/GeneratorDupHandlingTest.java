package com.fasterxml.jackson.core.write;

import java.io.*;

import com.fasterxml.jackson.core.*;

public class GeneratorDupHandlingTest
    extends com.fasterxml.jackson.core.BaseTest
{
    public void testSimpleDupsEagerlyBytes() throws Exception {
        _testSimpleDups(true, false, new JsonFactory());
    }
    public void testSimpleDupsEagerlyChars() throws Exception {
        _testSimpleDups(false, false, new JsonFactory());
    }

    // Testing ability to enable checking after construction of
    // generator, not just via JsonFactory
    public void testSimpleDupsLazilyBytes() throws Exception {
        final JsonFactory f = new JsonFactory();
        assertFalse(f.isEnabled(JsonGenerator.Feature.STRICT_DUPLICATE_DETECTION));
        _testSimpleDups(true, true, f);
    }

    public void testSimpleDupsLazilyChars() throws Exception {
        final JsonFactory f = new JsonFactory();
        _testSimpleDups(false, true, f);
    }

    @SuppressWarnings("resource")
    protected void _testSimpleDups(boolean useStream, boolean lazySetting, JsonFactory f)
            throws Exception
    {
        // First: fine, when not checking
        if (!lazySetting) {
            _writeSimple0(_generator(f, useStream), "a");
            _writeSimple1(_generator(f, useStream), "b");
        }

        // but not when checking
        JsonGenerator g1;

        if (lazySetting) {
            g1 = _generator(f, useStream);
            g1.enable(JsonGenerator.Feature.STRICT_DUPLICATE_DETECTION);
        } else {
            f.enable(JsonGenerator.Feature.STRICT_DUPLICATE_DETECTION);
            g1 = _generator(f, useStream);
        }
        try {
            _writeSimple0(g1, "a");
            fail("Should have gotten exception");
        } catch (JsonGenerationException e) {
            verifyException(e, "duplicate field 'a'");
        }

        JsonGenerator g2;
        if (lazySetting) {
            g2 = _generator(f, useStream);
            g2.enable(JsonGenerator.Feature.STRICT_DUPLICATE_DETECTION);
        } else {
            g2 = _generator(f, useStream);
        }
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
