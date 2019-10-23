package com.fasterxml.jackson.core.filter;

import java.io.StringWriter;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.filter.FilteringGeneratorDelegate.PathWriteMode;

@SuppressWarnings("resource")
public class JsonPointerGeneratorFilteringTest extends com.fasterxml.jackson.core.BaseTest
{
    private final JsonFactory JSON_F = new JsonFactory();

    final String SIMPLE_INPUT = aposToQuotes("{'a':1,'b':[1,2,3],'c':{'d':{'a':true}},'d':null}");

    public void testSimplePropertyWithPath() throws Exception
    {
        _assert(SIMPLE_INPUT, "/c", PathWriteMode.LAZY, "{'c':{'d':{'a':true}}}");
        _assert(SIMPLE_INPUT, "/c/d", PathWriteMode.LAZY, "{'c':{'d':{'a':true}}}");
        _assert(SIMPLE_INPUT, "/c/d/a", PathWriteMode.LAZY, "{'c':{'d':{'a':true}}}");

        _assert(SIMPLE_INPUT, "/c/d/a", PathWriteMode.LAZY, "{'c':{'d':{'a':true}}}");
        
        _assert(SIMPLE_INPUT, "/a", PathWriteMode.LAZY, "{'a':1}");
        _assert(SIMPLE_INPUT, "/d", PathWriteMode.LAZY, "{'d':null}");

        // and then non-match
        _assert(SIMPLE_INPUT, "/x", PathWriteMode.LAZY, "");
    }
    
    public void testSimplePropertyWithoutPath() throws Exception
    {
        _assert(SIMPLE_INPUT, "/c", PathWriteMode.NONE, "{'d':{'a':true}}");
        _assert(SIMPLE_INPUT, "/c/d", PathWriteMode.NONE, "{'a':true}");
        _assert(SIMPLE_INPUT, "/c/d/a", PathWriteMode.NONE, "true");
        
        _assert(SIMPLE_INPUT, "/a", PathWriteMode.NONE, "1");
        _assert(SIMPLE_INPUT, "/d", PathWriteMode.NONE, "null");

        // and then non-match
        _assert(SIMPLE_INPUT, "/x", PathWriteMode.NONE, "");
    }

    public void testArrayElementWithPath() throws Exception
    {
        _assert(SIMPLE_INPUT, "/b", PathWriteMode.LAZY, "{'b':[1,2,3]}");
        _assert(SIMPLE_INPUT, "/b/1", PathWriteMode.LAZY, "{'b':[2]}");
        _assert(SIMPLE_INPUT, "/b/2", PathWriteMode.LAZY, "{'b':[3]}");
        
        // and then non-match
        _assert(SIMPLE_INPUT, "/b/8", PathWriteMode.LAZY, "");
    }

    public void testArrayNestedWithPath() throws Exception
    {
        _assert("{'a':[true,{'b':3,'d':2},false]}", "/a/1/b", PathWriteMode.LAZY, "{'a':[{'b':3}]}");
        _assert("[true,[1]]", "/0", PathWriteMode.LAZY, "[true]");
        _assert("[true,[1]]", "/1", PathWriteMode.LAZY, "[[1]]");
        _assert("[true,[1,2,[true],3],0]", "/0", PathWriteMode.LAZY, "[true]");
        _assert("[true,[1,2,[true],3],0]", "/1", PathWriteMode.LAZY, "[[1,2,[true],3]]");

        _assert("[true,[1,2,[true],3],0]", "/1/2", PathWriteMode.LAZY, "[[[true]]]");
        _assert("[true,[1,2,[true],3],0]", "/1/2/0", PathWriteMode.LAZY, "[[[true]]]");
        _assert("[true,[1,2,[true],3],0]", "/1/3/0", PathWriteMode.LAZY, "");
    }

    public void testArrayNestedWithoutPath() throws Exception
    {
        _assert("{'a':[true,{'b':3,'d':2},false]}", "/a/1/b", PathWriteMode.NONE, "3");
        _assert("[true,[1,2,[true],3],0]", "/0", PathWriteMode.NONE, "true");
        _assert("[true,[1,2,[true],3],0]", "/1", PathWriteMode.NONE,
                "[1,2,[true],3]");

        _assert("[true,[1,2,[true],3],0]", "/1/2", PathWriteMode.NONE, "[true]");
        _assert("[true,[1,2,[true],3],0]", "/1/2/0", PathWriteMode.NONE, "true");
        _assert("[true,[1,2,[true],3],0]", "/1/3/0", PathWriteMode.NONE, "");
    }
    
//    final String SIMPLE_INPUT = aposToQuotes("{'a':1,'b':[1,2,3],'c':{'d':{'a':true}},'d':null}");
    
    public void testArrayElementWithoutPath() throws Exception
    {
        _assert(SIMPLE_INPUT, "/b", PathWriteMode.NONE, "[1,2,3]");
        _assert(SIMPLE_INPUT, "/b/1", PathWriteMode.NONE, "2");
        _assert(SIMPLE_INPUT, "/b/2", PathWriteMode.NONE, "3");

        _assert(SIMPLE_INPUT, "/b/8", PathWriteMode.NONE, "");

        // and then non-match
        _assert(SIMPLE_INPUT, "/x", PathWriteMode.NONE, "");
    }

    private void _assert(String input, String pathExpr, PathWriteMode pathWriteMode, String exp)
        throws Exception
    {
        StringWriter w = new StringWriter();

        JsonGenerator g0 = JSON_F.createGenerator(w);
        FilteringGeneratorDelegate g = new FilteringGeneratorDelegate(g0,
                new JsonPointerBasedFilter(pathExpr),
                pathWriteMode, false);

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
