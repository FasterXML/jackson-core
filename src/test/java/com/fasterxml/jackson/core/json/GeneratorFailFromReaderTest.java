package com.fasterxml.jackson.core.json;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;

public class GeneratorFailFromReaderTest
    extends com.fasterxml.jackson.core.BaseTest
{
    private final JsonFactory F = new JsonFactory();

    // [core#177]
    // Also: should not try writing JSON String if field name expected
    // (in future maybe take one as alias... but not yet)
    public void testFailOnWritingStringNotFieldNameBytes() throws Exception {
        _testFailOnWritingStringNotFieldName(F, false);
    }

    // [core#177]
    public void testFailOnWritingStringNotFieldNameChars() throws Exception {
        _testFailOnWritingStringNotFieldName(F, true);
    }

    public void testFailOnWritingStringFromReaderWithTooFewCharacters() throws Exception {
        _testFailOnWritingStringFromReaderWithTooFewCharacters(F, true);
        _testFailOnWritingStringFromReaderWithTooFewCharacters(F, false);
    }

    public void testFailOnWritingStringFromNullReader() throws Exception {
        _testFailOnWritingStringFromNullReader(F, true);
        _testFailOnWritingStringFromNullReader(F, false);
    }

    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */


    private void _testFailOnWritingStringNotFieldName(JsonFactory f, boolean useReader) throws Exception
    {
        JsonGenerator gen;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        if (useReader) {
            gen = f.createGenerator(new OutputStreamWriter(bout, "UTF-8"));
        } else {
            gen = f.createGenerator(bout, JsonEncoding.UTF8);
        }
        gen.writeStartObject();

        try {
            StringReader reader = new StringReader("a");
            gen.writeString(reader, -1);
            gen.flush();
            String json = bout.toString("UTF-8");
            fail("Should not have let "+gen.getClass().getName()+".writeString() be used in place of 'writeFieldName()': output = "+json);
        } catch (JsonProcessingException e) {
            verifyException(e, "can not write a String");
        }
        gen.close();
    }

    private void _testFailOnWritingStringFromReaderWithTooFewCharacters(JsonFactory f, boolean useReader) throws Exception{
        JsonGenerator gen;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        if (useReader) {
            gen = f.createGenerator(new OutputStreamWriter(bout, "UTF-8"));
        } else {
            gen = f.createGenerator(bout, JsonEncoding.UTF8);
        }
        gen.writeStartObject();

        try {
            String testStr = "aaaaaaaaa";
            StringReader reader = new StringReader(testStr);
            gen.writeFieldName("a");
            gen.writeString(reader, testStr.length() + 1);
            gen.flush();
            String json = bout.toString("UTF-8");
            fail("Should not have let "+gen.getClass().getName()+".writeString() ': output = "+json);
        } catch (JsonProcessingException e) {
            verifyException(e, "Didn't read enough from reader");
        }
        gen.close();
    }

    private void _testFailOnWritingStringFromNullReader(JsonFactory f, boolean useReader) throws Exception{
        JsonGenerator gen;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        if (useReader) {
            gen = f.createGenerator(new OutputStreamWriter(bout, "UTF-8"));
        } else {
            gen = f.createGenerator(bout, JsonEncoding.UTF8);
        }
        gen.writeStartObject();

        try {
            gen.writeFieldName("a");
            gen.writeString(null, -1);
            gen.flush();
            String json = bout.toString("UTF-8");
            fail("Should not have let "+gen.getClass().getName()+".writeString() ': output = "+json);
        } catch (JsonProcessingException e) {
            verifyException(e, "null reader");
        }
        gen.close();
    }
}
