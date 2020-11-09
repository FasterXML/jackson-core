package com.fasterxml.jackson.core.filter;

import java.io.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.filter.TokenFilter.Inclusion;

@SuppressWarnings("resource")
public class JsonPointerGeneratorFilteringTest extends com.fasterxml.jackson.core.BaseTest
{
    private final JsonFactory JSON_F = new JsonFactory();

    final String SIMPLE_INPUT = aposToQuotes("{'a':1,'b':[1,2,3],'c':{'d':{'a':true}},'d':null}");

    public void testSimplePropertyWithPath() throws Exception
    {
        _assert(SIMPLE_INPUT, "/c", Inclusion.INCLUDE_ALL_AND_PATH, "{'c':{'d':{'a':true}}}");
        _assert(SIMPLE_INPUT, "/c/d", Inclusion.INCLUDE_ALL_AND_PATH, "{'c':{'d':{'a':true}}}");
        _assert(SIMPLE_INPUT, "/c/d/a", Inclusion.INCLUDE_ALL_AND_PATH, "{'c':{'d':{'a':true}}}");

        _assert(SIMPLE_INPUT, "/c/d/a", Inclusion.INCLUDE_ALL_AND_PATH, "{'c':{'d':{'a':true}}}");
        
        _assert(SIMPLE_INPUT, "/a", Inclusion.INCLUDE_ALL_AND_PATH, "{'a':1}");
        _assert(SIMPLE_INPUT, "/d", Inclusion.INCLUDE_ALL_AND_PATH, "{'d':null}");

        // and then non-match
        _assert(SIMPLE_INPUT, "/x", Inclusion.INCLUDE_ALL_AND_PATH, "");
    }
    
    public void testSimplePropertyWithoutPath() throws Exception
    {
        _assert(SIMPLE_INPUT, "/c", Inclusion.ONLY_INCLUDE_ALL, "{'d':{'a':true}}");
        _assert(SIMPLE_INPUT, "/c/d", Inclusion.ONLY_INCLUDE_ALL, "{'a':true}");
        _assert(SIMPLE_INPUT, "/c/d/a", Inclusion.ONLY_INCLUDE_ALL, "true");
        
        _assert(SIMPLE_INPUT, "/a", Inclusion.ONLY_INCLUDE_ALL, "1");
        _assert(SIMPLE_INPUT, "/d", Inclusion.ONLY_INCLUDE_ALL, "null");

        // and then non-match
        _assert(SIMPLE_INPUT, "/x", Inclusion.ONLY_INCLUDE_ALL, "");
    }

    public void testArrayElementWithPath() throws Exception
    {
        _assert(SIMPLE_INPUT, "/b", Inclusion.INCLUDE_ALL_AND_PATH, "{'b':[1,2,3]}");
        _assert(SIMPLE_INPUT, "/b/1", Inclusion.INCLUDE_ALL_AND_PATH, "{'b':[2]}");
        _assert(SIMPLE_INPUT, "/b/2", Inclusion.INCLUDE_ALL_AND_PATH, "{'b':[3]}");
        
        // and then non-match
        _assert(SIMPLE_INPUT, "/b/8", Inclusion.INCLUDE_ALL_AND_PATH, "");
    }

    public void testArrayNestedWithPath() throws Exception
    {
        _assert("{'a':[true,{'b':3,'d':2},false]}", "/a/1/b", Inclusion.INCLUDE_ALL_AND_PATH, "{'a':[{'b':3}]}");
        _assert("[true,[1]]", "/0", Inclusion.INCLUDE_ALL_AND_PATH, "[true]");
        _assert("[true,[1]]", "/1", Inclusion.INCLUDE_ALL_AND_PATH, "[[1]]");
        _assert("[true,[1,2,[true],3],0]", "/0", Inclusion.INCLUDE_ALL_AND_PATH, "[true]");
        _assert("[true,[1,2,[true],3],0]", "/1", Inclusion.INCLUDE_ALL_AND_PATH, "[[1,2,[true],3]]");

        _assert("[true,[1,2,[true],3],0]", "/1/2", Inclusion.INCLUDE_ALL_AND_PATH, "[[[true]]]");
        _assert("[true,[1,2,[true],3],0]", "/1/2/0", Inclusion.INCLUDE_ALL_AND_PATH, "[[[true]]]");
        _assert("[true,[1,2,[true],3],0]", "/1/3/0", Inclusion.INCLUDE_ALL_AND_PATH, "");
    }

