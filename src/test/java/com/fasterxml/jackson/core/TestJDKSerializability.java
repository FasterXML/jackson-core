package com.fasterxml.jackson.core;

import java.io.*;

import com.fasterxml.jackson.core.io.ContentReference;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;

/**
 * Unit tests for [core#31] (https://github.com/FasterXML/jackson-core/issues/31)
 */
public class TestJDKSerializability extends BaseTest
{
    /*
    /**********************************************************************
    /* Main factory type(s)
    /**********************************************************************
     */

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
    /**********************************************************************
    /* Parser-related types
    /**********************************************************************
     */

    public void testBase64Variant() throws Exception
    {
        {
            Base64Variant orig = Base64Variants.PEM;
            final String exp = _encodeBase64(orig);
            byte[] stuff = jdkSerialize(orig);
            Base64Variant back = jdkDeserialize(stuff);
            assertSame(orig, back);
            assertEquals(exp, _encodeBase64(back));
        }

        // and then with a twist
        {
            Base64Variant orig = Base64Variants.MODIFIED_FOR_URL;
            assertFalse(orig.usesPadding());
            Base64Variant mod = orig.withWritePadding(true);
            assertTrue(mod.usesPadding());
            assertNotSame(orig, mod);
            assertFalse(orig.equals(mod));
            assertFalse(mod.equals(orig));

            final String exp = _encodeBase64(mod);
            byte[] stuff = jdkSerialize(mod);
            Base64Variant back = jdkDeserialize(stuff);
            assertTrue(back.usesPadding());
            assertNotSame(mod, back);
            assertEquals(mod, back);
            assertEquals(exp, _encodeBase64(back));
        }
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

    public void testSourceReference() throws Exception
    {
        ContentReference ref = ContentReference.construct(true, "text");

        byte[] stuff = jdkSerialize(ref);
        ContentReference ref2 = jdkDeserialize(stuff);
        assertNotNull(ref2);
        assertSame(ref2, ContentReference.unknown());
    }

    /*
    /**********************************************************************
    /* Exception types
    /**********************************************************************
     */

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
    /**********************************************************************
    /* Misc other types
    /**********************************************************************
     */

    public void testPointerSerializationNonEmpty() throws Exception
    {
        // First, see that we can write and read a general JsonPointer
        final String INPUT = "/Image/15/name";
        JsonPointer original = JsonPointer.compile(INPUT);
        byte[] ser = jdkSerialize(original);
        JsonPointer copy = jdkDeserialize(ser);
        assertNotSame(copy, original);
        assertEquals(original, copy);
        assertEquals(original.hashCode(), copy.hashCode());

        // 11-Oct-2022, tatu: Let's verify sub-path serializations too,
        //   since 2.14 has rewritten internal implementation
        JsonPointer branch = original.tail();
        assertEquals("/15/name", branch.toString());
        ser = jdkSerialize(branch);
        copy = jdkDeserialize(ser);
        assertEquals("/15/name", copy.toString());
        assertEquals(branch, copy);
        assertEquals(branch.hashCode(), copy.hashCode());

        JsonPointer leaf = branch.tail();
        assertEquals("/name", leaf.toString());
        ser = jdkSerialize(leaf);
        copy = jdkDeserialize(ser);
        assertEquals("/name", copy.toString());
        assertEquals(leaf, copy);
        assertEquals(leaf.hashCode(), copy.hashCode());
    }

    public void testPointerSerializationEmpty() throws Exception
    {
        // and then verify that "empty" instance gets canonicalized
        final JsonPointer emptyP = JsonPointer.empty();
        byte[] ser = jdkSerialize(emptyP);
        JsonPointer result = jdkDeserialize(ser);
        assertSame("Should get same 'empty' instance when JDK serialize+deserialize",
                emptyP, result);
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

    protected void _copyJson(JsonFactory f, String json, JsonGenerator g) throws IOException
    {
        JsonParser p = f.createParser(json);
        while (p.nextToken() != null) {
            g.copyCurrentEvent(p);
        }
        p.close();
        g.close();
    }

    protected String _encodeBase64(Base64Variant b64) throws IOException
    {
        // something with padding...
        return b64.encode("abcde".getBytes("UTF-8"));
    }
}
