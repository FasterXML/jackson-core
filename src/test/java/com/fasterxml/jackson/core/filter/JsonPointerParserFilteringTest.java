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
        _assert(SIMPLEST_INPUT, "/a", true, "{'a':1}");
        _assert(SIMPLEST_INPUT, "/b", true, "{'b':2}");
        _assert(SIMPLEST_INPUT, "/c", true, "{'c':3}");
        _assert(SIMPLEST_INPUT, "/c/0", true, "");
        _assert(SIMPLEST_INPUT, "/d", true, "");
    }

    public void testSimplestNoPath() throws Exception
    {
        _assert(SIMPLEST_INPUT, "/a", false, "1");
        _assert(SIMPLEST_INPUT, "/b", false, "2");
        _assert(SIMPLEST_INPUT, "/b/2", false, "");
        _assert(SIMPLEST_INPUT, "/c", false, "3");
        _assert(SIMPLEST_INPUT, "/d", false, "");
    }

    public void testSimpleWithPath() throws Exception
    {
        _assert(SIMPLE_INPUT, "/c", true, "{'c':{'d':{'a':true}}}");
        _assert(SIMPLE_INPUT, "/c/d", true, "{'c':{'d':{'a':true}}}");
        _assert(SIMPLE_INPUT, "/a", true, "{'a':1}");
        _assert(SIMPLE_INPUT, "/b", true, "{'b':[1,2,3]}");
        _assert(SIMPLE_INPUT, "/b/0", true, "{'b':[1]}");
        _assert(SIMPLE_INPUT, "/b/1", true, "{'b':[2]}");
        _assert(SIMPLE_INPUT, "/b/2", true, "{'b':[3]}");
        _assert(SIMPLE_INPUT, "/b/3", true, "");
    }

    public void testSimpleNoPath() throws Exception
    {
        _assert(SIMPLE_INPUT, "/c", false, "{'d':{'a':true}}");

        _assert(SIMPLE_INPUT, "/c/d", false, "{'a':true}");
        _assert(SIMPLE_INPUT, "/a", false, "1");
        _assert(SIMPLE_INPUT, "/b", false, "[1,2,3]");
        _assert(SIMPLE_INPUT, "/b/0", false, "1");
        _assert(SIMPLE_INPUT, "/b/1", false, "2");
        _assert(SIMPLE_INPUT, "/b/2", false, "3");
        _assert(SIMPLE_INPUT, "/b/3", false, "");
    }

    @SuppressWarnings("resource")
    void _assert(String input, String pathExpr, boolean includeParent, String exp)
        throws Exception
    {
        JsonParser p0 = JSON_F.createParser(input);
        FilteringParserDelegate p = new FilteringParserDelegate(p0,
                new JsonPointerBasedFilter(pathExpr),
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
