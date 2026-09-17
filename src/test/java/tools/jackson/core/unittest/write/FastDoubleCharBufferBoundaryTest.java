package tools.jackson.core.unittest.write;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonToken;
import tools.jackson.core.JsonParser;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.PrettyPrinter;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.core.io.NumberOutput;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.unittest.JacksonCoreTestBase;
import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that {@code WriterBasedJsonGenerator} writing floats/doubles directly
 * into its char buffer (with {@link StreamWriteFeature#USE_FAST_DOUBLE_WRITER})
 * flushes correctly when values land on the output buffer boundary.
 */
public class FastDoubleCharBufferBoundaryTest extends JacksonCoreTestBase
{
    private final JsonFactory FAST_FACTORY = JsonFactory.builder()
            .enable(StreamWriteFeature.USE_FAST_DOUBLE_WRITER)
            .build();

    @Test
    void testDoublesAcrossBufferBoundary() throws Exception
    {
        // Enough long values to cross the default char buffer several times
        final double v = -Double.MIN_NORMAL;
        final String expectedOne = NumberOutput.toString(v, true);
        final int count = 2000;

        StringWriter sw = new StringWriter();
        try (JsonGenerator gen = FAST_FACTORY.createGenerator(ObjectWriteContext.empty(), sw)) {
            gen.writeStartArray();
            for (int i = 0; i < count; ++i) {
                gen.writeNumber(v);
            }
            gen.writeEndArray();
        }
        _verify(sw.toString(), expectedOne, count);
    }

    @Test
    void testFloatsAcrossBufferBoundary() throws Exception
    {
        final float v = -Float.MIN_NORMAL;
        final String expectedOne = NumberOutput.toString(v, true);
        final int count = 3000;

        StringWriter sw = new StringWriter();
        try (JsonGenerator gen = FAST_FACTORY.createGenerator(ObjectWriteContext.empty(), sw)) {
            gen.writeStartArray();
            for (int i = 0; i < count; ++i) {
                gen.writeNumber(v);
            }
            gen.writeEndArray();
        }
        _verify(sw.toString(), expectedOne, count);
    }

    @Test
    void testFloatsAcrossBufferBoundaryBytes() throws Exception
    {
        // Same for UTF8JsonGenerator: float-only so its (smaller) flush reserve is what trips
        final float v = -Float.MIN_NORMAL;
        final String expectedOne = NumberOutput.toString(v, true);
        final int count = 3000;

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JsonGenerator gen = FAST_FACTORY.createGenerator(ObjectWriteContext.empty(), bytes)) {
            gen.writeStartArray();
            for (int i = 0; i < count; ++i) {
                gen.writeNumber(v);
            }
            gen.writeEndArray();
        }
        _verify(bytes.toString(StandardCharsets.UTF_8), expectedOne, count);
    }

    @Test
    void testMixedLengthsAcrossBufferBoundary() throws Exception
    {
        // Varying lengths so that boundary is hit at different offsets
        final double[] values = { 1.0, -Double.MIN_NORMAL, 0.1, 123456789.0, 1.0E-300, -2.5 };
        final int count = 4000;
        StringBuilder expected = new StringBuilder("[");
        StringWriter sw = new StringWriter();
        try (JsonGenerator gen = FAST_FACTORY.createGenerator(ObjectWriteContext.empty(), sw)) {
            gen.writeStartArray();
            for (int i = 0; i < count; ++i) {
                double v = values[i % values.length];
                gen.writeNumber(v);
                if (i > 0) {
                    expected.append(',');
                }
                expected.append(NumberOutput.toString(v, true));
            }
            gen.writeEndArray();
        }
        expected.append(']');
        assertEquals(expected.toString(), sw.toString());
    }

    // [core#1704]: PrettyPrinter writes indentation from within _verifyValueWrite(),
    // just before the fast path checks for room; varying the indent width walks the
    // value across every offset relative to the output buffer edge.
    @Test
    void testPrettyPrintedAcrossBufferBoundary() throws Exception
    {
        final double[] values = { -Double.MIN_NORMAL, 1.0, 0.1, 123456789.0, 1.0E-300, -2.5 };
        final int count = 500;

        for (int indentLen = 1; indentLen <= 40; ++indentLen) {
            StringBuilder indent = new StringBuilder();
            for (int i = 0; i < indentLen; ++i) {
                indent.append(' ');
            }
            final PrettyPrinter pp = new DefaultPrettyPrinter()
                    .withArrayIndenter(new DefaultIndenter(indent.toString(), "\n"));
            ObjectWriteContext ctxt = new ObjectWriteContext.Base() {
                @Override
                public PrettyPrinter getPrettyPrinter() { return pp; }
            };

            StringWriter sw = new StringWriter();
            try (JsonGenerator gen = FAST_FACTORY.createGenerator(ctxt, sw)) {
                _writeValues(gen, values, count);
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (JsonGenerator gen = FAST_FACTORY.createGenerator(ctxt, bytes)) {
                _writeValues(gen, values, count);
            }
            String desc = "indentLen="+indentLen;
            // Char- and byte-backed generators must agree...
            String doc = sw.toString();
            assertEquals(doc, bytes.toString(StandardCharsets.UTF_8), desc);
            // ... and every value must survive the round trip
            _verifyValues(doc, values, count, desc);
        }
    }

    private void _writeValues(JsonGenerator gen, double[] values, int count)
    {
        gen.writeStartArray();
        for (int i = 0; i < count; ++i) {
            gen.writeNumber(values[i % values.length]);
        }
        gen.writeEndArray();
    }

    private void _verifyValues(String doc, double[] values, int count, String desc) throws Exception
    {
        try (JsonParser p = FAST_FACTORY.createParser(ObjectReadContext.empty(), doc)) {
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            for (int i = 0; i < count; ++i) {
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals(values[i % values.length], p.getDoubleValue(), desc+", index "+i);
            }
            assertToken(JsonToken.END_ARRAY, p.nextToken());
            assertNull(p.nextToken());
        }
    }

    private void _verify(String json, String expectedOne, int count)
    {
        assertTrue(json.startsWith("["));
        assertTrue(json.endsWith("]"));
        String[] parts = json.substring(1, json.length() - 1).split(",");
        assertEquals(count, parts.length);
        for (int i = 0; i < count; ++i) {
            assertEquals(expectedOne, parts[i], "at index " + i);
        }
    }
}
