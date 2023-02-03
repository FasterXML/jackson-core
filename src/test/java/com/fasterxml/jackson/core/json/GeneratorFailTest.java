package com.fasterxml.jackson.core.json;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;

public class GeneratorFailTest
    extends com.fasterxml.jackson.core.BaseTest
{
    private final JsonFactory F = new JsonFactory();

    // [core#167]: no error for writing field name twice
    public void testDupFieldNameWrites() throws Exception
    {
        _testDupFieldNameWrites(F, false);
        _testDupFieldNameWrites(F, true);
    }

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

    // for [core#282]
    public void testFailOnWritingFieldNameInRoot() throws Exception {
        _testFailOnWritingFieldNameInRoot(F, false);
        _testFailOnWritingFieldNameInRoot(F, true);
    }

    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */

    private void _testDupFieldNameWrites(JsonFactory f, boolean useReader) throws Exception
    {
        JsonGenerator gen;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        if (useReader) {
            gen = f.createGenerator(new OutputStreamWriter(bout, "UTF-8"));
        } else {
            gen = f.createGenerator(bout, JsonEncoding.UTF8);
        }
        gen.writeStartObject();
        gen.writeFieldName("a");

        try {
            gen.writeFieldName("b");
            gen.flush();
            String json = bout.toString("UTF-8");
            fail("Should not have let two consecutive field name writes succeed: output = "+json);
        } catch (JsonProcessingException e) {
            verifyException(e, "can not write a field name, expecting a value");
        }
        gen.close();
    }

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
            gen.writeString("a");
            gen.flush();
            String json = bout.toString("UTF-8");
            fail("Should not have let "+gen.getClass().getName()+".writeString() be used in place of 'writeFieldName()': output = "+json);
        } catch (JsonProcessingException e) {
            verifyException(e, "can not write a String");
        }
        gen.close();
    }

    // for [core#282]
    private void _testFailOnWritingFieldNameInRoot(JsonFactory f, boolean useReader) throws Exception
    {
        JsonGenerator gen;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        if (useReader) {
            gen = f.createGenerator(new OutputStreamWriter(bout, "UTF-8"));
        } else {
            gen = f.createGenerator(bout, JsonEncoding.UTF8);
        }
        try {
            gen.writeFieldName("a");
            gen.flush();
            String json = bout.toString("UTF-8");
            fail("Should not have let "+gen.getClass().getName()+".writeFieldName() be used in root context: output = "+json);
        } catch (JsonProcessingException e) {
            verifyException(e, "can not write a field name");
        }
        gen.close();
    }
}
