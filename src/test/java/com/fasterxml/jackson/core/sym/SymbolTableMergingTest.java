package com.fasterxml.jackson.core.sym;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.json.ReaderBasedJsonParser;
import com.fasterxml.jackson.core.json.UTF8StreamJsonParser;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for verifying that {@link JsonParser} instances properly
 * merge back symbols to the root symbol table
 */
@SuppressWarnings("serial")
class SymbolTableMergingTest
        extends com.fasterxml.jackson.core.JUnit5TestBase
{
    /**
     * To peek into state of "root" symbol tables (parent of all symbol
     * tables for parsers constructed by this factory) we need to
     * add some methods.
     */
    final static class MyJsonFactory extends JsonFactory
    {
        public int byteSymbolCount() { return _byteSymbolCanonicalizer.size(); }
        public int charSymbolCount() { return _rootCharSymbols.size(); }
    }

    final static String JSON = "{ \"a\" : 3, \"aaa\" : 4, \"_a\" : 0 }";

    @Test
    void byteSymbolsWithClose() throws Exception
    {
        _testWithClose(true);
    }

    @Test
    void byteSymbolsWithEOF() throws Exception
    {
        MyJsonFactory f = new MyJsonFactory();
        JsonParser jp = _getParser(f, JSON, true);
        while (jp.nextToken() != null) {
            // shouldn't update before hitting end
            assertEquals(0, f.byteSymbolCount());
        }
        // but now should have it after hitting EOF
        assertEquals(3, f.byteSymbolCount());
        jp.close();
        assertEquals(3, f.byteSymbolCount());
    }

    @Test
    void hashCalc() throws Exception
    {
        CharsToNameCanonicalizer sym = CharsToNameCanonicalizer.createRoot(new JsonFactory());
        char[] str1 = "foo".toCharArray();
        char[] str2 = " foo ".toCharArray();

        assertEquals(sym.calcHash(str1, 0, 3), sym.calcHash(str2, 1, 3));
    }

    @Test
    void charSymbolsWithClose() throws Exception
    {
        _testWithClose(false);
    }

    @Test
    void charSymbolsWithEOF() throws Exception
    {
        MyJsonFactory f = new MyJsonFactory();
        JsonParser jp = _getParser(f, JSON, false);
        while (jp.nextToken() != null) {
            // shouldn't update before hitting end
            assertEquals(0, f.charSymbolCount());
        }
        // but now should have it
        assertEquals(3, f.charSymbolCount());
        jp.close();
        assertEquals(3, f.charSymbolCount());
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private void _testWithClose(boolean useBytes) throws IOException
    {
        MyJsonFactory f = new MyJsonFactory();
        JsonParser jp = _getParser(f, JSON, useBytes);
        // Let's check 2 names
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertToken(JsonToken.FIELD_NAME, jp.nextToken());

        // shouldn't update before close or EOF:
        assertEquals(0, useBytes ? f.byteSymbolCount() : f.charSymbolCount());
        jp.close();
        // but should after close
        assertEquals(2, useBytes ? f.byteSymbolCount() : f.charSymbolCount());
    }

    private JsonParser _getParser(MyJsonFactory f, String doc, boolean useBytes) throws IOException
    {
        JsonParser jp;
        if (useBytes) {
            jp = f.createParser(doc.getBytes("UTF-8"));
            assertEquals(UTF8StreamJsonParser.class, jp.getClass());
            assertEquals(0, f.byteSymbolCount());
        } else {
            jp = f.createParser(doc);
            assertEquals(ReaderBasedJsonParser.class, jp.getClass());
            assertEquals(0, f.charSymbolCount());
        }
        return jp;
    }
}
