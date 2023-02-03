package com.fasterxml.jackson.core.json;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.core.*;

/**
 * Set of basic unit tests for verifying that the basic generator
 * functionality works as expected.
 */
public class GeneratorFeaturesTest
    extends com.fasterxml.jackson.core.BaseTest
{
    private final JsonFactory JSON_F = new JsonFactory();

    @SuppressWarnings("deprecation")
    public void testConfigDefaults() throws IOException
    {
        JsonGenerator g = JSON_F.createGenerator(new StringWriter());
        assertFalse(g.isEnabled(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS));

        assertFalse(g.isEnabled(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN));
        assertFalse(g.isEnabled(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN));

        assertTrue(g.canOmitFields());
        assertFalse(g.canWriteBinaryNatively());
        assertTrue(g.canWriteFormattedNumbers());
        assertFalse(g.canWriteObjectId());
        assertFalse(g.canWriteTypeId());

        g.close();
    }

    @SuppressWarnings("deprecation")
    public void testConfigOverrides() throws IOException
    {
        // but also allow overide
        JsonGenerator g = JSON_F.createGenerator(new StringWriter());
        int mask = JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS.getMask()
                | JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN.getMask();
        g.overrideStdFeatures(mask, mask);
        assertTrue(g.isEnabled(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS));
        assertTrue(g.isEnabled(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN));
        assertTrue(g.isEnabled(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN));

        // and for now, also test straight override
        g.setFeatureMask(0);
        assertFalse(g.isEnabled(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS));
        assertFalse(g.isEnabled(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN));
        assertFalse(g.isEnabled(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN));
        g.close();
    }

    public void testFieldNameQuoting() throws IOException
    {
        JsonFactory f = new JsonFactory();
        // by default, quoting should be enabled
        _testFieldNameQuoting(f, true);
        // can disable it
        f = JsonFactory.builder()
                .disable(JsonWriteFeature.QUOTE_FIELD_NAMES)
                .build();
        _testFieldNameQuoting(f, false);
        // and (re)enable:
        f = JsonFactory.builder()
                .enable(JsonWriteFeature.QUOTE_FIELD_NAMES)
                .build();
        _testFieldNameQuoting(f, true);
    }

    public void testNonNumericQuoting() throws IOException
    {
        JsonFactory f = new JsonFactory();
        // by default, quoting should be enabled
        _testNonNumericQuoting(f, true);
        // can disable it
        f = JsonFactory.builder()
                .disable(JsonWriteFeature.WRITE_NAN_AS_STRINGS)
                .build();
        _testNonNumericQuoting(f, false);
        // and (re)enable:
        f = JsonFactory.builder()
                .enable(JsonWriteFeature.WRITE_NAN_AS_STRINGS)
                .build();
        _testNonNumericQuoting(f, true);
    }

    /**
     * Testing for [JACKSON-176], ability to force serializing numbers
     * as JSON Strings.
     */
    public void testNumbersAsJSONStrings() throws IOException
    {
        JsonFactory f = new JsonFactory();
        // by default should output numbers as-is:
        assertEquals("[1,2,3,1.25,2.25,3001,0.5,-1,12.3,null,null,null]", _writeNumbers(f, false));
        assertEquals("[1,2,3,1.25,2.25,3001,0.5,-1,12.3,null,null,null]", _writeNumbers(f, true));

        // but if overridden, quotes as Strings
        f = JsonFactory.builder()
                .enable(JsonWriteFeature.WRITE_NUMBERS_AS_STRINGS)
                .build();
        assertEquals("[\"1\",\"2\",\"3\",\"1.25\",\"2.25\",\"3001\",\"0.5\",\"-1\",\"12.3\",null,null,null]",
                     _writeNumbers(f, false));
        assertEquals("[\"1\",\"2\",\"3\",\"1.25\",\"2.25\",\"3001\",\"0.5\",\"-1\",\"12.3\",null,null,null]",
                _writeNumbers(f, true));


    }

    public void testBigDecimalAsPlain() throws IOException
    {
        JsonFactory f = new JsonFactory();
        BigDecimal ENG = new BigDecimal("1E+2");

        StringWriter sw = new StringWriter();
        JsonGenerator g = f.createGenerator(sw);
        g.writeNumber(ENG);
        g.close();
        assertEquals("1E+2", sw.toString());

        f.configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);
        sw = new StringWriter();
        g = f.createGenerator(sw);
        g.writeNumber(ENG);
        g.close();
        assertEquals("100", sw.toString());
    }

    public void testBigDecimalAsPlainString() throws Exception
    {
        BigDecimal ENG = new BigDecimal("1E+2");
        JsonFactory f = JsonFactory.builder()
                .enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN)
                .enable(JsonWriteFeature.WRITE_NUMBERS_AS_STRINGS)
                .build();

        StringWriter sw = new StringWriter();
        JsonGenerator g = f.createGenerator(sw);
        g.writeNumber(ENG);
        g.close();
        assertEquals(q("100"), sw.toString());

        // also, as bytes
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        g = f.createGenerator(bos);
        g.writeNumber(ENG);
        g.close();
        assertEquals(q("100"), utf8String(bos));
    }

    // [core#315]
    @SuppressWarnings("deprecation")
    public void testTooBigBigDecimal() throws Exception
    {
        JsonFactory f = new JsonFactory();
        f.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);

        // 24-Aug-2016, tatu: Initial check limits scale to [-9999,+9999]
        BigDecimal BIG = new BigDecimal("1E+9999");
        BigDecimal TOO_BIG = new BigDecimal("1E+10000");
        BigDecimal SMALL = new BigDecimal("1E-9999");
        BigDecimal TOO_SMALL = new BigDecimal("1E-10000");

        for (boolean useBytes : new boolean[] { false, true } ) {
            for (boolean asString : new boolean[] { false, true } ) {
                JsonGenerator g;

                if (useBytes) {
                    g = f.createGenerator(new ByteArrayOutputStream());
                } else {
                    g = f.createGenerator(new StringWriter());
                }
                if (asString) {
                    g.enable(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS);
                }

                // first, ok cases:
                g.writeStartArray();
                g.writeNumber(BIG);
                g.writeNumber(SMALL);
                g.writeEndArray();
                g.close();

                // then invalid
                for (BigDecimal input : new BigDecimal[] { TOO_BIG, TOO_SMALL }) {
                    if (useBytes) {
                        g = f.createGenerator(new ByteArrayOutputStream());
                    } else {
                        g = f.createGenerator(new StringWriter());
                    }
                    if (asString) {
                        g.enable(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS);
                    }
                    try {
                        g.writeNumber(input);
                        fail("Should not have written without exception: "+input);
                    } catch (JsonGenerationException e) {
                        verifyException(e, "Attempt to write plain `java.math.BigDecimal`");
                        verifyException(e, "illegal scale");
                    }
                    g.close();
                }
            }
        }
    }

    private String _writeNumbers(JsonFactory f, boolean useBytes) throws IOException
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        StringWriter sw = new StringWriter();
        JsonGenerator g;
        if (useBytes) {
            g = f.createGenerator(bytes);
        } else {
            g = f.createGenerator(sw);
        }

        g.writeStartArray();
        g.writeNumber(1);
        g.writeNumber(2L);
        g.writeNumber((short) 3);
        g.writeNumber(1.25);
        g.writeNumber(2.25f);
        g.writeNumber(BigInteger.valueOf(3001));
        g.writeNumber(BigDecimal.valueOf(0.5));
        g.writeNumber("-1");
        g.writeNumber(new char[]{'1', '2', '.', '3', '-'}, 0, 4);
        g.writeNumber((String) null);
        g.writeNumber((BigDecimal) null);
        g.writeNumber((BigInteger) null);
        g.writeEndArray();
        g.close();

        return useBytes ? utf8String(bytes) : sw.toString();
    }

    // for [core#246]
    public void testFieldNameQuotingEnabled() throws IOException
    {
        // // First, test with default factory, with quoting enabled by default

        // First, default, with quotes
        _testFieldNameQuotingEnabled(JSON_F, true, true, "{\"foo\":1}");
        _testFieldNameQuotingEnabled(JSON_F, false, true, "{\"foo\":1}");

        // then without quotes
        _testFieldNameQuotingEnabled(JSON_F, true, false, "{foo:1}");
        _testFieldNameQuotingEnabled(JSON_F, false, false, "{foo:1}");

        // // Then with alternatively configured factory

        JsonFactory f2 = JsonFactory.builder()
                .disable(JsonWriteFeature.QUOTE_FIELD_NAMES)
                .build();

        _testFieldNameQuotingEnabled(f2, true, true, "{\"foo\":1}");
        _testFieldNameQuotingEnabled(f2, false, true, "{\"foo\":1}");

        // then without quotes
        _testFieldNameQuotingEnabled(f2, true, false, "{foo:1}");
        _testFieldNameQuotingEnabled(f2, false, false, "{foo:1}");
    }

    @SuppressWarnings("deprecation")
    private void _testFieldNameQuotingEnabled(JsonFactory f, boolean useBytes,
            boolean useQuotes, String exp) throws IOException
    {
        ByteArrayOutputStream bytes = useBytes ? new ByteArrayOutputStream() : null;
        StringWriter sw = useBytes ? null : new StringWriter();
        JsonGenerator gen = useBytes ? f.createGenerator(bytes) : f.createGenerator(sw);
        if (useQuotes) {
            gen.enable(JsonGenerator.Feature.QUOTE_FIELD_NAMES);
        } else {
            gen.disable(JsonGenerator.Feature.QUOTE_FIELD_NAMES);
        }

        gen.writeStartObject();
        gen.writeFieldName("foo");
        gen.writeNumber(1);
        gen.writeEndObject();
        gen.close();

        String json = useBytes ? utf8String(bytes) : sw.toString();
        assertEquals(exp, json);
    }

    @SuppressWarnings("deprecation")
    public void testChangeOnGenerator() throws IOException
    {
        StringWriter w = new StringWriter();

        JsonGenerator g = JSON_F.createGenerator(w);
        g.enable(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS);
        g.writeNumber(123);
        g.close();
        assertEquals(quote("123"), w.toString());

        // but also the opposite
        w = new StringWriter();
        g = JSON_F.createGenerator(w);
        g.enable(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS);
        g.disable(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS);
        g.writeNumber(123);
        g.close();
        assertEquals("123", w.toString());
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private void _testFieldNameQuoting(JsonFactory f, boolean quoted)
        throws IOException
    {
        StringWriter sw = new StringWriter();
        JsonGenerator g = f.createGenerator(sw);
        g.writeStartObject();
        g.writeFieldName("foo");
        g.writeNumber(1);
        g.writeEndObject();
        g.close();

        String result = sw.toString();
        if (quoted) {
            assertEquals("{\"foo\":1}", result);
        } else {
            assertEquals("{foo:1}", result);
        }
    }
    private void _testNonNumericQuoting(JsonFactory f, boolean quoted)
        throws IOException
    {
        StringWriter sw = new StringWriter();
        JsonGenerator g = f.createGenerator(sw);
        g.writeStartObject();
        g.writeFieldName("double");
        g.writeNumber(Double.NaN);
        g.writeEndObject();
        g.writeStartObject();
        g.writeFieldName("float");
        g.writeNumber(Float.NaN);
        g.writeEndObject();
        g.close();

        String result = sw.toString();
        if (quoted) {
            assertEquals("{\"double\":\"NaN\"} {\"float\":\"NaN\"}", result);
        } else {
            assertEquals("{\"double\":NaN} {\"float\":NaN}", result);
        }
    }

    // [core#717]: configurable hex digits; lower-case
    public void testHexLowercase() throws Exception {
        JsonFactory f = JsonFactory.builder()
                .disable(JsonWriteFeature.WRITE_HEX_UPPER_CASE)
                .build();
        _testHexOutput(f, false, "\u001b", q("\\u001b"));
        _testHexOutput(f, true, "\u001b", q("\\u001b"));
    }

    // [core#717]: configurable hex digits; upper-case (default)
    public void testHexUppercase() throws Exception
    {
        JsonFactory f = JsonFactory.builder()
                .enable(JsonWriteFeature.WRITE_HEX_UPPER_CASE)
                .build();
        _testHexOutput(f, false, "\u001b", q("\\u001B"));
        _testHexOutput(f, true, "\u001b", q("\\u001B"));
    }

    private void _testHexOutput(JsonFactory f, boolean useBytes,
            String input, String exp) throws Exception
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        StringWriter sw = new StringWriter();
        JsonGenerator g;
        if (useBytes) {
            g = f.createGenerator(bytes);
        } else {
            g = f.createGenerator(sw);
        }

        g.writeString(input);
        g.flush();
        g.close();

        String result = useBytes ? utf8String(bytes) : sw.toString();
        assertEquals(exp, result);

        g.close();
    }
}
