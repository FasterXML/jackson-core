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
    extends com.fasterxml.jackson.test.BaseTest
{
    public void testConfigDefaults() throws IOException
    {
        JsonFactory jf = new JsonFactory();
        JsonGenerator jg = jf.createGenerator(new StringWriter());
        assertFalse(jg.isEnabled(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS));
        assertFalse(jg.isEnabled(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN));
        jg.close();
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

    public void testNonNumericQuoting()
        throws IOException
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

    // [Issue#85]
    public void testBigDecimalAsPlain() throws IOException
    {
        JsonFactory jf = new JsonFactory();
        BigDecimal ENG = new BigDecimal("1E+2");

        StringWriter sw = new StringWriter();
        JsonGenerator jg = jf.createGenerator(sw);
        jg.writeNumber(ENG);
        jg.close();
        assertEquals("1E+2", sw.toString());

        jf.configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);
        sw = new StringWriter();
        jg = jf.createGenerator(sw);
        jg.writeNumber(ENG);
        jg.close();
        assertEquals("100", sw.toString());
    }

    public void testJsonpCompliantOutput() throws IOException
    {
        JsonFactory jf = new JsonFactory();
        // by default, escaping should be disabled
        _testJsonpCompliantEscaping(jf, false);
        // can enable it
        jf.enable(JsonGenerator.Feature.JSONP_COMPLIANT);
        _testJsonpCompliantEscaping(jf, true);
        // and (re)disable:
        jf.disable(JsonGenerator.Feature.JSONP_COMPLIANT);
        _testJsonpCompliantEscaping(jf, false);
    }

    /**
     * Testing for generating JSONP compliant output.
     */
    
    private String _writeNumbers(JsonFactory jf) throws IOException
    {
        StringWriter sw = new StringWriter();
        JsonGenerator jg = jf.createGenerator(sw);
    
        jg.writeStartArray();
        jg.writeNumber(1);
        jg.writeNumber(2L);
        jg.writeNumber(1.25);
        jg.writeNumber(2.25f);
        jg.writeNumber(BigInteger.valueOf(3001));
        jg.writeNumber(BigDecimal.valueOf(0.5));
        jg.writeNumber("-1");
        jg.writeEndArray();
        jg.close();

        return sw.toString();
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
        JsonGenerator jg = jf.createGenerator(sw);
        jg.writeStartObject();
        jg.writeFieldName("foo");
        jg.writeNumber(1);
        jg.writeEndObject();
        jg.close();

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
        JsonGenerator jg = jf.createGenerator(sw);
        jg.writeStartObject();
        jg.writeFieldName("double");
        jg.writeNumber(Double.NaN);
        jg.writeEndObject();
        jg.writeStartObject();
        jg.writeFieldName("float");
        jg.writeNumber(Float.NaN);
        jg.writeEndObject();
        jg.close();
	
        String result = sw.toString();
        if (quoted) {
            assertEquals("{\"double\":\"NaN\"} {\"float\":\"NaN\"}", result);
        } else {
            assertEquals("{\"double\":NaN} {\"float\":NaN}", result);
        }
    }

    private void _testJsonpCompliantEscaping(JsonFactory jf, boolean escaped)
            throws IOException
    {
        StringWriter sw = new StringWriter();
        JsonGenerator jg = jf.createGenerator(sw);
        jg.writeStartObject();
        jg.writeStringField("str", "foo\u2028bar\u2029");
        jg.writeEndObject();
        jg.close();

        String result = sw.toString();
        if (escaped) {
            assertEquals("{\"str\":\"foo\\u2028bar\\u2029\"}", result);
        } else {
            assertEquals("{\"str\":\"foo\u2028bar\u2029\"}", result);
        }
    }
}
