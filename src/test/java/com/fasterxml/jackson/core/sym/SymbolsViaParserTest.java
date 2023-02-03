package com.fasterxml.jackson.core.sym;

import java.io.IOException;
import java.util.HashSet;

import com.fasterxml.jackson.core.*;

// Tests to guarad against [core#213]: does not verify symbol tables
// directly but only indirect issue(s).
public class SymbolsViaParserTest
    extends com.fasterxml.jackson.core.BaseTest
{
    // for [jackson-core#213]
    public void test17CharSymbols() throws Exception {
        _test17Chars(false);
    }

    // for [jackson-core#213]
    public void test17ByteSymbols() throws Exception {
        _test17Chars(true);
    }

    // for [jackson-core#216]
    public void testSymbolTableExpansionChars() throws Exception {
        _testSymbolTableExpansion(false);
    }

    // for [jackson-core#216]
    public void testSymbolTableExpansionBytes() throws Exception {
        _testSymbolTableExpansion(true);
    }

    /*
    /**********************************************************
    /* Secondary test methods
    /**********************************************************
     */

    private void _test17Chars(boolean useBytes) throws IOException
    {
        String doc = _createDoc17();
        JsonFactory f = new JsonFactory();

        JsonParser p = useBytes
                ? f.createParser(doc.getBytes("UTF-8"))
                : f.createParser(doc);
        HashSet<String> syms = new HashSet<String>();
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        for (int i = 0; i < 50; ++i) {
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            syms.add(p.getCurrentName());
            assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        }
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertEquals(50, syms.size());
        p.close();
    }

    private String _createDoc17() {
        StringBuilder sb = new StringBuilder(1000);
        sb.append("{\n");
        for (int i = 1; i <= 50; ++i) {
            if (i > 1) {
                sb.append(",\n");
            }
            sb.append("\"lengthmatters")
                .append(1000 + i)
                .append("\": true");
        }
        sb.append("\n}");
        return sb.toString();
    }

    public void _testSymbolTableExpansion(boolean useBytes) throws Exception
    {
        JsonFactory jsonFactory = new JsonFactory();
        // Important: must create separate documents to gradually build up symbol table
        for (int i = 0; i < 200; i++) {
            String field = Integer.toString(i);
            final String doc = "{ \"" + field + "\" : \"test\" }";
            JsonParser parser = useBytes
                    ? jsonFactory.createParser(doc.getBytes("UTF-8"))
                    : jsonFactory.createParser(doc);
            assertToken(JsonToken.START_OBJECT, parser.nextToken());
            assertToken(JsonToken.FIELD_NAME, parser.nextToken());
            assertEquals(field, parser.getCurrentName());
            assertToken(JsonToken.VALUE_STRING, parser.nextToken());
            assertToken(JsonToken.END_OBJECT, parser.nextToken());
            assertNull(parser.nextToken());
            parser.close();
        }
    }
}
