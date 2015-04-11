package com.fasterxml.jackson.core.filter;

import java.io.*;

import com.fasterxml.jackson.core.*;

@SuppressWarnings("resource")
public class JsonPointerGeneratorFilteringTest extends com.fasterxml.jackson.core.BaseTest
{
    private final JsonFactory JSON_F = new JsonFactory();

    final String SIMPLE_INPUT = aposToQuotes("{'a':1,'b':[1,2,3],'c':{'d':{'a':true}},'d':null}");

    public void testSimplePropertyWithPath() throws Exception
    {
        _assert(SIMPLE_INPUT, "/c", true, "{'c':{'d':{'a':true}}}");
        _assert(SIMPLE_INPUT, "/c/d", true, "{'c':{'d':{'a':true}}}");
        _assert(SIMPLE_INPUT, "/c/d/a", true, "{'c':{'d':{'a':true}}}");

        _assert(SIMPLE_INPUT, "/c/d/a", true, "{'c':{'d':{'a':true}}}");
        
        _assert(SIMPLE_INPUT, "/a", true, "{'a':1}");
        _assert(SIMPLE_INPUT, "/d", true, "{'d':null}");

        // and then non-match
        _assert(SIMPLE_INPUT, "/x", true, "");
    }
    
    public void testSimplePropertyWithoutPath() throws Exception
    {
        _assert(SIMPLE_INPUT, "/c", false, "{'d':{'a':true}}");
        _assert(SIMPLE_INPUT, "/c/d", false, "{'a':true}");
        _assert(SIMPLE_INPUT, "/c/d/a", false, "true");
        
        _assert(SIMPLE_INPUT, "/a", false, "1");
        _assert(SIMPLE_INPUT, "/d", false, "null");

        // and then non-match
        _assert(SIMPLE_INPUT, "/x", false, "");
    }

    public void testArrayElementWithPath() throws Exception
    {
        _assert(SIMPLE_INPUT, "/b", true, "{'b':[1,2,3]}");
        _assert(SIMPLE_INPUT, "/b/1", true, "{'b':[2]}");
        _assert(SIMPLE_INPUT, "/b/2", true, "{'b':[3]}");
        
        // and then non-match
        _assert(SIMPLE_INPUT, "/b/8", true, "");
    }

    public void testArrayNestedWithPath() throws Exception
    {
        _assert("{'a':[true,{'b':3,'d':2},false]}", "/a/1/b", true, "{'a':[{'b':3}]}");
    }
    
    public void testArrayElementWithoutPath() throws Exception
    {
        _assert(SIMPLE_INPUT, "/b", false, "[1,2,3]");
        _assert(SIMPLE_INPUT, "/b/1", false, "2");
        _assert(SIMPLE_INPUT, "/b/2", false, "3");

        _assert(SIMPLE_INPUT, "/b/8", false, "");

        // and then non-match
        _assert(SIMPLE_INPUT, "/x", false, "");
    }
    
    private void _assert(String input, String pathExpr, boolean includeParent, String exp)
        throws Exception
    {
        StringWriter w = new StringWriter();
        JsonGenerator g0 = JSON_F.createGenerator(w);
        FilteringGeneratorDelegate g = new FilteringGeneratorDelegate(g0,
                new JsonPointerBasedFilter(pathExpr, includeParent),
                includeParent, false);
        writeJsonDoc(JSON_F, input, g);

        assertEquals(aposToQuotes(exp), w.toString());
    }
}
