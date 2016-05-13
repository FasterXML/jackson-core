package com.fasterxml.jackson.failing;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;

public class GeneratorFail282Test
    extends com.fasterxml.jackson.core.BaseTest
{
    private final JsonFactory F = new JsonFactory();

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
