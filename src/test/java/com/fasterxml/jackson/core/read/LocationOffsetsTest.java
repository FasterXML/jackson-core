package com.fasterxml.jackson.core.read;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.json.JsonFactory;

public class LocationOffsetsTest extends com.fasterxml.jackson.core.BaseTest
{
    final JsonFactory JSON_F = new JsonFactory();

    // Trivially simple unit test for basics wrt offsets
    public void testSimpleInitialOffsets() throws Exception
    {
        JsonLocation loc;
        JsonParser p;
        final String DOC = "{ }";

        // first, char based:
        p = JSON_F.createParser(ObjectReadContext.empty(), DOC);
        assertToken(JsonToken.START_OBJECT, p.nextToken());

        loc = p.getTokenLocation();
        assertEquals(-1L, loc.getByteOffset());
        assertEquals(0L, loc.getCharOffset());
        assertEquals(1, loc.getLineNr());
        assertEquals(1, loc.getColumnNr());

        loc = p.getCurrentLocation();
        assertEquals(-1L, loc.getByteOffset());
        assertEquals(1L, loc.getCharOffset());
        assertEquals(1, loc.getLineNr());
        assertEquals(2, loc.getColumnNr());

        p.close();

        // then byte-based

        p = JSON_F.createParser(ObjectReadContext.empty(), DOC.getBytes("UTF-8"));
        assertToken(JsonToken.START_OBJECT, p.nextToken());

        loc = p.getTokenLocation();
        assertEquals(0L, loc.getByteOffset());
        assertEquals(-1L, loc.getCharOffset());
        assertEquals(1, loc.getLineNr());
        assertEquals(1, loc.getColumnNr());

        loc = p.getCurrentLocation();
        assertEquals(1L, loc.getByteOffset());
        assertEquals(-1L, loc.getCharOffset());
        assertEquals(1, loc.getLineNr());
        assertEquals(2, loc.getColumnNr());

        p.close();
    }

    // for [core#111]
    public void testOffsetWithInputOffset() throws Exception
    {
        JsonLocation loc;
        JsonParser p;
        // 3 spaces before, 2 after, just for padding
        byte[] b = "   { }  ".getBytes("UTF-8");

        // and then peel them off
        p = JSON_F.createParser(ObjectReadContext.empty(), b, 3, b.length - 5);
        assertToken(JsonToken.START_OBJECT, p.nextToken());

        loc = p.getTokenLocation();
        assertEquals(0L, loc.getByteOffset());
        assertEquals(-1L, loc.getCharOffset());
        assertEquals(1, loc.getLineNr());
        assertEquals(1, loc.getColumnNr());

        loc = p.getCurrentLocation();
        assertEquals(1L, loc.getByteOffset());
        assertEquals(-1L, loc.getCharOffset());
        assertEquals(1, loc.getLineNr());
        assertEquals(2, loc.getColumnNr());

        p.close();
    }

    public void testOffsetWithoutInputOffset() throws Exception
    {
        JsonLocation loc;
        JsonParser p;
        // 3 spaces before, 2 after, just for padding
        byte[] b = "   { }  ".getBytes("UTF-8");

        // and then peel them off
        p = JSON_F.createParser(ObjectReadContext.empty(), b);
        assertToken(JsonToken.START_OBJECT, p.nextToken());

        loc = p.getTokenLocation();
        assertEquals(3L, loc.getByteOffset());
        assertEquals(-1L, loc.getCharOffset());
        assertEquals(1, loc.getLineNr());
        assertEquals(4, loc.getColumnNr());

        loc = p.getCurrentLocation();
        assertEquals(4L, loc.getByteOffset());
        assertEquals(-1L, loc.getCharOffset());
        assertEquals(1, loc.getLineNr());
        assertEquals(5, loc.getColumnNr());

        p.close();
    }

    public void testWithLazyStringReadStreaming() throws Exception
    {
        _testWithLazyStringRead(MODE_READER);
        _testWithLazyStringRead(MODE_INPUT_STREAM);
    }

