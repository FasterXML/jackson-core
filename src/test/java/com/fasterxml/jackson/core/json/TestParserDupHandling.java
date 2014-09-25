package com.fasterxml.jackson.core.json;

import com.fasterxml.jackson.core.*;

public class TestParserDupHandling
    extends com.fasterxml.jackson.core.BaseTest
{
    public void testSimpleDups() throws Exception
    {
        for (String doc : new String[] {
            "{ 'a':1, 'a':2 }",
            "[{ 'a':1, 'a':2 }]",
            "{ 'a':1, 'b':2, 'c':3,'a':true,'e':false }",
            "{ 'foo': { 'bar': [ [ { 'x':3, 'a':1 } ]], 'x':0, 'a':'y', 'b':3,'a':13 } }",
            "[{'b':1},{'b\":3},[{'a':3}], {'a':1,'a':2}]",
            "{'b':1,'array':[{'b':3}],'ob':{'b':4,'x':0,'y':3,'a':true,'a':false }}",
        }) {
            doc = doc.replace("'", "\"");
            JsonFactory f = new JsonFactory();
            assertFalse(f.isEnabled(JsonParser.Feature.STRICT_DUPLICATE_DETECTION));
            _testSimpleDupsOk(doc, f, false);
            _testSimpleDupsOk(doc, f, true);
    
            f.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
            _testSimpleDupsFail(doc, f, false, "a");
            _testSimpleDupsFail(doc, f, true, "a");
        }
    }

    private void _testSimpleDupsOk(final String doc, JsonFactory f, boolean useStream) throws Exception
    {
        JsonParser jp = useStream ?
                createParserUsingStream(f, doc, "UTF-8") : createParserUsingReader(f, doc);
        JsonToken t = jp.nextToken();
        assertNotNull(t);
        assertTrue(t.isStructStart());
        while (jp.nextToken() != null) { }
        jp.close();
    }

    private void _testSimpleDupsFail(final String doc, JsonFactory f,
            boolean useStream, String name) throws Exception
    {
        JsonParser jp = useStream ?
                createParserUsingStream(f, doc, "UTF-8") : createParserUsingReader(f, doc);
        JsonToken t = jp.nextToken();
        assertNotNull(t);
        assertTrue(t.isStructStart());
        try {
            while (jp.nextToken() != null) { }
            fail("Should have caught dups in document: "+doc);
        } catch (JsonParseException e) {
            verifyException(e, "duplicate field '"+name+"'");
        }
        jp.close();
    }
    
}
