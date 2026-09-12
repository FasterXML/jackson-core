package tools.jackson.core.unittest.write;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.SerializableString;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.core.io.CharacterEscapes;
import tools.jackson.core.io.NumberOutput;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonWriteFeature;
import tools.jackson.core.unittest.JacksonCoreTestBase;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the quoted (`WRITE_NUMBERS_AS_STRINGS` / `WRITE_NAN_AS_STRINGS`)
 * float/double output paths when {@link StreamWriteFeature#USE_FAST_DOUBLE_WRITER}
 * is enabled, for both byte- and char-backed generators.
 */
public class FastDoubleQuotedWriteTest extends JacksonCoreTestBase
{
    private final JsonFactory NUMBERS_AS_STRINGS = JsonFactory.builder()
            .enable(StreamWriteFeature.USE_FAST_DOUBLE_WRITER)
            .enable(JsonWriteFeature.WRITE_NUMBERS_AS_STRINGS)
            .build();

    private final JsonFactory NAN_AS_STRINGS = JsonFactory.builder()
            .enable(StreamWriteFeature.USE_FAST_DOUBLE_WRITER)
            .enable(JsonWriteFeature.WRITE_NAN_AS_STRINGS)
            .build();

    private final double[] DOUBLES = {
        0.0, -0.0, 1.5, -1.5, 123.456789, 1.0E10, 1.0E-10,
        Double.MAX_VALUE, Double.MIN_VALUE, Double.MIN_NORMAL,
        Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY
    };

    private final float[] FLOATS = {
        0.0f, -0.0f, 1.5f, -1.5f, 123.456f, 1.0E10f, 1.0E-10f,
        Float.MAX_VALUE, Float.MIN_VALUE, Float.MIN_NORMAL,
        Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY
    };

    @Test
    void testNumbersAsStringsDouble() throws Exception
    {
        for (double v : DOUBLES) {
            String exp = q(NumberOutput.toString(v, true));
            assertEquals(exp, _writeChars(NUMBERS_AS_STRINGS, v));
            assertEquals(exp, _writeBytes(NUMBERS_AS_STRINGS, v));
        }
    }

    @Test
    void testNumbersAsStringsFloat() throws Exception
    {
        for (float v : FLOATS) {
            String exp = q(NumberOutput.toString(v, true));
            assertEquals(exp, _writeChars(NUMBERS_AS_STRINGS, v));
            assertEquals(exp, _writeBytes(NUMBERS_AS_STRINGS, v));
        }
    }

    @Test
    void testNaNAsStrings() throws Exception
    {
        // Only non-finite values get quoted
        assertEquals(q("NaN"), _writeChars(NAN_AS_STRINGS, Double.NaN));
        assertEquals(q("NaN"), _writeBytes(NAN_AS_STRINGS, Double.NaN));
        assertEquals(q("-Infinity"), _writeChars(NAN_AS_STRINGS, Double.NEGATIVE_INFINITY));
        assertEquals(q("Infinity"), _writeBytes(NAN_AS_STRINGS, Double.POSITIVE_INFINITY));
        assertEquals("1.5", _writeChars(NAN_AS_STRINGS, 1.5));
        assertEquals("1.5", _writeBytes(NAN_AS_STRINGS, 1.5));

        assertEquals(q("NaN"), _writeChars(NAN_AS_STRINGS, Float.NaN));
        assertEquals(q("NaN"), _writeBytes(NAN_AS_STRINGS, Float.NaN));
        assertEquals(q("-Infinity"), _writeChars(NAN_AS_STRINGS, Float.NEGATIVE_INFINITY));
        assertEquals(q("Infinity"), _writeBytes(NAN_AS_STRINGS, Float.POSITIVE_INFINITY));
        assertEquals("1.5", _writeChars(NAN_AS_STRINGS, 1.5f));
        assertEquals("1.5", _writeBytes(NAN_AS_STRINGS, 1.5f));
    }

    @Test
    void testQuotedAcrossBufferBoundary() throws Exception
    {
        final double[] values = { -Double.MIN_NORMAL, 1.0, 0.1, Double.NaN, 1.0E-300 };
        final float[] fvalues = { -Float.MIN_NORMAL, 1.0f, 0.1f, Float.NaN, 1.0E-30f };
        final int count = 4000;
        StringBuilder expected = new StringBuilder("[");
        for (int i = 0; i < count; ++i) {
            if (i > 0) {
                expected.append(',');
            }
            expected.append(q(NumberOutput.toString(values[i % values.length], true)));
            expected.append(',');
            expected.append(q(NumberOutput.toString(fvalues[i % fvalues.length], true)));
        }
        expected.append(']');

        StringWriter sw = new StringWriter();
        try (JsonGenerator gen = NUMBERS_AS_STRINGS.createGenerator(ObjectWriteContext.empty(), sw)) {
            _writeArray(gen, values, fvalues, count);
        }
        assertEquals(expected.toString(), sw.toString());

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JsonGenerator gen = NUMBERS_AS_STRINGS.createGenerator(ObjectWriteContext.empty(), bytes)) {
            _writeArray(gen, values, fvalues, count);
        }
        assertEquals(expected.toString(), bytes.toString(StandardCharsets.UTF_8));
    }

    @Test
    void testQuotedFloatsOnlyAcrossBufferBoundary() throws Exception
    {
        // Float-only run: the float flush check has a smaller reserve than the
        // double one, so mixing types never lets it trigger
        final float[] fvalues = { -Float.MIN_NORMAL, 1.0f, 0.1f, Float.NaN, 1.0E-30f };
        final int count = 6000;
        StringBuilder expected = new StringBuilder("[");
        for (int i = 0; i < count; ++i) {
            if (i > 0) {
                expected.append(',');
            }
            expected.append(q(NumberOutput.toString(fvalues[i % fvalues.length], true)));
        }
        expected.append(']');

        StringWriter sw = new StringWriter();
        try (JsonGenerator gen = NUMBERS_AS_STRINGS.createGenerator(ObjectWriteContext.empty(), sw)) {
            gen.writeStartArray();
            for (int i = 0; i < count; ++i) {
                gen.writeNumber(fvalues[i % fvalues.length]);
            }
            gen.writeEndArray();
        }
        assertEquals(expected.toString(), sw.toString());

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JsonGenerator gen = NUMBERS_AS_STRINGS.createGenerator(ObjectWriteContext.empty(), bytes)) {
            gen.writeStartArray();
            for (int i = 0; i < count; ++i) {
                gen.writeNumber(fvalues[i % fvalues.length]);
            }
            gen.writeEndArray();
        }
        assertEquals(expected.toString(), bytes.toString(StandardCharsets.UTF_8));
    }

    @Test
    void testNaNAsStringsDisabled() throws Exception
    {
        // With WRITE_NAN_AS_STRINGS off, non-finite values go through the
        // unquoted fast path (which must still render them correctly)
        JsonFactory f = JsonFactory.builder()
                .enable(StreamWriteFeature.USE_FAST_DOUBLE_WRITER)
                .disable(JsonWriteFeature.WRITE_NAN_AS_STRINGS)
                .build();
        assertEquals("NaN", _writeChars(f, Double.NaN));
        assertEquals("NaN", _writeBytes(f, Double.NaN));
        assertEquals("Infinity", _writeChars(f, Double.POSITIVE_INFINITY));
        assertEquals("-Infinity", _writeBytes(f, Double.NEGATIVE_INFINITY));

        assertEquals("NaN", _writeChars(f, Float.NaN));
        assertEquals("NaN", _writeBytes(f, Float.NaN));
        assertEquals("-Infinity", _writeChars(f, Float.NEGATIVE_INFINITY));
        assertEquals("Infinity", _writeBytes(f, Float.POSITIVE_INFINITY));
    }

    @Test
    void testQuotedWithCustomQuoteChar() throws Exception
    {
        JsonFactory f = JsonFactory.builder()
                .enable(StreamWriteFeature.USE_FAST_DOUBLE_WRITER)
                .enable(JsonWriteFeature.WRITE_NUMBERS_AS_STRINGS)
                .quoteChar('\'')
                .build();
        assertEquals("'1.5'", _writeChars(f, 1.5));
        assertEquals("'1.5'", _writeBytes(f, 1.5));
        assertEquals("'2.5'", _writeChars(f, 2.5f));
        assertEquals("'2.5'", _writeBytes(f, 2.5f));
        assertEquals("'NaN'", _writeChars(f, Double.NaN));
        assertEquals("'NaN'", _writeBytes(f, Float.NaN));
    }

    @Test
    void testQuotedRootValues() throws Exception
    {
        // Root-level values get the root value separator via _verifyValueWrite()
        StringWriter sw = new StringWriter();
        try (JsonGenerator gen = NUMBERS_AS_STRINGS.createGenerator(ObjectWriteContext.empty(), sw)) {
            gen.writeNumber(1.5);
            gen.writeNumber(2.5f);
        }
        assertEquals(a2q("'1.5' '2.5'"), sw.toString());

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JsonGenerator gen = NUMBERS_AS_STRINGS.createGenerator(ObjectWriteContext.empty(), bytes)) {
            gen.writeNumber(1.5);
            gen.writeNumber(2.5f);
        }
        assertEquals(a2q("'1.5' '2.5'"), bytes.toString(StandardCharsets.UTF_8));
    }

    @Test
    void testQuotedInObject() throws Exception
    {
        StringWriter sw = new StringWriter();
        try (JsonGenerator gen = NUMBERS_AS_STRINGS.createGenerator(ObjectWriteContext.empty(), sw)) {
            gen.writeStartObject();
            gen.writeName("d");
            gen.writeNumber(1.5);
            gen.writeName("f");
            gen.writeNumber(2.5f);
            gen.writeEndObject();
        }
        assertEquals(a2q("{'d':'1.5','f':'2.5'}"), sw.toString());
    }

    // Escapes every digit; number text must never be escaped, regardless of
    // whether USE_FAST_DOUBLE_WRITER is enabled
    @SuppressWarnings("serial")
    static class DigitEscapes extends CharacterEscapes
    {
        private final int[] _asciiEscapes;

        public DigitEscapes() {
            _asciiEscapes = standardAsciiEscapesForJSON();
            for (int c = '0'; c <= '9'; ++c) {
                _asciiEscapes[c] = CharacterEscapes.ESCAPE_STANDARD;
            }
        }

        @Override
        public int[] getEscapeCodesForAscii() { return _asciiEscapes; }

        @Override
        public SerializableString getEscapeSequence(int ch) { return null; }
    }

    @Test
    void testQuotedNotEscapedRegardlessOfFastWriter() throws Exception
    {
        for (boolean useFast : new boolean[] { true, false }) {
            JsonFactory f = JsonFactory.builder()
                    .configure(StreamWriteFeature.USE_FAST_DOUBLE_WRITER, useFast)
                    .enable(JsonWriteFeature.WRITE_NUMBERS_AS_STRINGS)
                    .characterEscapes(new DigitEscapes())
                    .build();
            String desc = "useFast="+useFast;
            assertEquals(q("1.5"), _writeChars(f, 1.5), desc);
            assertEquals(q("1.5"), _writeBytes(f, 1.5), desc);
            assertEquals(q("2.5"), _writeChars(f, 2.5f), desc);
            assertEquals(q("2.5"), _writeBytes(f, 2.5f), desc);
            // ... just like the pre-existing quoted-number paths
            assertEquals(q("1.5"), _writeChars(f, new BigDecimal("1.5")), desc);
            assertEquals(q("1.5"), _writeBytes(f, new BigDecimal("1.5")), desc);
        }
    }

    private void _writeArray(JsonGenerator gen, double[] values, float[] fvalues, int count)
    {
        gen.writeStartArray();
        for (int i = 0; i < count; ++i) {
            gen.writeNumber(values[i % values.length]);
            gen.writeNumber(fvalues[i % fvalues.length]);
        }
        gen.writeEndArray();
    }

    private String _writeChars(JsonFactory f, double v) throws Exception {
        StringWriter sw = new StringWriter();
        try (JsonGenerator gen = f.createGenerator(ObjectWriteContext.empty(), sw)) {
            gen.writeNumber(v);
        }
        return sw.toString();
    }

    private String _writeChars(JsonFactory f, float v) throws Exception {
        StringWriter sw = new StringWriter();
        try (JsonGenerator gen = f.createGenerator(ObjectWriteContext.empty(), sw)) {
            gen.writeNumber(v);
        }
        return sw.toString();
    }

    private String _writeBytes(JsonFactory f, double v) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JsonGenerator gen = f.createGenerator(ObjectWriteContext.empty(), bytes)) {
            gen.writeNumber(v);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    private String _writeBytes(JsonFactory f, float v) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JsonGenerator gen = f.createGenerator(ObjectWriteContext.empty(), bytes)) {
            gen.writeNumber(v);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    private String _writeChars(JsonFactory f, BigDecimal v) throws Exception {
        StringWriter sw = new StringWriter();
        try (JsonGenerator gen = f.createGenerator(ObjectWriteContext.empty(), sw)) {
            gen.writeNumber(v);
        }
        return sw.toString();
    }

    private String _writeBytes(JsonFactory f, BigDecimal v) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JsonGenerator gen = f.createGenerator(ObjectWriteContext.empty(), bytes)) {
            gen.writeNumber(v);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }
}
