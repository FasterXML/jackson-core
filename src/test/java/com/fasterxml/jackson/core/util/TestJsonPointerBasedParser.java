package com.fasterxml.jackson.core.util;

import java.io.IOException;
import java.io.StringWriter;

import com.fasterxml.jackson.core.BaseTest;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonPointer;

public class TestJsonPointerBasedParser extends BaseTest {

    private final JsonFactory JSON_FACTORY = new JsonFactory();

    /**
     * Simple test for json pointer based parsing
     * 
     * @throws Exception
     */
    public void testJsonPointerBasedParser() throws Exception {
        final String pointerExpr = "/c/d/a";
        final String jsonInput = aposToQuotes(
                "{'a':1,'b':[1,2,3]," + "'c':{'d':{'g':true,'a':true},'e':{'f':true}},'h':null}");

        JsonPointer pointer = JsonPointer.compile(pointerExpr);
        _assert(pointer, jsonInput, "{'c':{'d':{'a':true},'e':{'f':true}},'h':null}");
    }

    /**
     * More complex tests involving multiple levels of json pointer
     * 
     * @throws Exception
     */
    public void testJsonPointerBasedParserAllLevelTest() throws Exception {
        String pointerExpr = "/a";
        final String jsonInput = aposToQuotes(
                "{'a':1,'b':[1,2,3]," + "'c':{'d':{'g':true,'a':true},'e':{'b':true}},'d':null}");

        JsonPointer pointer = JsonPointer.compile(pointerExpr);
        _assert(pointer, jsonInput, "{'a':1,'b':[1,2,3]," + "'c':{'d':{'g':true,'a':true},'e':{'b':true}},'d':null}");

        pointerExpr = "/b";
        pointer = JsonPointer.compile(pointerExpr);
        _assert(pointer, jsonInput, "{'b':[1,2,3]," + "'c':{'d':{'g':true,'a':true},'e':{'b':true}},'d':null}");

        pointerExpr = "/c";
        pointer = JsonPointer.compile(pointerExpr);
        _assert(pointer, jsonInput, "{'c':{'d':{'g':true,'a':true},'e':{'b':true}},'d':null}");

        pointerExpr = "/d";
        pointer = JsonPointer.compile(pointerExpr);
        _assert(pointer, jsonInput, "{'d':null}");

        pointerExpr = "/c/e";
        pointer = JsonPointer.compile(pointerExpr);
        _assert(pointer, jsonInput, "{'c':{'e':{'b':true}},'d':null}");

        pointerExpr = "/c/e/b";
        pointer = JsonPointer.compile(pointerExpr);
        _assert(pointer, jsonInput, "{'c':{'e':{'b':true}},'d':null}");

        pointerExpr = "/c/d/g";
        pointer = JsonPointer.compile(pointerExpr);
        _assert(pointer, jsonInput, "{'c':{'d':{'g':true,'a':true},'e':{'b':true}},'d':null}");

        pointerExpr = "/c/d/a";
        pointer = JsonPointer.compile(pointerExpr);
        _assert(pointer, jsonInput, "{'c':{'d':{'a':true},'e':{'b':true}},'d':null}");
    }

    /**
     * Test for array objects in json pointer based parsing
     * 
     * @throws Exception
     */
    public void testArraysInJsonPointerBasedParser() throws Exception {
        String pointerExpr = "/b/1";
        String jsonInput = aposToQuotes(
                "{'a':1,'b':[1,2,3]}");//,'c':{'d':{'g':true,'b':[1,2,3],'a':true},'e':{'b':true}},'d':null}");

        JsonPointer pointer = JsonPointer.compile(pointerExpr);
        _assert(pointer, jsonInput, "{'b':[2,3]}");
        
        pointerExpr = "/b/1";
        jsonInput = aposToQuotes(
                "{'a':1,'b':[1,2,3],'c':{'d':{'g':true,'b':[1,2,3],'a':true},'e':{'b':true}},'d':null}");

        pointer = JsonPointer.compile(pointerExpr);
        _assert(pointer, jsonInput, "{'b':[2,3],'c':{'d':{'g':true,'b':[1,2,3],'a':true},'e':{'b':true}},'d':null}");
        
        pointerExpr = "/b/1/b3";
        jsonInput = aposToQuotes(
                "{'a':1,'b':[{'b1':true,'b2':true},{'b3':true},{'b4':true}],'c':{'d':{'g':true,'b':[1,2,3],'a':true},'e':{'b':true}},'d':null}");

        pointer = JsonPointer.compile(pointerExpr);
        _assert(pointer, jsonInput, "{'b':[{'b3':true},{'b4':true}],'c':{'d':{'g':true,'b':[1,2,3],'a':true},'e':{'b':true}},'d':null}");
        
        pointerExpr = "/b/1/b3/1/b32";
        jsonInput = aposToQuotes(
                "{'a':1,'b':[{'b1':true,'b2':true},{'b3':[{'b31':true},{'b32':true},3]},{'b4':true}],'c':{'d':{'g':true,'b':[1,2,3],'a':true},'e':{'b':true}},'d':null}");

        pointer = JsonPointer.compile(pointerExpr);
        _assert(pointer, jsonInput, "{'b':[{'b3':[{'b32':true},3]},{'b4':true}],'c':{'d':{'g':true,'b':[1,2,3],'a':true},'e':{'b':true}},'d':null}");
    }

    /**
     * Test for invalid json pointer
     * 
     * @throws Exception
     */
    public void testInvalidPointer() throws Exception {
        String pointerExpr = "/invalid";
        String jsonInput = aposToQuotes(
                "{'a':1,'b':[1,2,3]," + "'c':{'d':{'g':true,'b':[1,2,3],'a':true},'e':{'b':true}},'d':null}");

        JsonPointer pointer = JsonPointer.compile(pointerExpr);
        _assert(pointer, jsonInput, "{}");

        pointerExpr = "/c/invalid";
        pointer = JsonPointer.compile(pointerExpr);
        _assert(pointer, jsonInput, "{'c':{}}");

        pointerExpr = "/c/d/invalid";
        pointer = JsonPointer.compile(pointerExpr);
        _assert(pointer, jsonInput, "{'c':{'d':{}}}");
        
        pointerExpr = "/b/1/b3/1/b321";
        jsonInput = aposToQuotes(
                "{'a':1,'b':[{'b1':true,'b2':true},{'b3':[{'b31':true},{'b32':true}]}],'c':{'d':{'g':true,'b':[1,2,3],'a':true},'e':{'b':true}},'d':null}");

        pointer = JsonPointer.compile(pointerExpr);
        _assert(pointer, jsonInput, "{'b':[{'b3':[{}]}]}");
    }

    private void _assert(JsonPointer pointer, final String jsonInput, final String expectedOutput)
            throws IOException, JsonParseException {
        JsonParser parser = JSON_FACTORY.createParser(jsonInput);
        JsonPointerBasedParser pointerParser = new JsonPointerBasedParser(parser, pointer);

        StringWriter writer = new StringWriter();
        JsonGenerator generator = JSON_FACTORY.createGenerator(writer);
        while (pointerParser.nextToken() != null) {
            generator.copyCurrentEvent(pointerParser);
        }
        pointerParser.close();
        generator.close();

        assertEquals(aposToQuotes(expectedOutput), writer.toString());
    }

}
