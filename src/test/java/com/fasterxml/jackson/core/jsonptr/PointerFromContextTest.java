package com.fasterxml.jackson.core.jsonptr;

import java.io.StringWriter;

import com.fasterxml.jackson.core.BaseTest;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonToken;

public class PointerFromContextTest extends BaseTest
{
    /*
    /**********************************************************
    /* Test methods, basic
    /**********************************************************
     */

    private final JsonFactory JSON_F = new JsonFactory();

    private final JsonPointer EMPTY_PTR = JsonPointer.empty();

    public void testViaParser() throws Exception
    {
        final String SIMPLE = a2q("{'a':123,'array':[1,2,[3],5,{'obInArray':4}],"
                +"'ob':{'first':[false,true],'second':{'sub':37}},'b':true}");
        JsonParser p = JSON_F.createParser(SIMPLE);

        // by default should just get "empty"
        assertSame(EMPTY_PTR, p.getParsingContext().pathAsPointer());

        // let's just traverse, then:
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertSame(EMPTY_PTR, p.getParsingContext().pathAsPointer());

        assertToken(JsonToken.FIELD_NAME, p.nextToken()); // a
        assertEquals("/a", p.getParsingContext().pathAsPointer().toString());

        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals("/a", p.getParsingContext().pathAsPointer().toString());

        assertToken(JsonToken.FIELD_NAME, p.nextToken()); // array
        assertEquals("/array", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertEquals("/array", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken()); // 1
        assertEquals("/array/0", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken()); // 2
        assertEquals("/array/1", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertEquals("/array/2", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken()); // 3
        assertEquals("/array/2/0", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertEquals("/array/2", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken()); // 5
        assertEquals("/array/3", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertEquals("/array/4", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.FIELD_NAME, p.nextToken()); // obInArray
        assertEquals("/array/4/obInArray", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken()); // 4
        assertEquals("/array/4/obInArray", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertEquals("/array/4", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.END_ARRAY, p.nextToken()); // /array
        assertEquals("/array", p.getParsingContext().pathAsPointer().toString());

        assertToken(JsonToken.FIELD_NAME, p.nextToken()); // ob
        assertEquals("/ob", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertEquals("/ob", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.FIELD_NAME, p.nextToken()); // first
        assertEquals("/ob/first", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertEquals("/ob/first", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.VALUE_FALSE, p.nextToken());
        assertEquals("/ob/first/0", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertEquals("/ob/first/1", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertEquals("/ob/first", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.FIELD_NAME, p.nextToken()); // second
        assertEquals("/ob/second", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertEquals("/ob/second", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.FIELD_NAME, p.nextToken()); // sub
        assertEquals("/ob/second/sub", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken()); // 37
        assertEquals("/ob/second/sub", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertEquals("/ob/second", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.END_OBJECT, p.nextToken()); // /ob
        assertEquals("/ob", p.getParsingContext().pathAsPointer().toString());

        assertToken(JsonToken.FIELD_NAME, p.nextToken()); // b
        assertEquals("/b", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertEquals("/b", p.getParsingContext().pathAsPointer().toString());

        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertSame(EMPTY_PTR, p.getParsingContext().pathAsPointer());

        assertNull(p.nextToken());
        p.close();
    }

    public void testViaGenerator() throws Exception
    {
        StringWriter w = new StringWriter();
        JsonGenerator g = JSON_F.createGenerator(w);
        assertSame(EMPTY_PTR, g.getOutputContext().pathAsPointer());

        g.writeStartArray();
        // no path yet
        assertSame(EMPTY_PTR, g.getOutputContext().pathAsPointer());
        g.writeBoolean(true);
        assertEquals("/0", g.getOutputContext().pathAsPointer().toString());

        g.writeStartObject();
        assertEquals("/1", g.getOutputContext().pathAsPointer().toString());
        g.writeFieldName("x");
        assertEquals("/1/x", g.getOutputContext().pathAsPointer().toString());
        g.writeString("foo");
        assertEquals("/1/x", g.getOutputContext().pathAsPointer().toString());
        g.writeFieldName("stats");
        assertEquals("/1/stats", g.getOutputContext().pathAsPointer().toString());
        g.writeStartObject();
        assertEquals("/1/stats", g.getOutputContext().pathAsPointer().toString());
        g.writeFieldName("rate");
        assertEquals("/1/stats/rate", g.getOutputContext().pathAsPointer().toString());
        g.writeNumber(13);
        assertEquals("/1/stats/rate", g.getOutputContext().pathAsPointer().toString());
        g.writeEndObject();
        assertEquals("/1/stats", g.getOutputContext().pathAsPointer().toString());

        g.writeEndObject();
        assertEquals("/1", g.getOutputContext().pathAsPointer().toString());

        g.writeEndArray();
        assertSame(EMPTY_PTR, g.getOutputContext().pathAsPointer());
        g.close();
        w.close();
    }

    /*
    /**********************************************************
    /* Test methods, root-offset
    /**********************************************************
     */

    public void testParserWithRoot() throws Exception
    {
        final String JSON = a2q("{'a':1,'b':3}\n"
                +"{'a':5,'c':[1,2]}\n[1,2]\n");
        JsonParser p = JSON_F.createParser(JSON);
        // before pointing to anything, we have no path to point to
        assertSame(EMPTY_PTR, p.getParsingContext().pathAsPointer(true));

        // but immediately after advancing we do
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertEquals("/0", p.getParsingContext().pathAsPointer(true).toString());
        assertToken(JsonToken.FIELD_NAME, p.nextToken()); // a
        assertEquals("/0/a", p.getParsingContext().pathAsPointer(true).toString());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken()); // a:1
        assertEquals("/0/a", p.getParsingContext().pathAsPointer(true).toString());
        assertToken(JsonToken.FIELD_NAME, p.nextToken()); // b
        assertEquals("/0/b", p.getParsingContext().pathAsPointer(true).toString());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken()); // a:1
        assertEquals("/0/b", p.getParsingContext().pathAsPointer(true).toString());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertEquals("/0", p.getParsingContext().pathAsPointer(true).toString());

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertEquals("/1", p.getParsingContext().pathAsPointer(true).toString());
        assertToken(JsonToken.FIELD_NAME, p.nextToken()); // a
        assertEquals("/1/a", p.getParsingContext().pathAsPointer(true).toString());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken()); // a:1
        assertEquals("/1/a", p.getParsingContext().pathAsPointer(true).toString());
        assertToken(JsonToken.FIELD_NAME, p.nextToken()); // c
        assertEquals("/1/c", p.getParsingContext().pathAsPointer(true).toString());

        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertEquals("/1/c", p.getParsingContext().pathAsPointer(true).toString());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals("/1/c/0", p.getParsingContext().pathAsPointer(true).toString());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals("/1/c/1", p.getParsingContext().pathAsPointer(true).toString());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertEquals("/1/c", p.getParsingContext().pathAsPointer(true).toString());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertEquals("/1", p.getParsingContext().pathAsPointer(true).toString());

        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertEquals("/2", p.getParsingContext().pathAsPointer(true).toString());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals("/2/0", p.getParsingContext().pathAsPointer(true).toString());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals("/2/1", p.getParsingContext().pathAsPointer(true).toString());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertEquals("/2", p.getParsingContext().pathAsPointer(true).toString());

        assertNull(p.nextToken());

        // 21-Mar-2017, tatu: This is not entirely satisfactory: ideally should get
        //   EMPTY here as well. But context doesn't really get reset at the end
        //   and it's not 100% clear what is the best path forward. So, for now...
        //   just verify current sub-optimal behavior

        assertEquals("/2", p.getParsingContext().pathAsPointer(true).toString());

        p.close();
    }

    public void testGeneratorWithRoot() throws Exception
    {
        StringWriter w = new StringWriter();
        JsonGenerator g = JSON_F.createGenerator(w);
        assertSame(EMPTY_PTR, g.getOutputContext().pathAsPointer(true));

        g.writeStartArray();
        assertEquals("/0", g.getOutputContext().pathAsPointer(true).toString());
        g.writeBoolean(true);
        assertEquals("/0/0", g.getOutputContext().pathAsPointer(true).toString());

        g.writeStartObject();
        assertEquals("/0/1", g.getOutputContext().pathAsPointer(true).toString());
        g.writeFieldName("x");
        assertEquals("/0/1/x", g.getOutputContext().pathAsPointer(true).toString());
        g.writeString("foo");
        assertEquals("/0/1/x", g.getOutputContext().pathAsPointer(true).toString());
        g.writeEndObject();
        assertEquals("/0/1", g.getOutputContext().pathAsPointer(true).toString());
        g.writeEndArray();
        assertEquals("/0", g.getOutputContext().pathAsPointer(true).toString());

        g.writeBoolean(true);
        assertEquals("/1", g.getOutputContext().pathAsPointer(true).toString());

        g.writeStartArray();
        assertEquals("/2", g.getOutputContext().pathAsPointer(true).toString());
        g.writeString("foo");
        assertEquals("/2/0", g.getOutputContext().pathAsPointer(true).toString());
        g.writeString("bar");
        assertEquals("/2/1", g.getOutputContext().pathAsPointer(true).toString());
        g.writeEndArray();
        assertEquals("/2", g.getOutputContext().pathAsPointer(true).toString());

        // as earlier, not optimal result, but verify it's stable:
        assertEquals("/2", g.getOutputContext().pathAsPointer(true).toString());

        g.close();
    }
}