    public void testArrayNestedWithoutPath() throws Exception
    {
        _assert("{'a':[true,{'b':3,'d':2},false]}", "/a/1/b", Inclusion.ONLY_INCLUDE_ALL, "3");
        _assert("[true,[1,2,[true],3],0]", "/0", Inclusion.ONLY_INCLUDE_ALL, "true");
        _assert("[true,[1,2,[true],3],0]", "/1", Inclusion.ONLY_INCLUDE_ALL,
                "[1,2,[true],3]");

        _assert("[true,[1,2,[true],3],0]", "/1/2", Inclusion.ONLY_INCLUDE_ALL, "[true]");
        _assert("[true,[1,2,[true],3],0]", "/1/2/0", Inclusion.ONLY_INCLUDE_ALL, "true");
        _assert("[true,[1,2,[true],3],0]", "/1/3/0", Inclusion.ONLY_INCLUDE_ALL, "");
    }
    
//    final String SIMPLE_INPUT = aposToQuotes("{'a':1,'b':[1,2,3],'c':{'d':{'a':true}},'d':null}");
    
    public void testArrayElementWithoutPath() throws Exception
    {
        _assert(SIMPLE_INPUT, "/b", Inclusion.ONLY_INCLUDE_ALL, "[1,2,3]");
        _assert(SIMPLE_INPUT, "/b/1", Inclusion.ONLY_INCLUDE_ALL, "2");
        _assert(SIMPLE_INPUT, "/b/2", Inclusion.ONLY_INCLUDE_ALL, "3");

        _assert(SIMPLE_INPUT, "/b/8", Inclusion.ONLY_INCLUDE_ALL, "");

        // and then non-match
        _assert(SIMPLE_INPUT, "/x", Inclusion.ONLY_INCLUDE_ALL, "");
    }

    private void _assert(String input, String pathExpr, Inclusion tokenFilterInclusion, String exp)
        throws Exception
    {
        StringWriter w = new StringWriter();

        JsonGenerator g0 = JSON_F.createGenerator(w);
        FilteringGeneratorDelegate g = new FilteringGeneratorDelegate(g0,
                new JsonPointerBasedFilter(pathExpr),
                tokenFilterInclusion, false);

        try {
            writeJsonDoc(JSON_F, input, g);
        } catch (Exception e) {
            g0.flush();
            System.err.println("With input '"+input+"', output at point of failure: <"+w+">");
            throw e;
        }

        assertEquals(aposToQuotes(exp), w.toString());
    }


    // for [core#582]: regression wrt array filtering

    public void testArrayFiltering582WithoutObject() throws IOException {
        _testArrayFiltering582(0);
    }

    public void testArrayFiltering582WithoutSize() throws IOException {
        _testArrayFiltering582(1);
    }

    public void testArrayFiltering582WithSize() throws IOException {
        _testArrayFiltering582(2);
    }

    private void _testArrayFiltering582(int mode) throws IOException
    {
         StringWriter output = new StringWriter();
         JsonGenerator jg = JSON_F.createGenerator(output);

         FilteringGeneratorDelegate gen = new FilteringGeneratorDelegate(jg,
                 new JsonPointerBasedFilter("/noMatch"), Inclusion.INCLUDE_ALL_AND_PATH, true);
         final String[] stuff = new String[] { "foo", "bar" };

         switch (mode) {
         case 0:
             gen.writeStartArray();
             break;
         case 1:
             gen.writeStartArray(stuff);
             break;
         default:
             gen.writeStartArray(stuff, stuff.length);
         }
         gen.writeString(stuff[0]);
         gen.writeString(stuff[1]);
         gen.writeEndArray();
         gen.close();
         jg.close();

         assertEquals("", output.toString());
    }
}
