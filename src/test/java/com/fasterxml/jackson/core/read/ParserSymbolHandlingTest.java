package com.fasterxml.jackson.core.read;

import com.fasterxml.jackson.core.*;

public class ParserSymbolHandlingTest
    extends com.fasterxml.jackson.core.BaseTest
{
    // For [core#148]
    public void testSymbolsWithNullBytes() throws Exception {
        JsonFactory f = new JsonFactory();
        _testSymbolsWithNull(f, true);
        // and repeat with same factory, just for fun, and to ensure symbol table is fine
        _testSymbolsWithNull(f, true);
    }

    // For [core#148]
    public void testSymbolsWithNullChars() throws Exception {
        JsonFactory f = new JsonFactory();
        _testSymbolsWithNull(f, false);
        _testSymbolsWithNull(f, false);
    }

    private void _testSymbolsWithNull(JsonFactory f, boolean useBytes) throws Exception
    {
        final String INPUT = "{\"\\u0000abc\" : 1, \"abc\":2}";
        JsonParser parser = useBytes ? f.createParser(INPUT.getBytes("UTF-8"))
                : f.createParser(INPUT);

        assertToken(JsonToken.START_OBJECT, parser.nextToken());

        assertToken(JsonToken.FIELD_NAME, parser.nextToken());
        String currName = parser.getCurrentName();
        if (!"\u0000abc".equals(currName)) {
            fail("Expected \\0abc (4 bytes), '"+currName+"' ("+currName.length()+")");
        }
        assertToken(JsonToken.VALUE_NUMBER_INT, parser.nextToken());
        assertEquals(1, parser.getIntValue());

        assertToken(JsonToken.FIELD_NAME, parser.nextToken());
        currName = parser.getCurrentName();
        if (!"abc".equals(currName)) {
            /*
            for (int i = 0; i < currName.length(); ++i) {
                System.out.println("#"+i+" -> 0x"+Integer.toHexString(currName.charAt(i)));
            }
            */
            fail("Expected 'abc' (3 bytes), '"+currName+"' ("+currName.length()+")");
        }
        assertToken(JsonToken.VALUE_NUMBER_INT, parser.nextToken());
        assertEquals(2, parser.getIntValue());

        assertToken(JsonToken.END_OBJECT, parser.nextToken());
        parser.close();
    }

    // // Additional testing inspired by [dataformats-binary#312]; did not
    // // affect JSON backend but wanted to ensure

    public void testSymbolsWithNullOnlyNameBytes() throws Exception {
        JsonFactory f = new JsonFactory();
        _testSymbolsWithNullOnlyNameBytes(f, true);
        // and repeat with same factory, just for fun, and to ensure symbol table is fine
        _testSymbolsWithNullOnlyNameBytes(f, true);
    }

    public void testSymbolsWithNullOnlyNameChars() throws Exception {
        JsonFactory f = new JsonFactory();
        _testSymbolsWithNullOnlyNameBytes(f, false);
        _testSymbolsWithNullOnlyNameBytes(f, false);
    }

    private void _testSymbolsWithNullOnlyNameBytes(JsonFactory f, boolean useBytes) throws Exception
    {
        final String FIELD1 = "\u0000";
        final String FIELD2 = FIELD1 + FIELD1;
        final String FIELD3 = FIELD2 + FIELD1;
        final String FIELD4 = FIELD3 + FIELD1;
        final String QUOTED_NULL = "\\u0000";

        final String INPUT = a2q(String.format("{'%s':1, '%s':2, '%s':3, '%s':4}",
                QUOTED_NULL, QUOTED_NULL + QUOTED_NULL,
                QUOTED_NULL + QUOTED_NULL + QUOTED_NULL,
                QUOTED_NULL + QUOTED_NULL + QUOTED_NULL + QUOTED_NULL
                ));
        JsonParser p = useBytes ? f.createParser(INPUT.getBytes("UTF-8"))
                : f.createParser(INPUT);

        assertToken(JsonToken.START_OBJECT, p.nextToken());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        _assertNullStrings(FIELD1, p.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(1, p.getIntValue());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        _assertNullStrings(FIELD2, p.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(2, p.getIntValue());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        _assertNullStrings(FIELD3, p.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(3, p.getIntValue());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        _assertNullStrings(FIELD4, p.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(4, p.getIntValue());

        assertToken(JsonToken.END_OBJECT, p.nextToken());
    }

    private void _assertNullStrings(String exp, String actual) {
        if (exp.length() != actual.length()) {
            fail("Expected "+exp.length()+" nulls, got "+actual.length());
        }
        assertEquals(exp, actual);
    }
}
