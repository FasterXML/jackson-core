package com.fasterxml.jackson.core.json.async;

import java.io.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.json.JsonReadFeature;
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
        _testEnabled(DOC_WITH_SLASHSTAR_COMMENT, 99);
        _testEnabled(DOC_WITH_SLASHSTAR_COMMENT, 3);
        _testEnabled(DOC_WITH_SLASHSTAR_COMMENT, 1);

        _testEnabled(DOC_WITH_SLASHSLASH_COMMENT, 99);
        _testEnabled(DOC_WITH_SLASHSLASH_COMMENT, 3);
        _testEnabled(DOC_WITH_SLASHSLASH_COMMENT, 1);
    }

    public void testCCommentsWithUTF8() throws Exception
    {
        final String JSON = "/* \u00a9 2099 Yoyodyne Inc. */\n [ \"bar? \u00a9\" ]\n";

        _testWithUTF8Chars(JSON, 99);
        _testWithUTF8Chars(JSON, 5);
        _testWithUTF8Chars(JSON, 3);
        _testWithUTF8Chars(JSON, 2);
        _testWithUTF8Chars(JSON, 1);
    }

    public void testYAMLCommentsEnabled() throws Exception
    {
        final JsonFactory f = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_YAML_COMMENTS)
                .build();
        _testYAMLComments(f, 99);
        _testYAMLComments(f, 3);
        _testYAMLComments(f, 1);

        _testCommentsBeforePropValue(f, "# foo\n", 99);
        _testCommentsBeforePropValue(f, "# foo\n", 3);
        _testCommentsBeforePropValue(f, "# foo\n", 1);

        _testCommentsBetweenArrayValues(f, "# foo\n", 99);
        _testCommentsBetweenArrayValues(f, "# foo\n", 3);
        _testCommentsBetweenArrayValues(f, "# foo\n", 1);
    }

    public void testCCommentsEnabled() throws Exception {
        final JsonFactory f = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
                .build();
        final String COMMENT = "/* foo */\n";
        _testCommentsBeforePropValue(f, COMMENT, 99);
        _testCommentsBeforePropValue(f, COMMENT, 3);
        _testCommentsBeforePropValue(f, COMMENT, 1);
    }

    public void testCppCommentsEnabled() throws Exception {
        final JsonFactory f = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
                .build();
        final String COMMENT = "// foo\n";
        _testCommentsBeforePropValue(f, COMMENT, 99);
        _testCommentsBeforePropValue(f, COMMENT, 3);
        _testCommentsBeforePropValue(f, COMMENT, 1);
    }

    private void _testCommentsBeforePropValue(JsonFactory f,
            String comment, int bytesPerRead) throws Exception
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
            AsyncReaderWrapper p = _createParser(f, DOC, bytesPerRead);
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
            String comment, int bytesPerRead) throws Exception
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
            AsyncReaderWrapper p = _createParser(f, DOC, bytesPerRead);
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

    private void _testYAMLComments(JsonFactory f, int bytesPerRead) throws Exception
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
        AsyncReaderWrapper p = _createParser(f, DOC, bytesPerRead);

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

    private void _testWithUTF8Chars(String doc, int bytesPerRead) throws IOException
    {
        // should basically just stream through
        AsyncReaderWrapper p = _createParser(doc, true, bytesPerRead);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertNull(p.nextToken());
        p.close();
    }

    private void _testDisabled(String doc) throws IOException
    {
        AsyncReaderWrapper p = _createParser(doc, false, 3);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            p.nextToken();
            fail("Expected exception for unrecognized comment");
        } catch (JsonParseException je) {
            // Should have something denoting that user may want to enable 'ALLOW_COMMENTS'
            verifyException(je, "ALLOW_COMMENTS");
        }
        p.close();
    }

    private void _testEnabled(String doc, int bytesPerRead) throws IOException
    {
        AsyncReaderWrapper p = _createParser(doc, true, bytesPerRead);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(1, p.getIntValue());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();
    }

    private AsyncReaderWrapper _createParser(String doc, boolean enabled,
            int bytesPerRead)
        throws IOException
    {
        final JsonFactory f = JsonFactory.builder()
                .configure(JsonReadFeature.ALLOW_JAVA_COMMENTS, enabled)
                .build();
        return asyncForBytes(f, bytesPerRead, _jsonDoc(doc), 0);
    }

    private AsyncReaderWrapper _createParser(JsonFactory f, String doc, int bytesPerRead)
        throws IOException
    {
        return asyncForBytes(f, bytesPerRead, _jsonDoc(doc), 0);
    }
}