    public void testWithLazyStringReadDataInput() throws Exception
    {
        // DataInput-backed reader does not track column, so can not
        // verify much; but force finishToken() regardless
        JsonParser p = createParser(JSON_F, MODE_DATA_INPUT, "[\"text\"]");
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(1, p.getCurrentLocation().getLineNr());
        p.finishToken();
        assertEquals("text", p.getText());
        p.close();
    }

    private void _testWithLazyStringRead(int readMode) throws Exception
    {
        JsonParser p = createParser(JSON_F, readMode, "[\"text\"]");
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        // initially location pointing to first character
        assertEquals(3, p.getCurrentLocation().getColumnNr());
        p.finishToken();
        // but will move once we force reading
        assertEquals(8, p.getCurrentLocation().getColumnNr());
        // and no change if we call again (but is ok to call)
        p.finishToken();
        assertEquals(8, p.getCurrentLocation().getColumnNr());

        // also just for fun, verify content
        assertEquals("text", p.getText());
        assertEquals(8, p.getCurrentLocation().getColumnNr());
        p.close();
    }
    
    // for [core#533]
    public void testUtf8Bom() throws Exception
    {
        JsonLocation loc;
        JsonParser p;

        byte[] b = withUtf8Bom("{ }".getBytes());

        // and then peel them off
        p = JSON_F.createParser(ObjectReadContext.empty(), b);
        assertToken(JsonToken.START_OBJECT, p.nextToken());

        loc = p.getTokenLocation();
        assertEquals(3L, loc.getByteOffset());
        assertEquals(-1L, loc.getCharOffset());
        assertEquals(1, loc.getLineNr());
        assertEquals(4, loc.getColumnNr());

        loc = p.getCurrentLocation();
        assertEquals(4L, loc.getByteOffset());
        assertEquals(-1L, loc.getCharOffset());
        assertEquals(1, loc.getLineNr());
        assertEquals(5, loc.getColumnNr());

        p.close();
    }

    public void testUtf8BomWithPadding() throws Exception
    {
        JsonLocation loc;
        JsonParser p;

        byte[] b = withUtf8Bom("   { }".getBytes());

        // and then peel them off
        p = JSON_F.createParser(ObjectReadContext.empty(), b);
        assertToken(JsonToken.START_OBJECT, p.nextToken());

        loc = p.getTokenLocation();
        assertEquals(6L, loc.getByteOffset());
        assertEquals(-1L, loc.getCharOffset());
        assertEquals(1, loc.getLineNr());
        assertEquals(7, loc.getColumnNr());

        loc = p.getCurrentLocation();
        assertEquals(7L, loc.getByteOffset());
        assertEquals(-1L, loc.getCharOffset());
        assertEquals(1, loc.getLineNr());
        assertEquals(8, loc.getColumnNr());

        p.close();
    }

    public void testUtf8BomWithInputOffset() throws Exception
    {
        JsonLocation loc;
        JsonParser p;

        byte[] b = withUtf8Bom("   { }".getBytes());

        // and then peel them off
        p = JSON_F.createParser(ObjectReadContext.empty(), b);
        assertToken(JsonToken.START_OBJECT, p.nextToken());

        loc = p.getTokenLocation();
        assertEquals(6L, loc.getByteOffset());
        assertEquals(-1L, loc.getCharOffset());
        assertEquals(1, loc.getLineNr());
        assertEquals(7, loc.getColumnNr());

        loc = p.getCurrentLocation();
        assertEquals(7L, loc.getByteOffset());
        assertEquals(-1L, loc.getCharOffset());
        assertEquals(1, loc.getLineNr());
        assertEquals(8, loc.getColumnNr());

        p.close();
    }

    private byte[] withUtf8Bom(byte[] bytes) {
        byte[] arr = new byte[bytes.length + 3];
        // write UTF-8 BOM
        arr[0] = (byte) 0xEF;
        arr[1] = (byte) 0xBB;
        arr[2] = (byte) 0xBF;
        System.arraycopy(bytes, 0, arr, 3, bytes.length);
        return arr;
    }
}
