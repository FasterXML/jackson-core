package com.fasterxml.jackson.failing.async;

import java.io.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;

/**
 * Unit tests for verifying that support for (non-standard) comments
 * works as expected.
 */
public class AsyncCommentParsingTest extends AsyncTestBase
{
    final static String DOC_WITH_SLASHSTAR_COMMENT =
        "[ /* comment:\n ends here */ 1 /* one more ok to have \"unquoted\"  */ ]"
        ;

    final static String DOC_WITH_SLASHSLASH_COMMENT =
        "[ // comment...\n 1 \r  // one more, not array: []   \n ]"
        ;

    /*
    /**********************************************************
    /* Test method wrappers
    /**********************************************************
     */

    public void testCommentsDisabled() throws Exception
    {
        _testDisabled(DOC_WITH_SLASHSTAR_COMMENT);
        _testDisabled(DOC_WITH_SLASHSLASH_COMMENT);
    }

    public void testCommentsEnabled() throws Exception
    {
        _testEnabled(DOC_WITH_SLASHSTAR_COMMENT);
        _testEnabled(DOC_WITH_SLASHSLASH_COMMENT);
    }

    public void testCommentsWithUTF8() throws Exception
    {
        final String JSON = "/* \u00a9 2099 Yoyodyne Inc. */\n [ \"bar? \u00a9\" ]\n";
        _testWithUTF8Chars(JSON);
    }

    public void testYAMLComments() throws Exception
    {
        JsonFactory f = new JsonFactory();
        f.enable(JsonParser.Feature.ALLOW_YAML_COMMENTS);

        _testYAMLComments(f);
        _testCommentsBeforePropValue(f, "# foo\n");

        _testCommentsBetweenArrayValues(f, "# foo\n");
    }

    public void testCComments() throws Exception {
        JsonFactory f = new JsonFactory();
        f.enable(JsonParser.Feature.ALLOW_COMMENTS);
        final String COMMENT = "/* foo */\n";
        _testCommentsBeforePropValue(f, COMMENT);
    }

    public void testCppComments() throws Exception {
        JsonFactory f = new JsonFactory();
        f.enable(JsonParser.Feature.ALLOW_COMMENTS);
        final String COMMENT = "// foo\n";
        _testCommentsBeforePropValue(f, COMMENT);
    }

    private void _testCommentsBeforePropValue(JsonFactory f,
            String comment) throws Exception
    {
        for (String arg : new String[] {
                ":%s123",
                " :%s123",
                "\t:%s123",
                ": %s123",
                ":\t%s123",
        }) {
            String commented = String.format(arg, comment);
            
            final String DOC = "{\"abc\"" + commented + "}";
            AsyncReaderWrapper p = _createParser(f, DOC, 3);
            assertEquals(JsonToken.START_OBJECT, p.nextToken());
            JsonToken t = null;
            try {
                t = p.nextToken();
            } catch (Exception e) {
                throw new RuntimeException("Failed on '"+DOC+"' due to "+e, e);
            }
            assertEquals(JsonToken.FIELD_NAME, t);

            try {
                t = p.nextToken();
            } catch (Exception e) {
                throw new RuntimeException("Failed on '"+DOC+"' due to "+e, e);
            }
            assertEquals(JsonToken.VALUE_NUMBER_INT, t);
            assertEquals(123, p.getIntValue());
            assertEquals(JsonToken.END_OBJECT, p.nextToken());
            p.close();
        }
    }

    private void _testCommentsBetweenArrayValues(JsonFactory f,
            String comment) throws Exception
    {
        for (String tmpl : new String[] {
                "%s,",
                " %s,",
                "\t%s,",
                "%s ,",
                "%s\t,",
                " %s ,",
                "\t%s\t,",
                "\n%s,",
                "%s\n,",
        }) {
            String commented = String.format(tmpl, comment);
            
            final String DOC = "[1"+commented+"2]";
            AsyncReaderWrapper p = _createParser(f, DOC, 3);
            assertEquals(JsonToken.START_ARRAY, p.nextToken());
            JsonToken t = null;
            try {
                t = p.nextToken();
            } catch (Exception e) {
                throw new RuntimeException("Failed on '"+DOC+"' due to "+e, e);
            }
            assertEquals(JsonToken.VALUE_NUMBER_INT, t);
            assertEquals(1, p.getIntValue());

            try {
                t = p.nextToken();
            } catch (Exception e) {
                throw new RuntimeException("Failed on '"+DOC+"' due to "+e, e);
            }
            assertEquals(JsonToken.VALUE_NUMBER_INT, t);
            assertEquals(2, p.getIntValue());
            assertEquals(JsonToken.END_ARRAY, p.nextToken());
            p.close();
        }
        
    }
    
    private void _testYAMLComments(JsonFactory f) throws Exception
    {
        final String DOC = "# foo\n"
                +" {\"a\" # xyz\n"
                +" : # foo\n"
                +" 1, # more\n"
                +"\"b\": [ \n"
                +" #all!\n"
                +" 3 #yay!\n"
                +"] # foobar\n"
                +"} # x"
                ;
        AsyncReaderWrapper p = _createParser(f, DOC, 3);

        assertEquals(JsonToken.START_OBJECT, p.nextToken());
        assertEquals(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("a", p.currentName());
        assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(1, p.getIntValue());
        assertEquals(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("b", p.currentName());
        assertEquals(JsonToken.START_ARRAY, p.nextToken());
        assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(3, p.getIntValue());
        assertEquals(JsonToken.END_ARRAY, p.nextToken());
        assertEquals(JsonToken.END_OBJECT, p.nextToken());
        assertNull(p.nextToken());
        p.close();
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private void _testWithUTF8Chars(String doc) throws IOException
    {
        // should basically just stream through
        AsyncReaderWrapper p = _createParser(doc, true, 3);
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertNull(p.nextToken());
        p.close();
    }

    private void _testDisabled(String doc) throws IOException
    {
        AsyncReaderWrapper p = _createParser(doc, false, 3);
        try {
            p.nextToken();
            fail("Expected exception for unrecognized comment");
        } catch (JsonParseException je) {
            // Should have something denoting that user may want to enable 'ALLOW_COMMENTS'
            verifyException(je, "ALLOW_COMMENTS");
        }
        p.close();
    }

    private void _testEnabled(String doc) throws IOException
    {
        AsyncReaderWrapper p = _createParser(doc, true, 3);
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(1, p.getIntValue());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();
    }

    private AsyncReaderWrapper _createParser(String doc, boolean enabled,
            int bytesPerRead)
        throws IOException
    {
        JsonFactory f = new JsonFactory();
        f.configure(JsonParser.Feature.ALLOW_COMMENTS, enabled);
        AsyncReaderWrapper p = asyncForBytes(f, bytesPerRead, _jsonDoc(doc), 0);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        return p;
    }

    private AsyncReaderWrapper _createParser(JsonFactory f, String doc, int bytesPerRead)
        throws IOException
    {
        AsyncReaderWrapper p = asyncForBytes(f, bytesPerRead, _jsonDoc(doc), 0);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        return p;
    }
}
