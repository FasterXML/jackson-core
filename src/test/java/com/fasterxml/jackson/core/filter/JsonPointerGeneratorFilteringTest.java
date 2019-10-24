package com.fasterxml.jackson.core.filter;

import java.io.StringWriter;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.filter.FilteringGeneratorDelegate.TokenInclusion;

@SuppressWarnings("resource")
public class JsonPointerGeneratorFilteringTest extends com.fasterxml.jackson.core.BaseTest
{
    private final JsonFactory JSON_F = new JsonFactory();

    final String SIMPLE_INPUT = aposToQuotes("{'a':1,'b':[1,2,3],'c':{'d':{'a':true}},'d':null}");

    public void testSimplePropertyWithPath() throws Exception
    {
        _assert(SIMPLE_INPUT, "/c", TokenInclusion.INCLUDE_ALL_AND_PATH, "{'c':{'d':{'a':true}}}");
        _assert(SIMPLE_INPUT, "/c/d", TokenInclusion.INCLUDE_ALL_AND_PATH, "{'c':{'d':{'a':true}}}");
        _assert(SIMPLE_INPUT, "/c/d/a", TokenInclusion.INCLUDE_ALL_AND_PATH, "{'c':{'d':{'a':true}}}");

        _assert(SIMPLE_INPUT, "/c/d/a", TokenInclusion.INCLUDE_ALL_AND_PATH, "{'c':{'d':{'a':true}}}");
        
        _assert(SIMPLE_INPUT, "/a", TokenInclusion.INCLUDE_ALL_AND_PATH, "{'a':1}");
        _assert(SIMPLE_INPUT, "/d", TokenInclusion.INCLUDE_ALL_AND_PATH, "{'d':null}");

        // and then non-match
        _assert(SIMPLE_INPUT, "/x", TokenInclusion.INCLUDE_ALL_AND_PATH, "");
    }
    
    public void testSimplePropertyWithoutPath() throws Exception
    {
        _assert(SIMPLE_INPUT, "/c", TokenInclusion.ONLY_INCLUDE_ALL, "{'d':{'a':true}}");
        _assert(SIMPLE_INPUT, "/c/d", TokenInclusion.ONLY_INCLUDE_ALL, "{'a':true}");
        _assert(SIMPLE_INPUT, "/c/d/a", TokenInclusion.ONLY_INCLUDE_ALL, "true");
        
        _assert(SIMPLE_INPUT, "/a", TokenInclusion.ONLY_INCLUDE_ALL, "1");
        _assert(SIMPLE_INPUT, "/d", TokenInclusion.ONLY_INCLUDE_ALL, "null");

        // and then non-match
        _assert(SIMPLE_INPUT, "/x", TokenInclusion.ONLY_INCLUDE_ALL, "");
    }

    public void testArrayElementWithPath() throws Exception
    {
        _assert(SIMPLE_INPUT, "/b", TokenInclusion.INCLUDE_ALL_AND_PATH, "{'b':[1,2,3]}");
        _assert(SIMPLE_INPUT, "/b/1", TokenInclusion.INCLUDE_ALL_AND_PATH, "{'b':[2]}");
        _assert(SIMPLE_INPUT, "/b/2", TokenInclusion.INCLUDE_ALL_AND_PATH, "{'b':[3]}");
        
        // and then non-match
        _assert(SIMPLE_INPUT, "/b/8", TokenInclusion.INCLUDE_ALL_AND_PATH, "");
    }

    public void testArrayNestedWithPath() throws Exception
    {
        _assert("{'a':[true,{'b':3,'d':2},false]}", "/a/1/b", TokenInclusion.INCLUDE_ALL_AND_PATH, "{'a':[{'b':3}]}");
        _assert("[true,[1]]", "/0", TokenInclusion.INCLUDE_ALL_AND_PATH, "[true]");
        _assert("[true,[1]]", "/1", TokenInclusion.INCLUDE_ALL_AND_PATH, "[[1]]");
        _assert("[true,[1,2,[true],3],0]", "/0", TokenInclusion.INCLUDE_ALL_AND_PATH, "[true]");
        _assert("[true,[1,2,[true],3],0]", "/1", TokenInclusion.INCLUDE_ALL_AND_PATH, "[[1,2,[true],3]]");

        _assert("[true,[1,2,[true],3],0]", "/1/2", TokenInclusion.INCLUDE_ALL_AND_PATH, "[[[true]]]");
        _assert("[true,[1,2,[true],3],0]", "/1/2/0", TokenInclusion.INCLUDE_ALL_AND_PATH, "[[[true]]]");
        _assert("[true,[1,2,[true],3],0]", "/1/3/0", TokenInclusion.INCLUDE_ALL_AND_PATH, "");
    }

    public void testArrayNestedWithoutPath() throws Exception
    {
        _assert("{'a':[true,{'b':3,'d':2},false]}", "/a/1/b", TokenInclusion.ONLY_INCLUDE_ALL, "3");
        _assert("[true,[1,2,[true],3],0]", "/0", TokenInclusion.ONLY_INCLUDE_ALL, "true");
        _assert("[true,[1,2,[true],3],0]", "/1", TokenInclusion.ONLY_INCLUDE_ALL,
                "[1,2,[true],3]");

        _assert("[true,[1,2,[true],3],0]", "/1/2", TokenInclusion.ONLY_INCLUDE_ALL, "[true]");
        _assert("[true,[1,2,[true],3],0]", "/1/2/0", TokenInclusion.ONLY_INCLUDE_ALL, "true");
        _assert("[true,[1,2,[true],3],0]", "/1/3/0", TokenInclusion.ONLY_INCLUDE_ALL, "");
    }
    
//    final String SIMPLE_INPUT = aposToQuotes("{'a':1,'b':[1,2,3],'c':{'d':{'a':true}},'d':null}");
    
    public void testArrayElementWithoutPath() throws Exception
    {
        _assert(SIMPLE_INPUT, "/b", TokenInclusion.ONLY_INCLUDE_ALL, "[1,2,3]");
        _assert(SIMPLE_INPUT, "/b/1", TokenInclusion.ONLY_INCLUDE_ALL, "2");
        _assert(SIMPLE_INPUT, "/b/2", TokenInclusion.ONLY_INCLUDE_ALL, "3");

        _assert(SIMPLE_INPUT, "/b/8", TokenInclusion.ONLY_INCLUDE_ALL, "");

        // and then non-match
        _assert(SIMPLE_INPUT, "/x", TokenInclusion.ONLY_INCLUDE_ALL, "");
    }

    private void _assert(String input, String pathExpr, TokenInclusion tokenInclusion, String exp)
        throws Exception
    {
        StringWriter w = new StringWriter();

        JsonGenerator g0 = JSON_F.createGenerator(w);
        FilteringGeneratorDelegate g = new FilteringGeneratorDelegate(g0,
                new JsonPointerBasedFilter(pathExpr),
            tokenInclusion, false);

        try {
            writeJsonDoc(JSON_F, input, g);
        } catch (Exception e) {
            g0.flush();
            System.err.println("With input '"+input+"', output at point of failure: <"+w+">");
            throw e;
        }

        assertEquals(aposToQuotes(exp), w.toString());
    }
}
