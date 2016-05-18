package com.fasterxml.jackson.core.json;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.core.*;

/**
 * Set of basic unit tests for verifying that the basic generator
 * functionality works as expected.
 */
public class TestJsonGeneratorFeatures
    extends com.fasterxml.jackson.core.BaseTest
{
    private final JsonFactory JSON_F = new JsonFactory();

    public void testConfigDefaults() throws IOException
    {
        JsonGenerator g = JSON_F.createGenerator(new StringWriter());
        assertFalse(g.isEnabled(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS));
        assertFalse(g.isEnabled(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN));
        g.close();
    }

    public void testFieldNameQuoting() throws IOException
    {
        JsonFactory jf = new JsonFactory();
        // by default, quoting should be enabled
        _testFieldNameQuoting(jf, true);
        // can disable it
        jf.disable(JsonGenerator.Feature.QUOTE_FIELD_NAMES);
        _testFieldNameQuoting(jf, false);
        // and (re)enable:
        jf.enable(JsonGenerator.Feature.QUOTE_FIELD_NAMES);
        _testFieldNameQuoting(jf, true);
    }

    public void testNonNumericQuoting() throws IOException
    {
        JsonFactory jf = new JsonFactory();
        // by default, quoting should be enabled
        _testNonNumericQuoting(jf, true);
        // can disable it
        jf.disable(JsonGenerator.Feature.QUOTE_NON_NUMERIC_NUMBERS);
        _testNonNumericQuoting(jf, false);
        // and (re)enable:
        jf.enable(JsonGenerator.Feature.QUOTE_NON_NUMERIC_NUMBERS);
        _testNonNumericQuoting(jf, true);
    }

    /**
     * Testing for [JACKSON-176], ability to force serializing numbers
     * as JSON Strings.
     */
    public void testNumbersAsJSONStrings() throws IOException
    {
        JsonFactory jf = new JsonFactory();
        // by default should output numbers as-is:
        assertEquals("[1,2,1.25,2.25,3001,0.5,-1]", _writeNumbers(jf));        

        // but if overridden, quotes as Strings
        jf.configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, true);
        assertEquals("[\"1\",\"2\",\"1.25\",\"2.25\",\"3001\",\"0.5\",\"-1\"]",
                     _writeNumbers(jf));
    }

    public void testBigDecimalAsPlain() throws IOException
    {
        JsonFactory jf = new JsonFactory();
        BigDecimal ENG = new BigDecimal("1E+2");

        StringWriter sw = new StringWriter();
        JsonGenerator g = jf.createGenerator(sw);
        g.writeNumber(ENG);
        g.close();
        assertEquals("1E+2", sw.toString());

        jf.configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);
        sw = new StringWriter();
        g = jf.createGenerator(sw);
        g.writeNumber(ENG);
        g.close();
        assertEquals("100", sw.toString());
    }

    public void testBigDecimalAsPlainString() throws Exception
    {
        JsonFactory jf = new JsonFactory();
        BigDecimal ENG = new BigDecimal("1E+2");
        jf.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
        jf.enable(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS);

        StringWriter sw = new StringWriter();
        JsonGenerator g = jf.createGenerator(sw);
        g.writeNumber(ENG);
        g.close();
        assertEquals(quote("100"), sw.toString());

        // also, as bytes
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        g = jf.createGenerator(bos);
        g.writeNumber(ENG);
        g.close();
        assertEquals(quote("100"), bos.toString("UTF-8"));
    }
    
    private String _writeNumbers(JsonFactory jf) throws IOException
    {
        StringWriter sw = new StringWriter();
        JsonGenerator g = jf.createGenerator(sw);
    
        g.writeStartArray();
        g.writeNumber(1);
        g.writeNumber(2L);
        g.writeNumber(1.25);
        g.writeNumber(2.25f);
        g.writeNumber(BigInteger.valueOf(3001));
        g.writeNumber(BigDecimal.valueOf(0.5));
        g.writeNumber("-1");
        g.writeEndArray();
        g.close();

        return sw.toString();
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

        JsonFactory JF2 = new JsonFactory();
        JF2.disable(JsonGenerator.Feature.QUOTE_FIELD_NAMES);

        _testFieldNameQuotingEnabled(JF2, true, true, "{\"foo\":1}");
        _testFieldNameQuotingEnabled(JF2, false, true, "{\"foo\":1}");

        // then without quotes
        _testFieldNameQuotingEnabled(JF2, true, false, "{foo:1}");
        _testFieldNameQuotingEnabled(JF2, false, false, "{foo:1}");
    }

    private void _testFieldNameQuotingEnabled(JsonFactory jf, boolean useBytes,
            boolean useQuotes, String exp) throws IOException
    {
        ByteArrayOutputStream bytes = useBytes ? new ByteArrayOutputStream() : null;
        StringWriter sw = useBytes ? null : new StringWriter();
        JsonGenerator gen = useBytes ? jf.createGenerator(bytes) : jf.createGenerator(sw);
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

        String json = useBytes ? bytes.toString("UTF-8") : sw.toString();
        assertEquals(exp, json);
    }
    
    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private void _testFieldNameQuoting(JsonFactory jf, boolean quoted)
        throws IOException
    {
        StringWriter sw = new StringWriter();
        JsonGenerator g = jf.createGenerator(sw);
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
    private void _testNonNumericQuoting(JsonFactory jf, boolean quoted)
        throws IOException
    {
        StringWriter sw = new StringWriter();
        JsonGenerator g = jf.createGenerator(sw);
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
}
