package tools.jackson.core.unittest.sym;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.ReaderBasedJsonParser;
import tools.jackson.core.json.UTF8StreamJsonParser;
import tools.jackson.core.sym.CharsToNameCanonicalizer;
import tools.jackson.core.unittest.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for verifying that {@link JsonParser} instances properly
 * merge back symbols to the root symbol table
 */
@SuppressWarnings("serial")
class SymbolTableMergingTest
    extends JacksonCoreTestBase
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
        JsonParser p = _getParser(f, JSON, true);
        while (p.nextToken() != null) {
            // shouldn't update before hitting end
            assertEquals(0, f.byteSymbolCount());
        }
        // but now should have it after hitting EOF
        assertEquals(3, f.byteSymbolCount());
        p.close();
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
        JsonParser p = _getParser(f, JSON, false);
        while (p.nextToken() != null) {
            // shouldn't update before hitting end
            assertEquals(0, f.charSymbolCount());
        }
        // but now should have it
        assertEquals(3, f.charSymbolCount());
        p.close();
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
        JsonParser p = _getParser(f, JSON, useBytes);
        // Let's check 2 names
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());

        // shouldn't update before close or EOF:
        assertEquals(0, useBytes ? f.byteSymbolCount() : f.charSymbolCount());
        p.close();
        // but should after close
        assertEquals(2, useBytes ? f.byteSymbolCount() : f.charSymbolCount());
    }

    private JsonParser _getParser(MyJsonFactory f, String doc, boolean useBytes) throws IOException
    {
        JsonParser p;
        if (useBytes) {
            p = f.createParser(ObjectReadContext.empty(), doc.getBytes("UTF-8"));
            assertEquals(UTF8StreamJsonParser.class, p.getClass());
            assertEquals(0, f.byteSymbolCount());
        } else {
            p = f.createParser(ObjectReadContext.empty(), doc);
            assertEquals(ReaderBasedJsonParser.class, p.getClass());
            assertEquals(0, f.charSymbolCount());
        }
        return p;
    }
}
