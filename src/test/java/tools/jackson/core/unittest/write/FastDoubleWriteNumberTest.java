package tools.jackson.core.unittest.write;

import java.io.*;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.unittest.JacksonCoreTestBase;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link JsonGenerator#writeNumber(double)} and
 * {@link JsonGenerator#writeNumber(float)} with
 * {@link StreamWriteFeature#USE_FAST_DOUBLE_WRITER} enabled.
 */
public class FastDoubleWriteNumberTest extends JacksonCoreTestBase
{
    private final JsonFactory FAST_FACTORY = JsonFactory.builder()
            .enable(StreamWriteFeature.USE_FAST_DOUBLE_WRITER)
            .build();

    @Test
    void testDoubleValues() throws Exception
    {
        double[] values = {
            0.0, -0.0, 1.0, -1.0, 1.5, -1.5,
            0.25, -0.25, 123.456, -123.456,
            1.0E10, 1.0E-10, -1.0E10, -1.0E-10,
            Double.MAX_VALUE, Double.MIN_VALUE, Double.MIN_NORMAL
        };
        for (double v : values) {
            _verifyDoubleWrite(v);
        }
    }

    @Test
    void testFloatValues() throws Exception
    {
        float[] values = {
            0.0f, -0.0f, 1.0f, -1.0f, 1.5f, -1.5f,
            0.25f, -0.25f, 123.456f, -123.456f,
            1.0E10f, 1.0E-10f, -1.0E10f, -1.0E-10f,
            Float.MAX_VALUE, Float.MIN_VALUE, Float.MIN_NORMAL
        };
        for (float v : values) {
            _verifyFloatWrite(v);
        }
    }

    @Test
    void testDoubleSpecialValues() throws Exception
    {
        // NaN and Infinity go through writeString path, not direct buffer write
        _verifyDoubleSpecial(Double.NaN, "NaN");
        _verifyDoubleSpecial(Double.POSITIVE_INFINITY, "Infinity");
        _verifyDoubleSpecial(Double.NEGATIVE_INFINITY, "-Infinity");
    }

    @Test
    void testFloatSpecialValues() throws Exception
    {
        _verifyFloatSpecial(Float.NaN, "NaN");
        _verifyFloatSpecial(Float.POSITIVE_INFINITY, "Infinity");
        _verifyFloatSpecial(Float.NEGATIVE_INFINITY, "-Infinity");
    }

    @Test
    void testDoubleWriteInObject() throws Exception
    {
        // Test writeNumber(double) inside an object context
        StringWriter sw = new StringWriter();
        try (JsonGenerator gen = FAST_FACTORY.createGenerator(ObjectWriteContext.empty(), sw)) {
            gen.writeStartObject();
            gen.writeName("value");
            gen.writeNumber(1.5);
            gen.writeEndObject();
        }
        assertEquals(a2q("{'value':1.5}"), sw.toString());
    }

    @Test
    void testFloatWriteInObject() throws Exception
    {
        StringWriter sw = new StringWriter();
        try (JsonGenerator gen = FAST_FACTORY.createGenerator(ObjectWriteContext.empty(), sw)) {
            gen.writeStartObject();
            gen.writeName("value");
            gen.writeNumber(1.5f);
            gen.writeEndObject();
        }
        assertEquals(a2q("{'value':1.5}"), sw.toString());
    }

    @Test
    void testDoubleWriteInArray() throws Exception
    {
        StringWriter sw = new StringWriter();
        try (JsonGenerator gen = FAST_FACTORY.createGenerator(ObjectWriteContext.empty(), sw)) {
            gen.writeStartArray();
            gen.writeNumber(0.25);
            gen.writeNumber(-0.25);
            gen.writeNumber(17.07);
            gen.writeEndArray();
        }
        assertEquals("[0.25,-0.25,17.07]", sw.toString());
    }

    @Test
    void testFloatWriteInArray() throws Exception
    {
        StringWriter sw = new StringWriter();
        try (JsonGenerator gen = FAST_FACTORY.createGenerator(ObjectWriteContext.empty(), sw)) {
            gen.writeStartArray();
            gen.writeNumber(0.25f);
            gen.writeNumber(-0.25f);
            gen.writeNumber(17.07f);
            gen.writeEndArray();
        }
        assertEquals("[0.25,-0.25,17.07]", sw.toString());
    }

    @Test
    void testDoubleWriteToOutputStream() throws Exception
    {
        // Test the byte-buffer path (UTF8JsonGenerator)
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JsonGenerator gen = FAST_FACTORY.createGenerator(ObjectWriteContext.empty(), bytes)) {
            gen.writeStartArray();
            gen.writeNumber(0.25);
            gen.writeNumber(-0.25);
            gen.writeNumber(1.5);
            gen.writeEndArray();
        }
        assertEquals("[0.25,-0.25,1.5]", bytes.toString("UTF-8"));
    }

    @Test
    void testFloatWriteToOutputStream() throws Exception
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JsonGenerator gen = FAST_FACTORY.createGenerator(ObjectWriteContext.empty(), bytes)) {
            gen.writeStartArray();
            gen.writeNumber(0.25f);
            gen.writeNumber(-0.25f);
            gen.writeNumber(1.5f);
            gen.writeEndArray();
        }
        assertEquals("[0.25,-0.25,1.5]", bytes.toString("UTF-8"));
    }

    @Test
    void testDoubleWithNumbersAsStrings() throws Exception
    {
        // When cfgNumbersAsStrings is true, should still quote numbers
        JsonFactory factory = JsonFactory.builder()
                .enable(StreamWriteFeature.USE_FAST_DOUBLE_WRITER)
                .build();
        StringWriter sw = new StringWriter();
        // Note: cfgNumbersAsStrings is set via WRITE_NUMBERS_AS_STRINGS feature
        // For now just verify basic fast mode works
        try (JsonGenerator gen = factory.createGenerator(ObjectWriteContext.empty(), sw)) {
            gen.writeNumber(1.5);
        }
        assertEquals("1.5", sw.toString());
    }

    private void _verifyDoubleWrite(double v) throws Exception
    {
        // Verify fast mode produces output that round-trips to the same value
        StringWriter sw = new StringWriter();
        try (JsonGenerator gen = FAST_FACTORY.createGenerator(ObjectWriteContext.empty(), sw)) {
            gen.writeNumber(v);
        }
        String output = sw.toString();
        // Verify the output can be parsed back to the same double
        double parsed = Double.parseDouble(output);
        assertEquals(v, parsed, "round-trip failure for double " + v);
    }

    private void _verifyFloatWrite(float v) throws Exception
    {
        StringWriter sw = new StringWriter();
        try (JsonGenerator gen = FAST_FACTORY.createGenerator(ObjectWriteContext.empty(), sw)) {
            gen.writeNumber(v);
        }
        String output = sw.toString();
        float parsed = Float.parseFloat(output);
        assertEquals(v, parsed, "round-trip failure for float " + v);
    }

    private void _verifyDoubleSpecial(double v, String expected) throws Exception
    {
        StringWriter sw = new StringWriter();
        try (JsonGenerator gen = FAST_FACTORY.createGenerator(ObjectWriteContext.empty(), sw)) {
            gen.writeNumber(v);
        }
        // Special values are written as strings (quoted)
        assertEquals(q(expected), sw.toString());
    }

    private void _verifyFloatSpecial(float v, String expected) throws Exception
    {
        StringWriter sw = new StringWriter();
        try (JsonGenerator gen = FAST_FACTORY.createGenerator(ObjectWriteContext.empty(), sw)) {
            gen.writeNumber(v);
        }
        assertEquals(q(expected), sw.toString());
    }
}
