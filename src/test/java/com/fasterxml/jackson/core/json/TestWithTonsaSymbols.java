package com.fasterxml.jackson.core.json;


import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.test.BaseTest;

import java.io.*;

/**
 * Some unit tests to try to exercise part of parser code that
 * deals with symbol (table) management.
 */
public class TestWithTonsaSymbols
    extends BaseTest
{
    /**
     * How many fields to generate? Since maximum symbol table
     * size is defined as 6000 (above which table gets cleared,
     * assuming the name vocabulary is unbounded), let's do something
     * just slightly below it.
     */
    final static int FIELD_COUNT = 5000;

    public void testStreamReaderParser() throws Exception
    {
        _testWith(true);
    }

    public void testReaderParser() throws Exception
    {
        _testWith(false);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private void _testWith(boolean useStream)
        throws Exception
    {
        JsonFactory jf = new JsonFactory();
        String doc = buildDoc(FIELD_COUNT);

        /* And let's do this multiple times: just so that symbol table
         * state is different between runs.
         */
        for (int x = 0; x < 3; ++x) {
            JsonParser jp = useStream ?
                jf.createParser(new ByteArrayInputStream(doc.getBytes("UTF-8")))
                : jf.createParser(new StringReader(doc));
            assertToken(JsonToken.START_OBJECT, jp.nextToken());
            for (int i = 0; i < FIELD_COUNT; ++i) {
                assertToken(JsonToken.FIELD_NAME, jp.nextToken());
                assertEquals(fieldNameFor(i), jp.getCurrentName());
                assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
                assertEquals(i, jp.getIntValue());
            }
            assertToken(JsonToken.END_OBJECT, jp.nextToken());
            jp.close();
        }
    }
        
    private String buildDoc(int len)
    {
        StringBuilder sb = new StringBuilder(len * 12);
        sb.append('{');
        for (int i = 0; i < len; ++i) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('"');
            fieldNameFor(sb, i);
            sb.append('"');
            sb.append(':');
            sb.append(i);
        }
        sb.append('}');
        return sb.toString();
    }
}
