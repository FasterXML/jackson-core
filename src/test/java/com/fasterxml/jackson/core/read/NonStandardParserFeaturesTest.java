package com.fasterxml.jackson.core.read;

import com.fasterxml.jackson.core.*;

public class NonStandardParserFeaturesTest
    extends com.fasterxml.jackson.core.BaseTest
{
    public void testSimpleUnquotedBytes() throws Exception {
        _testSimpleUnquoted(MODE_INPUT_STREAM);
        _testSimpleUnquoted(MODE_INPUT_STREAM_THROTTLED);
        _testSimpleUnquoted(MODE_DATA_INPUT);
    }

    public void testSimpleUnquotedChars() throws Exception {
        _testSimpleUnquoted(MODE_READER);
    }
    
    public void testLargeUnquoted() throws Exception
    {
        _testLargeUnquoted(MODE_INPUT_STREAM);
        _testLargeUnquoted(MODE_INPUT_STREAM_THROTTLED);
        _testLargeUnquoted(MODE_DATA_INPUT);
        _testLargeUnquoted(MODE_READER);
    }

    public void testSingleQuotesDefault() throws Exception
    {
        _testSingleQuotesDefault(MODE_INPUT_STREAM);
        _testSingleQuotesDefault(MODE_INPUT_STREAM_THROTTLED);
        _testSingleQuotesDefault(MODE_DATA_INPUT);
        _testSingleQuotesDefault(MODE_READER);
    }

    public void testSingleQuotesEnabled() throws Exception
    {
        _testSingleQuotesEnabled(MODE_INPUT_STREAM);
        _testSingleQuotesEnabled(MODE_INPUT_STREAM_THROTTLED);
        _testSingleQuotesEnabled(MODE_DATA_INPUT);
        _testSingleQuotesEnabled(MODE_READER);

        _testSingleQuotesEscaped(MODE_INPUT_STREAM);
        _testSingleQuotesEscaped(MODE_INPUT_STREAM_THROTTLED);
        _testSingleQuotesEscaped(MODE_DATA_INPUT);
        _testSingleQuotesEscaped(MODE_READER);
    }

    public void testNonStandardNameChars() throws Exception
    {
        _testNonStandardNameChars(MODE_INPUT_STREAM);
        _testNonStandardNameChars(MODE_INPUT_STREAM_THROTTLED);
        _testNonStandardNameChars(MODE_DATA_INPUT);
        _testNonStandardNameChars(MODE_READER);
    }

    public void testNonStandardAnyCharQuoting() throws Exception
    {
        _testNonStandarBackslashQuoting(MODE_INPUT_STREAM);
        _testNonStandarBackslashQuoting(MODE_INPUT_STREAM_THROTTLED);
        _testNonStandarBackslashQuoting(MODE_DATA_INPUT);
        _testNonStandarBackslashQuoting(MODE_READER);
    }

    public void testLeadingZeroesUTF8() throws Exception {
        _testLeadingZeroes(MODE_INPUT_STREAM, false);
        _testLeadingZeroes(MODE_INPUT_STREAM, true);
        _testLeadingZeroes(MODE_INPUT_STREAM_THROTTLED, false);
        _testLeadingZeroes(MODE_INPUT_STREAM_THROTTLED, true);

        // 17-May-2016, tatu: With DataInput, must have trailing space
        //   since there's no way to detect end of input
        _testLeadingZeroes(MODE_DATA_INPUT, true);
    }

    public void testLeadingZeroesReader() throws Exception {
        _testLeadingZeroes(MODE_READER, false);
        _testLeadingZeroes(MODE_READER, true);
    }

    // allow NaN
    public void testAllowNaN() throws Exception {
        _testAllowNaN(MODE_INPUT_STREAM);
        _testAllowNaN(MODE_INPUT_STREAM_THROTTLED);
        _testAllowNaN(MODE_DATA_INPUT);
        _testAllowNaN(MODE_READER);
    }

    // allow +Inf/-Inf
    public void testAllowInfinity() throws Exception {
        _testAllowInf(MODE_INPUT_STREAM);
        _testAllowInf(MODE_INPUT_STREAM_THROTTLED);
        _testAllowInf(MODE_DATA_INPUT);
        _testAllowInf(MODE_READER);
    }

    /*
    /****************************************************************
    /* Secondary test methods
    /****************************************************************
     */

    private void _testLargeUnquoted(int mode) throws Exception
    {
        StringBuilder sb = new StringBuilder(5000);
        sb.append("[\n");
        //final int REPS = 2000;
        final int REPS = 1050;
        for (int i = 0; i < REPS; ++i) {
            if (i > 0) {
                sb.append(',');
                if ((i & 7) == 0) {
                    sb.append('\n');
                }
            }
            sb.append("{");
            sb.append("abc").append(i&127).append(':');
            sb.append((i & 1) != 0);
            sb.append("}\n");
        }
        sb.append("]");
        String JSON = sb.toString();
        JsonFactory f = new JsonFactory();
        f.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        JsonParser p = createParser(f, mode, JSON);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        for (int i = 0; i < REPS; ++i) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            assertEquals("abc"+(i&127), p.getCurrentName());
            assertToken(((i&1) != 0) ? JsonToken.VALUE_TRUE : JsonToken.VALUE_FALSE, p.nextToken());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();
    }

    
    private void _testSimpleUnquoted(int mode) throws Exception
    {
        final JsonFactory f = new JsonFactory();
        f.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

        String JSON = "{ a : 1, _foo:true, $:\"money!\", \" \":null }";
        JsonParser p = createParser(f, mode, JSON);

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("a", p.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("_foo", p.getCurrentName());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("$", p.getCurrentName());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("money!", p.getText());

        // and then regular quoted one should still work too:
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals(" ", p.getCurrentName());

        assertToken(JsonToken.VALUE_NULL, p.nextToken());

        assertToken(JsonToken.END_OBJECT, p.nextToken());
        p.close();

        // Another thing, as per [Issue#102]: numbers

        JSON = "{ 123:true,4:false }";
        p = createParser(f, mode, JSON);

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("123", p.getCurrentName());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("4", p.getCurrentName());
        assertToken(JsonToken.VALUE_FALSE, p.nextToken());

        assertToken(JsonToken.END_OBJECT, p.nextToken());
        p.close();
    }

    /**
     * Test to verify that the default parser settings do not
     * accept single-quotes for String values (field names,
     * textual values)
     */
    private void _testSingleQuotesDefault(int mode) throws Exception
    {
        JsonFactory f = new JsonFactory();
        // First, let's see that by default they are not allowed
        String JSON = "[ 'text' ]";
        JsonParser p = createParser(f, mode, JSON);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            p.nextToken();
            fail("Expected exception");
        } catch (JsonParseException e) {
            verifyException(e, "Unexpected character ('''");
        } finally {
            p.close();
        }

        JSON = "{ 'a':1 }";
        p = createParser(f, mode, JSON);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        try {
            p.nextToken();
            fail("Expected exception");
        } catch (JsonParseException e) {
            verifyException(e, "Unexpected character ('''");
        } finally {
            p.close();
        }
    }

    /**
     * Test to verify optional handling of
     * single quotes, to allow handling invalid (but, alas, common)
     * JSON.
     */
    private void _testSingleQuotesEnabled(int mode) throws Exception
    {
        JsonFactory f = new JsonFactory();
        f.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);

        String JSON = "{ 'a' : 1, \"foobar\": 'b', '_abcde1234':'d', '\"' : '\"\"', '':'' }";
        JsonParser p = createParser(f, mode, JSON);

        assertToken(JsonToken.START_OBJECT, p.nextToken());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("a", p.getText());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals("1", p.getText());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("foobar", p.getText());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("b", p.getText());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("_abcde1234", p.getText());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("d", p.getText());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("\"", p.getText());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        //assertEquals("\"\"", p.getText());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("", p.getText());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("", p.getText());

        assertToken(JsonToken.END_OBJECT, p.nextToken());
        p.close();


        JSON = "{'b':1,'array':[{'b':3}],'ob':{'b':4,'x':0,'y':3,'a':false }}";
        p = createParser(f, mode, JSON);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("b", p.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("b", p.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(3, p.getIntValue());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertToken(JsonToken.START_OBJECT, p.nextToken());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("b", p.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(4, p.getIntValue());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("x", p.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(0, p.getIntValue());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("y", p.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(3, p.getIntValue());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("a", p.getCurrentName());
        assertToken(JsonToken.VALUE_FALSE, p.nextToken());

        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        p.close();
    }

    // test to verify that we implicitly allow escaping of apostrophe
    private void _testSingleQuotesEscaped(int mode) throws Exception
    {
        JsonFactory f = new JsonFactory();
        f.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);

        String JSON = "[ '16\\'' ]";
        JsonParser p = createParser(f, mode, JSON);

        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("16'", p.getText());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();
    }
    
    private void _testNonStandardNameChars(int mode) throws Exception
    {
        JsonFactory f = new JsonFactory();
        f.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        String JSON = "{ @type : \"mytype\", #color : 123, *error* : true, "
            +" hyphen-ated : \"yes\", me+my : null"
            +"}";
        JsonParser p = createParser(f, mode, JSON);

        assertToken(JsonToken.START_OBJECT, p.nextToken());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("@type", p.getText());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("mytype", p.getText());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("#color", p.getText());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(123, p.getIntValue());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("*error*", p.getText());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("hyphen-ated", p.getText());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("yes", p.getText());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("me+my", p.getText());
        assertToken(JsonToken.VALUE_NULL, p.nextToken());
    
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        p.close();
    }

    private void _testNonStandarBackslashQuoting(int mode) throws Exception
    {
        // first: verify that we get an exception
        JsonFactory f = new JsonFactory();
        assertFalse(f.isEnabled(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER));
        final String JSON = quote("\\'");
        JsonParser p = createParser(f, mode, JSON);
        try {      
            p.nextToken();
            p.getText();
            fail("Should have thrown an exception for doc <"+JSON+">");
        } catch (JsonParseException e) {
            verifyException(e, "unrecognized character escape");
        } finally {
            p.close();
        }
        // and then verify it's ok...
        f.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);
        assertTrue(f.isEnabled(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER));
        p = createParser(f, mode, JSON);
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("'", p.getText());
        p.close();
    }

    private void _testLeadingZeroes(int mode, boolean appendSpace) throws Exception
    {
        // first: verify that we get an exception
        JsonFactory f = new JsonFactory();
        assertFalse(f.isEnabled(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS));
        String JSON = "00003";
        if (appendSpace) {
            JSON += " ";
        }
        JsonParser p = createParser(f, mode, JSON);
        try {      
            p.nextToken();
            p.getText();
            fail("Should have thrown an exception for doc <"+JSON+">");
        } catch (JsonParseException e) {
            verifyException(e, "invalid numeric value");
        } finally {
            p.close();
        }
        
        // and then verify it's ok when enabled
        f.configure(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS, true);
        assertTrue(f.isEnabled(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS));
        p = createParser(f, mode, JSON);
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(3, p.getIntValue());
        assertEquals("3", p.getText());
        p.close();
    
        // Plus, also: verify that leading zero magnitude is ok:
        JSON = "0"+Integer.MAX_VALUE;
        if (appendSpace) {
            JSON += " ";
        }
        p = createParser(f, mode, JSON);
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(String.valueOf(Integer.MAX_VALUE), p.getText());
        assertEquals(Integer.MAX_VALUE, p.getIntValue());
        Number nr = p.getNumberValue();
        assertSame(Integer.class, nr.getClass());
        p.close();
    }

    private void _testAllowNaN(int mode) throws Exception
    {
        final String JSON = "[ NaN]";
        JsonFactory f = new JsonFactory();
        assertFalse(f.isEnabled(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS));

        // without enabling, should get an exception
        JsonParser p = createParser(f, mode, JSON);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            p.nextToken();
            fail("Expected exception");
        } catch (Exception e) {
            verifyException(e, "non-standard");
        } finally {
            p.close();
        }

        // we can enable it dynamically (impl detail)
        f.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true);
        p = createParser(f, mode, JSON);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        
        double d = p.getDoubleValue();
        assertTrue(Double.isNaN(d));
        assertEquals("NaN", p.getText());

        // [Issue#98]
        try {
            /*BigDecimal dec =*/ p.getDecimalValue();
            fail("Should fail when trying to access NaN as BigDecimal");
        } catch (NumberFormatException e) {
            verifyException(e, "can not be represented as BigDecimal");
        }
       
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();

        // finally, should also work with skipping
        f.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true);
        p = createParser(f, mode, JSON);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();
    }

    private void _testAllowInf(int mode) throws Exception
    {
        final String JSON = "[ -INF, +INF, +Infinity, Infinity, -Infinity ]";
        JsonFactory f = new JsonFactory();
        assertFalse(f.isEnabled(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS));

        // without enabling, should get an exception
        JsonParser p = createParser(f, mode, JSON);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            p.nextToken();
            fail("Expected exception");
        } catch (Exception e) {
            verifyException(e, "Non-standard token '-INF'");
        } finally {
            p.close();
        }
        f.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true);
        p = createParser(f, mode, JSON);
        assertToken(JsonToken.START_ARRAY, p.nextToken());

        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        double d = p.getDoubleValue();
        assertEquals("-INF", p.getText());
        assertTrue(Double.isInfinite(d));
        assertTrue(d == Double.NEGATIVE_INFINITY);

        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        d = p.getDoubleValue();
        assertEquals("+INF", p.getText());
        assertTrue(Double.isInfinite(d));
        assertTrue(d == Double.POSITIVE_INFINITY);

        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        d = p.getDoubleValue();
        assertEquals("+Infinity", p.getText());
        assertTrue(Double.isInfinite(d));
        assertTrue(d == Double.POSITIVE_INFINITY);

        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        d = p.getDoubleValue();
        assertEquals("Infinity", p.getText());
        assertTrue(Double.isInfinite(d));
        assertTrue(d == Double.POSITIVE_INFINITY);

        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        d = p.getDoubleValue();
        assertEquals("-Infinity", p.getText());
        assertTrue(Double.isInfinite(d));
        assertTrue(d == Double.NEGATIVE_INFINITY);

        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();

        // finally, should also work with skipping
        f.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true);
        p = createParser(f, mode, JSON);

        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        
        p.close();
    }
}
