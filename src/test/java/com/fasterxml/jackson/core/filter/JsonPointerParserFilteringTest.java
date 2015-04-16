package com.fasterxml.jackson.core.filter;

import java.io.StringWriter;

import com.fasterxml.jackson.core.*;

public class JsonPointerParserFilteringTest extends com.fasterxml.jackson.core.BaseTest
{
    private final JsonFactory JSON_F = new JsonFactory();

    final String SIMPLEST_INPUT = aposToQuotes("{'a':1,'b':2,'c':3}");

    final String SIMPLE_INPUT = aposToQuotes("{'a':1,'b':[1,2,3],'c':{'d':{'a':true}},'d':null}");

    public void testSimplestWithPath() throws Exception
    {
        _assert(SIMPLEST_INPUT, "/b", true, "{'b':2}");
    }

    public void testSimplestNoPath() throws Exception
    {
        _assert(SIMPLEST_INPUT, "/b", false, "2");
    }

    @SuppressWarnings("resource")
    void _assert(String input, String pathExpr, boolean includeParent, String exp)
        throws Exception
    {
        JsonParser p0 = JSON_F.createParser(input);
        FilteringParserDelegate p = new FilteringParserDelegate(p0,
                new JsonPointerBasedFilter(pathExpr, includeParent),
                includeParent, false);
        StringWriter w = new StringWriter();
        JsonGenerator g = JSON_F.createGenerator(w);

        try {
            while (p.nextToken() != null) {
                g.copyCurrentEvent(p);
            }
            p.close();
            g.close();
        } catch (Exception e) {
            g.flush();
            System.err.println("With input '"+input+"', output at point of failure: <"+w+">");
            throw e;
        }

        assertEquals(aposToQuotes(exp), w.toString());
    }
}
