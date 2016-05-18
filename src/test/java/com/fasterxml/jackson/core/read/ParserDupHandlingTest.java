package com.fasterxml.jackson.core.json;

import com.fasterxml.jackson.core.*;

public class TestParserDupHandling
    extends com.fasterxml.jackson.core.BaseTest
{
    private final String[] DUP_DOCS = new String[] {
            "{ 'a':1, 'a':2 }",
            "[{ 'a':1, 'a':2 }]",
            "{ 'a':1, 'b':2, 'c':3,'a':true,'e':false }",
            "{ 'foo': { 'bar': [ [ { 'x':3, 'a':1 } ]], 'x':0, 'a':'y', 'b':3,'a':13 } }",
            "[{'b':1},{'b\":3},[{'a':3}], {'a':1,'a':2}]",
            "{'b':1,'array':[{'b':3}],'ob':{'b':4,'x':0,'y':3,'a':true,'a':false }}",
    };
    {
        for (int i = 0; i < DUP_DOCS.length; ++i) {
            DUP_DOCS[i] = DUP_DOCS[i].replace("'", "\"");
        }
    }
    
    public void testSimpleDupsDisabled() throws Exception
    {
        // first: verify no problems if detection NOT enabled
        final JsonFactory f = new JsonFactory();
        assertFalse(f.isEnabled(JsonParser.Feature.STRICT_DUPLICATE_DETECTION));
        for (String doc : DUP_DOCS) {
            _testSimpleDupsOk(doc, f, false);
            _testSimpleDupsOk(doc, f, true);
        }
    }

    public void testSimpleDupsBytes() throws Exception
    {
        JsonFactory nonDupF = new JsonFactory();
        JsonFactory dupF = new JsonFactory();
        dupF.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        for (String doc : DUP_DOCS) {
            // First, with static setting
            _testSimpleDupsFail(doc, dupF, true, "a", false);

            // and then dynamic
            _testSimpleDupsFail(doc, nonDupF, true, "a", true);
        }
    }

    public void testSimpleDupsChars() throws Exception
    {
        JsonFactory nonDupF = new JsonFactory();
        JsonFactory dupF = new JsonFactory();
        dupF.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        for (String doc : DUP_DOCS) {
            _testSimpleDupsFail(doc, dupF, false, "a", false);
            _testSimpleDupsFail(doc, nonDupF, false, "a", true);
        }
    }
    
    private void _testSimpleDupsOk(final String doc, JsonFactory f,
            boolean useStream) throws Exception
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
            boolean useStream, String name, boolean lazily) throws Exception
    {
        JsonParser jp = useStream ?
                createParserUsingStream(f, doc, "UTF-8") : createParserUsingReader(f, doc);
        if (lazily) {
            jp.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        }
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
