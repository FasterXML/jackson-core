package tools.jackson.core.json;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamWriteException;

/**
 * Set of basic unit tests for verifying that the basic generator
 * functionality works as expected.
 */
public class StreamWriteFeaturesTest
    extends tools.jackson.core.BaseTest
{
    private final JsonFactory JSON_F = new JsonFactory();

    public void testConfigDefaults()
    {
        JsonGenerator g = JSON_F.createGenerator(ObjectWriteContext.empty(), new StringWriter());
        assertFalse(((JsonGeneratorBase) g).isEnabled(JsonWriteFeature.WRITE_NUMBERS_AS_STRINGS));
        assertFalse(g.isEnabled(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN));

        assertTrue(g.canOmitProperties());
        assertFalse(g.canWriteObjectId());
        assertFalse(g.canWriteTypeId());

        g.close();
    }

    public void testFieldNameQuoting()
    {
        JsonFactory f = new JsonFactory();
        // by default, quoting should be enabled
        _testFieldNameQuoting(f, true);
        // can disable it
        f = f.rebuild().disable(JsonWriteFeature.QUOTE_PROPERTY_NAMES)
                .build();
        _testFieldNameQuoting(f, false);
        // and (re)enable:
        f = f.rebuild()
                .enable(JsonWriteFeature.QUOTE_PROPERTY_NAMES)
                .build();
        _testFieldNameQuoting(f, true);
    }

    public void testNonNumericQuoting()
    {
        JsonFactory f = new JsonFactory();
        // by default, quoting should be enabled
        assertTrue(f.isEnabled(JsonWriteFeature.WRITE_NAN_AS_STRINGS));
        _testNonNumericQuoting(f, true);
        // can disable it
        f = f.rebuild().disable(JsonWriteFeature.WRITE_NAN_AS_STRINGS)
                .build();
        _testNonNumericQuoting(f, false);
        // and (re)enable:
        f = f.rebuild()
                .enable(JsonWriteFeature.WRITE_NAN_AS_STRINGS)
                .build();
        _testNonNumericQuoting(f, true);
    }

    /**
     * Testing for [JACKSON-176], ability to force serializing numbers
     * as JSON Strings.
     */
    public void testNumbersAsJSONStrings()
    {
        JsonFactory f = new JsonFactory();
        // by default should output numbers as-is:
        assertEquals("[1,2,3,1.25,2.25,3001,0.5,-1,12.3,null,null,null]", _writeNumbers(f, false));
        assertEquals("[1,2,3,1.25,2.25,3001,0.5,-1,12.3,null,null,null]", _writeNumbers(f, true));

        // but if overridden, quotes as Strings
        f = f.rebuild().configure(JsonWriteFeature.WRITE_NUMBERS_AS_STRINGS, true)
                .build();
        assertEquals("[\"1\",\"2\",\"3\",\"1.25\",\"2.25\",\"3001\",\"0.5\",\"-1\",\"12.3\",null,null,null]",
                     _writeNumbers(f, false));
        assertEquals("[\"1\",\"2\",\"3\",\"1.25\",\"2.25\",\"3001\",\"0.5\",\"-1\",\"12.3\",null,null,null]",
                _writeNumbers(f, true));
    }

    public void testBigDecimalAsPlain()
    {
        JsonFactory f = new JsonFactory();
        BigDecimal ENG = new BigDecimal("1E+2");

        StringWriter sw = new StringWriter();
        JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), sw);
        g.writeNumber(ENG);
        g.close();
        assertEquals("1E+2", sw.toString());

        f = f.rebuild().enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN)
                .build();
        sw = new StringWriter();
        g = f.createGenerator(ObjectWriteContext.empty(), sw);
        g.writeNumber(ENG);
        g.close();
        assertEquals("100", sw.toString());
    }

    public void testBigDecimalAsPlainString()
    {
        JsonFactory f = new JsonFactory();
        BigDecimal ENG = new BigDecimal("1E+2");
        f = f.rebuild().enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN)
                .enable(JsonWriteFeature.WRITE_NUMBERS_AS_STRINGS)
                .build();
        StringWriter sw = new StringWriter();
        JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), sw);
        g.writeNumber(ENG);
        g.close();
        assertEquals(q("100"), sw.toString());

        // also, as bytes
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        g = f.createGenerator(ObjectWriteContext.empty(), bos);
        g.writeNumber(ENG);
        g.close();
        assertEquals(q("100"), utf8String(bos));
    }

    // [core#315]
    public void testTooBigBigDecimal()
    {
        // 24-Aug-2016, tatu: Initial check limits scale to [-9999,+9999]
        BigDecimal BIG = new BigDecimal("1E+9999");
        BigDecimal TOO_BIG = new BigDecimal("1E+10000");
        BigDecimal SMALL = new BigDecimal("1E-9999");
        BigDecimal TOO_SMALL = new BigDecimal("1E-10000");

        for (boolean asString : new boolean[] { false, true } ) {
            for (boolean useBytes : new boolean[] { false, true } ) {
                JsonFactory f = JsonFactory.builder()
                        .enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN)
                        .configure(JsonWriteFeature.WRITE_NUMBERS_AS_STRINGS, asString)
                        .build();
                JsonGenerator g;
                if (useBytes) {
                    g = f.createGenerator(ObjectWriteContext.empty(), new ByteArrayOutputStream());
                } else {
                    g = f.createGenerator(ObjectWriteContext.empty(), new StringWriter());
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
                        g = f.createGenerator(ObjectWriteContext.empty(), new ByteArrayOutputStream());
                    } else {
                        g = f.createGenerator(ObjectWriteContext.empty(), new StringWriter());
                    }
                    try {
                        g.writeNumber(input);
                        fail("Should not have written without exception: "+input);
                    } catch (StreamWriteException e) {
                        verifyException(e, "Attempt to write plain `java.math.BigDecimal`");
                        verifyException(e, "illegal scale");
                    }
                    g.close();
                }
            }
        }
    }

    private String _writeNumbers(JsonFactory f, boolean useBytes)
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        StringWriter sw = new StringWriter();
        JsonGenerator g;
        if (useBytes) {
            g = f.createGenerator(ObjectWriteContext.empty(), bytes);
        } else {
            g = f.createGenerator(ObjectWriteContext.empty(), sw);
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
    public void testFieldNameQuotingEnabled()
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
                .disable(JsonWriteFeature.QUOTE_PROPERTY_NAMES)
                .build();

        _testFieldNameQuotingEnabled(f2, true, true, "{\"foo\":1}");
        _testFieldNameQuotingEnabled(f2, false, true, "{\"foo\":1}");

        // then without quotes
        _testFieldNameQuotingEnabled(f2, true, false, "{foo:1}");
        _testFieldNameQuotingEnabled(f2, false, false, "{foo:1}");
    }

    private void _testFieldNameQuotingEnabled(JsonFactory f, boolean useBytes,
            boolean useQuotes, String exp)
    {
        if (useQuotes) {
            f = f.rebuild()
                    .enable(JsonWriteFeature.QUOTE_PROPERTY_NAMES)
                    .build();
        } else {
            f = f.rebuild()
                    .disable(JsonWriteFeature.QUOTE_PROPERTY_NAMES)
                    .build();
        }

        ByteArrayOutputStream bytes = useBytes ? new ByteArrayOutputStream() : null;
        StringWriter sw = useBytes ? null : new StringWriter();
        JsonGenerator gen = useBytes ? f.createGenerator(ObjectWriteContext.empty(),bytes)
                : f.createGenerator(ObjectWriteContext.empty(), sw);

        gen.writeStartObject();
        gen.writeName("foo");
        gen.writeNumber(1);
        gen.writeEndObject();
        gen.close();

        String json = useBytes ? utf8String(bytes) : sw.toString();
        assertEquals(exp, json);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private void _testFieldNameQuoting(JsonFactory f, boolean quoted)
    {
        StringWriter sw = new StringWriter();
        JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), sw);
        g.writeStartObject();
        g.writeName("foo");
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
    {
        StringWriter sw = new StringWriter();
        JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), sw);
        g.writeStartObject();
        g.writeName("double");
        g.writeNumber(Double.NaN);
        g.writeEndObject();
        g.writeStartObject();
        g.writeName("float");
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
            g = f.createGenerator(ObjectWriteContext.empty(), bytes);
        } else {
            g = f.createGenerator(ObjectWriteContext.empty(), sw);
        }

        g.writeString(input);
        g.flush();
        g.close();

        String result = useBytes ? utf8String(bytes) : sw.toString();
        assertEquals(exp, result);

        g.close();
    }
}
