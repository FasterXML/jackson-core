package tools.jackson.core.write;

import java.io.*;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamWriteException;

public class GeneratorDupHandlingTest
    extends tools.jackson.core.BaseTest
{
    public void testSimpleDupsEagerlyBytes() {
        _testSimpleDups(true, newStreamFactory());
    }
    public void testSimpleDupsEagerlyChars() {
        _testSimpleDups(false, newStreamFactory());
    }

    @SuppressWarnings("resource")
    protected void _testSimpleDups(boolean useStream, TokenStreamFactory f)
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
        } catch (StreamWriteException e) {
            verifyException(e, "duplicate Object property \"a\"");
        }

        JsonGenerator g2;
        g2 = _generator(f, useStream);
        try {
            _writeSimple1(g2, "x");
            fail("Should have gotten exception");
        } catch (StreamWriteException e) {
            verifyException(e, "duplicate Object property \"x\"");
        }
    }

    protected JsonGenerator _generator(TokenStreamFactory f, boolean useStream)
    {
        return useStream ?
                f.createGenerator(ObjectWriteContext.empty(), new ByteArrayOutputStream())
                : f.createGenerator(ObjectWriteContext.empty(), new StringWriter());
    }

    protected void _writeSimple0(JsonGenerator g, String name)
    {
        g.writeStartObject();
        g.writeNumberProperty(name, 1);
        g.writeNumberProperty(name, 2);
        g.writeEndObject();
        g.close();
    }

    protected void _writeSimple1(JsonGenerator g, String name)
    {
        g.writeStartArray();
        g.writeNumber(3);
        g.writeStartObject();
        g.writeNumberProperty("foo", 1);
        g.writeNumberProperty("bar", 1);
        g.writeNumberProperty(name, 1);
        g.writeNumberProperty("bar2", 1);
        g.writeNumberProperty(name, 2);
        g.writeEndObject();
        g.writeEndArray();
        g.close();
    }
}
