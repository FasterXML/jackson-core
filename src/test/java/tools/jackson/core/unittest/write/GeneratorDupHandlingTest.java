package tools.jackson.core.unittest.write;

import java.io.*;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.core.TokenStreamFactory;
import tools.jackson.core.exc.StreamWriteException;
import tools.jackson.core.unittest.*;

import static org.junit.jupiter.api.Assertions.fail;

public class GeneratorDupHandlingTest
    extends JacksonCoreTestBase
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
        f = f.rebuild().enable(StreamWriteFeature.STRICT_DUPLICATE_DETECTION).build();
        try (JsonGenerator g1 = _generator(f, useStream)) {
            _writeSimple0(g1, "a");
            fail("Should have gotten exception");
        } catch (StreamWriteException e) {
            verifyException(e, "duplicate Object property \"a\"");
        }

        try (JsonGenerator g2 = _generator(f, useStream)) {
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
