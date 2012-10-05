package com.fasterxml.jackson.core;

import java.io.*;

import com.fasterxml.jackson.test.BaseTest;

/**
 * Unit tests for [Issue#31] (https://github.com/FasterXML/jackson-core/issues/31)
 */
public class TestSerializability extends BaseTest
{
    public void testJsonFactorySerializable() throws Exception
    {
        JsonFactory f = new JsonFactory();
        String origJson = "{\"simple\":[1,true,{}]}";
        assertEquals(origJson, _copyJson(f, origJson, false));

        // Ok: freeze dry factory, thaw, and try to use again:
        byte[] frozen = jdkSerialize(f);
        JsonFactory f2 = jdkDeserialize(frozen);
        assertNotNull(f2);
        assertEquals(origJson, _copyJson(f2, origJson, false));

        // Let's also try byte-based variant, for fun...
        assertEquals(origJson, _copyJson(f2, origJson, true));
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */
    
    protected byte[] jdkSerialize(Object o) throws IOException
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(1000);
        ObjectOutputStream obOut = new ObjectOutputStream(bytes);
        obOut.writeObject(o);
        obOut.close();
        return bytes.toByteArray();
    }

    @SuppressWarnings("unchecked")
    protected <T> T jdkDeserialize(byte[] raw) throws IOException
    {
        ObjectInputStream objIn = new ObjectInputStream(new ByteArrayInputStream(raw));
        try {
            return (T) objIn.readObject();
        } catch (ClassNotFoundException e) {
            fail("Missing class: "+e.getMessage());
            return null;
        } finally {
            objIn.close();
        }
    }
    
    protected String _copyJson(JsonFactory f, String json, boolean useBytes) throws IOException
    {
        if (useBytes) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            JsonGenerator jg = f.createGenerator(bytes);
            _copyJson(f, json, jg);
            return bytes.toString("UTF-8");
        }
        StringWriter sw = new StringWriter();
        JsonGenerator jg = f.createGenerator(sw);
        _copyJson(f, json, jg);
        return sw.toString();
    }
        
    protected void _copyJson(JsonFactory f, String json, JsonGenerator jg) throws IOException
    {
        JsonParser jp = f.createParser(json);
        while (jp.nextToken() != null) {
            jg.copyCurrentEvent(jp);
        }
        jp.close();
        jg.close();
    }
}
