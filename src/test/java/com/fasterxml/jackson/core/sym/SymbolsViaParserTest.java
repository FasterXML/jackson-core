package com.fasterxml.jackson.core.sym;

import java.io.IOException;
import java.util.HashSet;

import com.fasterxml.jackson.core.*;

/**
 * Tests that use symbol table functionality through parser.
 */
public class SymbolsViaParserTest
    extends com.fasterxml.jackson.core.BaseTest
{
    // for [jackson-core#213]
    public void test17CharSymbols() throws Exception
    {
        _test17Chars(false);
    }

    // for [jackson-core#213]
    public void test17ByteSymbols() throws Exception
    {
        _test17Chars(true);
    }

    private void _test17Chars(boolean useBytes) throws IOException
    {
        String doc = _createDoc();
        JsonFactory f = new JsonFactory();
        
        JsonParser p;
        if (useBytes) {
            p = f.createParser(doc.getBytes("UTF-8"));
        } else {
            p = f.createParser(doc);
        }
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

    private String _createDoc() {
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
}
