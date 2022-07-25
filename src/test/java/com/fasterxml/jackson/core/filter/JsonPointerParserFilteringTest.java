package com.fasterxml.jackson.core.filter;

import java.io.StringWriter;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.filter.TokenFilter.Inclusion;

public class JsonPointerParserFilteringTest extends com.fasterxml.jackson.core.BaseTest
{
    private final JsonFactory JSON_F = new JsonFactory();

    final String SIMPLEST_INPUT = a2q("{'a':1,'b':2,'c':3}");

    final String SIMPLE_INPUT = a2q("{'a':1,'b':[1,2,3],'c':{'d':{'a':true}},'d':null}");

    public void testSimplestWithPath() throws Exception
    {
        _assert(SIMPLEST_INPUT, "/a", Inclusion.INCLUDE_ALL_AND_PATH, "{'a':1}");
        _assert(SIMPLEST_INPUT, "/b", Inclusion.INCLUDE_ALL_AND_PATH, "{'b':2}");
        _assert(SIMPLEST_INPUT, "/c", Inclusion.INCLUDE_ALL_AND_PATH, "{'c':3}");
        _assert(SIMPLEST_INPUT, "/c/0", Inclusion.INCLUDE_ALL_AND_PATH, "");
        _assert(SIMPLEST_INPUT, "/d", Inclusion.INCLUDE_ALL_AND_PATH, "");
    }

    public void testSimplestNoPath() throws Exception
    {
        _assert(SIMPLEST_INPUT, "/a", Inclusion.ONLY_INCLUDE_ALL, "1");
        _assert(SIMPLEST_INPUT, "/b", Inclusion.ONLY_INCLUDE_ALL, "2");
        _assert(SIMPLEST_INPUT, "/b/2", Inclusion.ONLY_INCLUDE_ALL, "");
        _assert(SIMPLEST_INPUT, "/c", Inclusion.ONLY_INCLUDE_ALL, "3");
        _assert(SIMPLEST_INPUT, "/d", Inclusion.ONLY_INCLUDE_ALL, "");
    }

    public void testSimpleWithPath() throws Exception
    {
        _assert(SIMPLE_INPUT, "/c", Inclusion.INCLUDE_ALL_AND_PATH, "{'c':{'d':{'a':true}}}");
        _assert(SIMPLE_INPUT, "/c/d", Inclusion.INCLUDE_ALL_AND_PATH, "{'c':{'d':{'a':true}}}");
        _assert(SIMPLE_INPUT, "/a", Inclusion.INCLUDE_ALL_AND_PATH, "{'a':1}");
        _assert(SIMPLE_INPUT, "/b", Inclusion.INCLUDE_ALL_AND_PATH, "{'b':[1,2,3]}");
        _assert(SIMPLE_INPUT, "/b/0", Inclusion.INCLUDE_ALL_AND_PATH, "{'b':[1]}");
        _assert(SIMPLE_INPUT, "/b/1", Inclusion.INCLUDE_ALL_AND_PATH, "{'b':[2]}");
        _assert(SIMPLE_INPUT, "/b/2", Inclusion.INCLUDE_ALL_AND_PATH, "{'b':[3]}");
        _assert(SIMPLE_INPUT, "/b/3", Inclusion.INCLUDE_ALL_AND_PATH, "");
    }

    public void testSimpleNoPath() throws Exception
    {
        _assert(SIMPLE_INPUT, "/c", Inclusion.ONLY_INCLUDE_ALL, "{'d':{'a':true}}");

        _assert(SIMPLE_INPUT, "/c/d", Inclusion.ONLY_INCLUDE_ALL, "{'a':true}");
        _assert(SIMPLE_INPUT, "/a", Inclusion.ONLY_INCLUDE_ALL, "1");
        _assert(SIMPLE_INPUT, "/b", Inclusion.ONLY_INCLUDE_ALL, "[1,2,3]");
        _assert(SIMPLE_INPUT, "/b/0", Inclusion.ONLY_INCLUDE_ALL, "1");
        _assert(SIMPLE_INPUT, "/b/1", Inclusion.ONLY_INCLUDE_ALL, "2");
        _assert(SIMPLE_INPUT, "/b/2", Inclusion.ONLY_INCLUDE_ALL, "3");
        _assert(SIMPLE_INPUT, "/b/3", Inclusion.ONLY_INCLUDE_ALL, "");
    }

    @SuppressWarnings("resource")
    void _assert(String input, String pathExpr, TokenFilter.Inclusion inclusion, String exp)
        throws Exception
    {
        JsonParser p0 = JSON_F.createParser(input);
        FilteringParserDelegate p = new FilteringParserDelegate(p0,
                new JsonPointerBasedFilter(pathExpr),
                inclusion, false);
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

        assertEquals(a2q(exp), w.toString());
    }
}
