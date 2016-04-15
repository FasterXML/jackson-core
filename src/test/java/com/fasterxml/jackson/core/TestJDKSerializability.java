package com.fasterxml.jackson.core;

import java.io.*;

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.Base64Variants;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;

/**
 * Unit tests for [Issue#31] (https://github.com/FasterXML/jackson-core/issues/31)
 */
public class TestJDKSerializability extends BaseTest
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

    public void testBase64Variant() throws Exception
    {
        Base64Variant orig = Base64Variants.PEM;
        byte[] stuff = jdkSerialize(orig);
        Base64Variant back = jdkDeserialize(stuff);
        assertSame(orig, back);
    }

    public void testPrettyPrinter() throws Exception
    {
        PrettyPrinter p = new DefaultPrettyPrinter();
        byte[] stuff = jdkSerialize(p);
        PrettyPrinter back = jdkDeserialize(stuff);
        // what should we test?
        assertNotNull(back);
    }

    public void testLocation() throws Exception
    {
        JsonFactory jf = new JsonFactory();
        JsonParser jp = jf.createParser("  { }");
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        JsonLocation loc = jp.getCurrentLocation();

        byte[] stuff = jdkSerialize(loc);
        JsonLocation loc2 = jdkDeserialize(stuff);
        assertNotNull(loc2);
        
        assertEquals(loc.getLineNr(), loc2.getLineNr());
        assertEquals(loc.getColumnNr(), loc2.getColumnNr());
        jp.close();
    }

    public void testParseException() throws Exception
    {
        JsonFactory jf = new JsonFactory();
        JsonParser p = jf.createParser("  { garbage! }");
        JsonParseException exc = null;
        try {
            p.nextToken();
            p.nextToken();
            fail("Should not get here");
        } catch (JsonParseException e) {
            exc = e;
        }
        p.close();
        byte[] stuff = jdkSerialize(exc);
        JsonParseException result = jdkDeserialize(stuff);
        assertNotNull(result);
    }

    public void testGenerationException() throws Exception
    {
        JsonFactory jf = new JsonFactory();
        JsonGenerator g = jf.createGenerator(new ByteArrayOutputStream());
        JsonGenerationException exc = null;
        g.writeStartObject();
        try {
            g.writeNumber(4);
            fail("Should not get here");
        } catch (JsonGenerationException e) {
            exc = e;
        }
        g.close();
        byte[] stuff = jdkSerialize(exc);
        JsonGenerationException result = jdkDeserialize(stuff);
        assertNotNull(result);
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
    
    @SuppressWarnings("resource")
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
