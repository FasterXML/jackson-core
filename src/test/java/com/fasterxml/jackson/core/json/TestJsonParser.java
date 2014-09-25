package com.fasterxml.jackson.core.json;

import com.fasterxml.jackson.core.*;

import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * Set of basic unit tests for verifying that the basic parser
 * functionality works as expected.
 */
public class TestJsonParser
    extends com.fasterxml.jackson.core.BaseTest
{
    private final JsonFactory JSON_FACTORY = new JsonFactory();

    public void testConfig() throws Exception
    {
        JsonParser jp = createParserUsingReader("[ ]");
        jp.enable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
        assertTrue(jp.isEnabled(JsonParser.Feature.AUTO_CLOSE_SOURCE));
        jp.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
        assertFalse(jp.isEnabled(JsonParser.Feature.AUTO_CLOSE_SOURCE));

        jp.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);
        assertTrue(jp.isEnabled(JsonParser.Feature.AUTO_CLOSE_SOURCE));
        jp.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
        assertFalse(jp.isEnabled(JsonParser.Feature.AUTO_CLOSE_SOURCE));
        jp.close();
    }

    public void testInterningWithStreams() throws Exception
    {
        _testIntern(true, true, "a");
        _testIntern(true, false, "b");
    }

    public void testInterningWithReaders() throws Exception
    {
        _testIntern(false, true, "c");
        _testIntern(false, false, "d");
    }
    
    private void _testIntern(boolean useStream, boolean enableIntern, String expName) throws IOException
    {
        JsonFactory f = new JsonFactory();
        f.configure(JsonFactory.Feature.INTERN_FIELD_NAMES, enableIntern);
        assertEquals(enableIntern, f.isEnabled(JsonFactory.Feature.INTERN_FIELD_NAMES));
        final String JSON = "{ \""+expName+"\" : 1}";
        JsonParser jp = useStream ?
            createParserUsingStream(f, JSON, "UTF-8") : createParserUsingReader(f, JSON);
            
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        // needs to be same of cours
        String actName = jp.getCurrentName();
        assertEquals(expName, actName);
        if (enableIntern) {
            assertSame(expName, actName);
        } else {
            assertNotSame(expName, actName);
        }
        jp.close();
    }

    /**
     * This basic unit test verifies that example given in the Json
     * specification (RFC-4627 or later) is properly parsed at
     * high-level, without verifying values.
     */
    public void testSpecExampleSkipping()
        throws Exception
    {
        doTestSpec(false);
    }

    /**
     * Unit test that verifies that the spec example JSON is completely
     * parsed, and proper values are given for contents of all
     * events/tokens.
     */
    public void testSpecExampleFully()
        throws Exception
    {
        doTestSpec(true);
    }

    /**
     * Unit test that verifies that 3 basic keywords (null, true, false)
     * are properly parsed in various contexts.
     */
    public void testKeywords()
        throws Exception
    {
        final String DOC = "{\n"
            +"\"key1\" : null,\n"
            +"\"key2\" : true,\n"
            +"\"key3\" : false,\n"
            +"\"key4\" : [ false, null, true ]\n"
            +"}"
            ;

        JsonParser jp = createParserUsingStream(DOC, "UTF-8");

        JsonStreamContext ctxt = jp.getParsingContext();
        assertTrue(ctxt.inRoot());
        assertFalse(ctxt.inArray());
        assertFalse(ctxt.inObject());
        assertEquals(0, ctxt.getEntryCount());
        assertEquals(0, ctxt.getCurrentIndex());

        /* Before advancing to content, we should have following
         * default state...
         */
        assertFalse(jp.hasCurrentToken());
        assertNull(jp.getText());
        assertNull(jp.getTextCharacters());
        assertEquals(0, jp.getTextLength());
        // not sure if this is defined but:
        assertEquals(0, jp.getTextOffset());

        assertToken(JsonToken.START_OBJECT, jp.nextToken());

        assertTrue(jp.hasCurrentToken());
        JsonLocation loc = jp.getTokenLocation();
        assertNotNull(loc);
        assertEquals(1, loc.getLineNr());
        assertEquals(1, loc.getColumnNr());

        ctxt = jp.getParsingContext();
        assertFalse(ctxt.inRoot());
        assertFalse(ctxt.inArray());
        assertTrue(ctxt.inObject());
        assertEquals(0, ctxt.getEntryCount());
        assertEquals(0, ctxt.getCurrentIndex());

        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        verifyFieldName(jp, "key1");
        assertEquals(2, jp.getTokenLocation().getLineNr());

        ctxt = jp.getParsingContext();
        assertFalse(ctxt.inRoot());
        assertFalse(ctxt.inArray());
        assertTrue(ctxt.inObject());
        assertEquals(1, ctxt.getEntryCount());
        assertEquals(0, ctxt.getCurrentIndex());
        assertEquals("key1", ctxt.getCurrentName());

        assertToken(JsonToken.VALUE_NULL, jp.nextToken());
        assertEquals("key1", ctxt.getCurrentName());

        ctxt = jp.getParsingContext();
        assertEquals(1, ctxt.getEntryCount());
        assertEquals(0, ctxt.getCurrentIndex());

        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        verifyFieldName(jp, "key2");
        ctxt = jp.getParsingContext();
        assertEquals(2, ctxt.getEntryCount());
        assertEquals(1, ctxt.getCurrentIndex());
        assertEquals("key2", ctxt.getCurrentName());

        assertToken(JsonToken.VALUE_TRUE, jp.nextToken());
        assertEquals("key2", ctxt.getCurrentName());

        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        verifyFieldName(jp, "key3");
        assertToken(JsonToken.VALUE_FALSE, jp.nextToken());

        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        verifyFieldName(jp, "key4");

        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        ctxt = jp.getParsingContext();
        assertTrue(ctxt.inArray());
        assertNull(ctxt.getCurrentName());
        assertEquals("key4", ctxt.getParent().getCurrentName());
        
        assertToken(JsonToken.VALUE_FALSE, jp.nextToken());
        assertToken(JsonToken.VALUE_NULL, jp.nextToken());
        assertToken(JsonToken.VALUE_TRUE, jp.nextToken());
        assertToken(JsonToken.END_ARRAY, jp.nextToken());

        ctxt = jp.getParsingContext();
        assertTrue(ctxt.inObject());

        assertToken(JsonToken.END_OBJECT, jp.nextToken());
        ctxt = jp.getParsingContext();
        assertTrue(ctxt.inRoot());
        assertNull(ctxt.getCurrentName());

        jp.close();
    }

    public void testSkipping() throws Exception
    {
        String DOC =
            "[ 1, 3, [ true, null ], 3, { \"a\":\"b\" }, [ [ ] ], { } ]";
            ;
        JsonParser jp = createParserUsingStream(DOC, "UTF-8");

        // First, skipping of the whole thing
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        jp.skipChildren();
        assertEquals(JsonToken.END_ARRAY, jp.getCurrentToken());
        JsonToken t = jp.nextToken();
        if (t != null) {
            fail("Expected null at end of doc, got "+t);
        }
        jp.close();

        // Then individual ones
        jp = createParserUsingStream(DOC, "UTF-8");
        assertToken(JsonToken.START_ARRAY, jp.nextToken());

        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        jp.skipChildren();
        // shouldn't move
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.getCurrentToken());
        assertEquals(1, jp.getIntValue());

        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        // then skip array
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        jp.skipChildren();
        assertToken(JsonToken.END_ARRAY, jp.getCurrentToken());

        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        jp.skipChildren();
        assertToken(JsonToken.END_OBJECT, jp.getCurrentToken());

        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        jp.skipChildren();
        assertToken(JsonToken.END_ARRAY, jp.getCurrentToken());

        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        jp.skipChildren();
        assertToken(JsonToken.END_OBJECT, jp.getCurrentToken());

        assertToken(JsonToken.END_ARRAY, jp.nextToken());

        jp.close();
    }

    public void testNameEscaping() throws IOException
    {
        _testNameEscaping(false);
        _testNameEscaping(true);
    }

    private void _testNameEscaping(boolean useStream) throws IOException
    {
        final Map<String,String> NAME_MAP = new LinkedHashMap<String,String>();
        NAME_MAP.put("", "");
        NAME_MAP.put("\\\"funny\\\"", "\"funny\"");
        NAME_MAP.put("\\\\", "\\");
        NAME_MAP.put("\\r", "\r");
        NAME_MAP.put("\\n", "\n");
        NAME_MAP.put("\\t", "\t");
        NAME_MAP.put("\\r\\n", "\r\n");
        NAME_MAP.put("\\\"\\\"", "\"\"");
        NAME_MAP.put("Line\\nfeed", "Line\nfeed");
        NAME_MAP.put("Yet even longer \\\"name\\\"!", "Yet even longer \"name\"!");

        int entry = 0;
        for (Map.Entry<String,String> en : NAME_MAP.entrySet()) {
            ++entry;
            String input = en.getKey();
            String expResult = en.getValue();
            final String DOC = "{ \""+input+"\":null}";
            JsonParser jp = useStream ?
                    JSON_FACTORY.createParser(new ByteArrayInputStream(DOC.getBytes("UTF-8")))
                : JSON_FACTORY.createParser(new StringReader(DOC));

            assertToken(JsonToken.START_OBJECT, jp.nextToken());
            assertToken(JsonToken.FIELD_NAME, jp.nextToken());
            // first, sanity check (field name == getText()
            String act = jp.getCurrentName();
            assertEquals(act, getAndVerifyText(jp));
            if (!expResult.equals(act)) {
                String msg = "Failed for name #"+entry+"/"+NAME_MAP.size();
                if (expResult.length() != act.length()) {
                    fail(msg+": exp length "+expResult.length()+", actual "+act.length());
                }
                assertEquals(msg, expResult, act);
            }
            assertToken(JsonToken.VALUE_NULL, jp.nextToken());
            assertToken(JsonToken.END_OBJECT, jp.nextToken());
            jp.close();
        }
    }
    
    /**
     * Unit test that verifies that long text segments are handled
     * correctly; mostly to stress-test underlying segment-based
     * text buffer(s).
     */
    public void testLongText() throws Exception
    {
        // lengths chosen to tease out problems with buffer allocation...
        _testLongText(7700);
        _testLongText(49000);
        _testLongText(96000);
    }

    @SuppressWarnings("resource")
    private void _testLongText(int LEN) throws Exception
    {
        StringBuilder sb = new StringBuilder(LEN + 100);
        Random r = new Random(99);
        while (sb.length() < LEN) {
            sb.append(r.nextInt());
            sb.append(" xyz foo");
            if (r.nextBoolean()) {
                sb.append(" and \"bar\"");
            } else if (r.nextBoolean()) {
                sb.append(" [whatever].... ");
            } else {
                // Let's try some more 'exotic' chars
                sb.append(" UTF-8-fu: try this {\u00E2/\u0BF8/\uA123!} (look funny?)");
            }
            if (r.nextBoolean()) {
                if (r.nextBoolean()) {
                    sb.append('\n');
                } else if (r.nextBoolean()) {
                    sb.append('\r');
                } else {
                    sb.append("\r\n");
                }
            }
        }
        final String VALUE = sb.toString();
        
        // Let's use real generator to get JSON done right
        StringWriter sw = new StringWriter(LEN + (LEN >> 2));
        JsonGenerator jg = JSON_FACTORY.createGenerator(sw);
        jg.writeStartObject();
        jg.writeFieldName("doc");
        jg.writeString(VALUE);
        jg.writeEndObject();
        jg.close();
        
        final String DOC = sw.toString();

        for (int type = 0; type < 3; ++type) {
            JsonParser jp;

            switch (type) {
            default:
                jp = JSON_FACTORY.createParser(DOC.getBytes("UTF-8"));
                break;
            case 1:
                jp = JSON_FACTORY.createParser(DOC);
                break;
            case 2: // NEW: let's also exercise UTF-32...
                jp = JSON_FACTORY.createParser(encodeInUTF32BE(DOC));
                break;
            }
            assertToken(JsonToken.START_OBJECT, jp.nextToken());
            assertToken(JsonToken.FIELD_NAME, jp.nextToken());
            assertEquals("doc", jp.getCurrentName());
            assertToken(JsonToken.VALUE_STRING, jp.nextToken());
            
            String act = getAndVerifyText(jp);
            if (act.length() != VALUE.length()) {
                fail("Expected length "+VALUE.length()+", got "+act.length());
            }
            if (!act.equals(VALUE)) {
                fail("Long text differs");
            }

            // should still know the field name
            assertEquals("doc", jp.getCurrentName());
            assertToken(JsonToken.END_OBJECT, jp.nextToken());
            assertNull(jp.nextToken());
            jp.close();
        }
    }

    /**
     * Simple unit test that verifies that passing in a byte array
     * as source works as expected.
     */
    public void testBytesAsSource() throws Exception
    {
        String JSON = "[ 1, 2, 3, 4 ]";
        byte[] b = JSON.getBytes("UTF-8");
        int offset = 50;
        int len = b.length;
        byte[] src = new byte[offset + len + offset];

        System.arraycopy(b, 0, src, offset, len);

        JsonParser jp = JSON_FACTORY.createParser(src, offset, len);

        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(1, jp.getIntValue());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(2, jp.getIntValue());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(3, jp.getIntValue());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(4, jp.getIntValue());
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        assertNull(jp.nextToken());

        jp.close();
    }

    // [JACKSON-632]
    public void testUtf8BOMHandling() throws Exception
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        // first, write BOM:
        bytes.write(0xEF);
        bytes.write(0xBB);
        bytes.write(0xBF);
        bytes.write("[ 1 ]".getBytes("UTF-8"));
        JsonParser jp = JSON_FACTORY.createParser(bytes.toByteArray());
        assertEquals(JsonToken.START_ARRAY, jp.nextToken());
        // should also have skipped first 3 bytes of BOM; but do we have offset available?
        /* 08-Oct-2013, tatu: Alas, due to [Issue#111], we have to omit BOM in calculations
         *   as we do not know what the offset is due to -- may need to revisit, if this
         *   discrepancy becomes an issue. For now it just means that BOM is considered
         *   "out of stream" (not part of input).
         */
        JsonLocation loc = jp.getTokenLocation();
        // so if BOM was consider in-stream (part of input), this should expect 3:
        assertEquals(0, loc.getByteOffset());
        assertEquals(-1, loc.getCharOffset());
        jp.close();
    }

    // [Issue#48]
    public void testSpacesInURL() throws Exception
    {
        File f = File.createTempFile("pre fix&stuff", ".txt");
        BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8"));
        w.write("{ }");
        w.close();
        URL url = f.toURI().toURL();

        JsonParser jp = JSON_FACTORY.createParser(url);
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertToken(JsonToken.END_OBJECT, jp.nextToken());
        jp.close();
    }

    // [#142]
    public void testHandlingOfInvalidSpaceBytes() throws Exception {
        _testHandlingOfInvalidSpace(true);
        _testHandlingOfInvalidSpaceFromResource(true);
    }
    
    // [#142]
    public void testHandlingOfInvalidSpaceChars() throws Exception {
        _testHandlingOfInvalidSpace(false);
        _testHandlingOfInvalidSpaceFromResource(false);
    }

    private void _testHandlingOfInvalidSpace(boolean useStream) throws Exception
    {
        final String JSON = "{ \u00A0 \"a\":1}";
        JsonParser jp = useStream
                ? createParserUsingStream(JSON_FACTORY, JSON, "UTF-8")
                : createParserUsingReader(JSON_FACTORY, JSON);
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        try {
            jp.nextToken();
            fail("Should have failed");
        } catch (JsonParseException e) {
            verifyException(e, "unexpected character");
            // and correct error code
            verifyException(e, "code 160");
        }
        jp.close();
    }

    private void _testHandlingOfInvalidSpaceFromResource(boolean useStream) throws Exception
    {
        InputStream in = getClass().getResourceAsStream("/test_0xA0.json");
        @SuppressWarnings("resource")
        JsonParser jp = useStream
                ? JSON_FACTORY.createParser(in)
                : JSON_FACTORY.createParser(new InputStreamReader(in, "UTF-8"));
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        try {
            assertToken(JsonToken.FIELD_NAME, jp.nextToken());
            assertEquals("request", jp.getCurrentName());
            assertToken(JsonToken.START_OBJECT, jp.nextToken());
            assertToken(JsonToken.FIELD_NAME, jp.nextToken());
            assertEquals("mac", jp.getCurrentName());
            assertToken(JsonToken.VALUE_STRING, jp.nextToken());
            assertNotNull(jp.getText());
            assertToken(JsonToken.FIELD_NAME, jp.nextToken());
            assertEquals("data", jp.getCurrentName());
            assertToken(JsonToken.START_OBJECT, jp.nextToken());

            // ... and from there on, just loop
            
            while (jp.nextToken()  != null) { }
            fail("Should have failed");
        } catch (JsonParseException e) {
            verifyException(e, "unexpected character");
            // and correct error code
            verifyException(e, "code 160");
        }
        jp.close();
    }
    
    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private void doTestSpec(boolean verify) throws IOException
    {
        // First, using a StringReader:
        doTestSpecIndividual(null, verify);

        // Then with streams using supported encodings:
        doTestSpecIndividual("UTF-8", verify);
        doTestSpecIndividual("UTF-16BE", verify);
        doTestSpecIndividual("UTF-16LE", verify);

        /* Hmmh. UTF-32 is harder only because JDK doesn't come with
         * a codec for it. Can't test it yet using this method
         */
        doTestSpecIndividual("UTF-32", verify);
    }

    private void doTestSpecIndividual(String enc, boolean verify) throws IOException
    {
        String doc = SAMPLE_DOC_JSON_SPEC;
        JsonParser jp;

        if (enc == null) {
            jp = createParserUsingReader(doc);
        } else {
            jp = createParserUsingStream(doc, enc);
        }
        verifyJsonSpecSampleDoc(jp, verify);
        jp.close();
    }
}

