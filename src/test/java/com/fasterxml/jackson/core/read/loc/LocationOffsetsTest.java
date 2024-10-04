package com.fasterxml.jackson.core.read.loc;

import java.io.IOException;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocationOffsetsTest extends JUnit5TestBase
{
    final JsonFactory JSON_F = new JsonFactory();

    // Trivially simple unit test for basics wrt offsets
    @Test
    void simpleInitialOffsets() throws Exception
    {
        JsonLocation loc;
        JsonParser p;
        final String DOC = "{ }";

        // first, char based:
        p = JSON_F.createParser(DOC);
        assertToken(JsonToken.START_OBJECT, p.nextToken());

        loc = p.currentTokenLocation();
        assertEquals(-1L, loc.getByteOffset());
        assertEquals(0L, loc.getCharOffset());
        assertEquals(1, loc.getLineNr());
        assertEquals(1, loc.getColumnNr());

        loc = p.currentLocation();
        assertEquals(-1L, loc.getByteOffset());
        assertEquals(1L, loc.getCharOffset());
        assertEquals(1, loc.getLineNr());
        assertEquals(2, loc.getColumnNr());

        p.close();

        // then byte-based

        p = JSON_F.createParser(DOC.getBytes("UTF-8"));
        assertToken(JsonToken.START_OBJECT, p.nextToken());

        loc = p.currentTokenLocation();
        assertEquals(0L, loc.getByteOffset());
        assertEquals(-1L, loc.getCharOffset());
        assertEquals(1, loc.getLineNr());
        assertEquals(1, loc.getColumnNr());

        loc = p.currentLocation();
        assertEquals(1L, loc.getByteOffset());
        assertEquals(-1L, loc.getCharOffset());
        assertEquals(1, loc.getLineNr());
        assertEquals(2, loc.getColumnNr());

        p.close();
    }

    // for [core#111]
    @Test
    void offsetWithInputOffset() throws Exception
    {
        JsonLocation loc;
        JsonParser p;
        // 3 spaces before, 2 after, just for padding
        byte[] b = "   { }  ".getBytes("UTF-8");

        // and then peel them off
        p = JSON_F.createParser(b, 3, b.length-5);
        assertToken(JsonToken.START_OBJECT, p.nextToken());

        loc = p.currentTokenLocation();
        assertEquals(0L, loc.getByteOffset());
        assertEquals(-1L, loc.getCharOffset());
        assertEquals(1, loc.getLineNr());
        assertEquals(1, loc.getColumnNr());

        loc = p.currentLocation();
        assertEquals(1L, loc.getByteOffset());
        assertEquals(-1L, loc.getCharOffset());
        assertEquals(1, loc.getLineNr());
        assertEquals(2, loc.getColumnNr());

        p.close();
    }

    @Test
    void offsetWithoutInputOffset() throws Exception
    {
        JsonLocation loc;
        JsonParser p;
        // 3 spaces before, 2 after, just for padding
        byte[] b = "   { }  ".getBytes("UTF-8");

        // and then peel them off
        p = JSON_F.createParser(b);
        assertToken(JsonToken.START_OBJECT, p.nextToken());

        loc = p.currentTokenLocation();
        assertEquals(3L, loc.getByteOffset());
        assertEquals(-1L, loc.getCharOffset());
        assertEquals(1, loc.getLineNr());
        assertEquals(4, loc.getColumnNr());

        loc = p.currentLocation();
        assertEquals(4L, loc.getByteOffset());
        assertEquals(-1L, loc.getCharOffset());
        assertEquals(1, loc.getLineNr());
        assertEquals(5, loc.getColumnNr());

        p.close();
    }

    @Test
    void withLazyStringReadStreaming() throws Exception
    {
        _testWithLazyStringRead(MODE_READER);
        _testWithLazyStringRead(MODE_INPUT_STREAM);
    }

    @Test
    void withLazyStringReadDataInput() throws Exception
    {
        // DataInput-backed reader does not track column, so can not
        // verify much; but force finishToken() regardless
        JsonParser p = createParser(JSON_F, MODE_DATA_INPUT, "[\"text\"]");
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(1, p.currentLocation().getLineNr());
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
        assertEquals(3, p.currentLocation().getColumnNr());
        p.finishToken();
        // but will move once we force reading
        assertEquals(8, p.currentLocation().getColumnNr());
        // and no change if we call again (but is ok to call)
        p.finishToken();
        assertEquals(8, p.currentLocation().getColumnNr());

        // also just for fun, verify content
        assertEquals("text", p.getText());
        assertEquals(8, p.currentLocation().getColumnNr());
        p.close();
    }

    // for [core#533]
    @Test
    void utf8Bom() throws Exception
    {
        JsonLocation loc;
        JsonParser p;

        byte[] b = withUtf8Bom("{ }".getBytes());

        // and then peel them off
        p = JSON_F.createParser(b);
        assertToken(JsonToken.START_OBJECT, p.nextToken());

        loc = p.currentTokenLocation();
        assertEquals(3L, loc.getByteOffset());
        assertEquals(-1L, loc.getCharOffset());
        assertEquals(1, loc.getLineNr());
        assertEquals(4, loc.getColumnNr());

        loc = p.currentLocation();
        assertEquals(4L, loc.getByteOffset());
        assertEquals(-1L, loc.getCharOffset());
        assertEquals(1, loc.getLineNr());
        assertEquals(5, loc.getColumnNr());

        p.close();
    }

    @Test
    void utf8BomWithPadding() throws Exception
    {
        JsonLocation loc;
        JsonParser p;

        byte[] b = withUtf8Bom("   { }".getBytes());

        // and then peel them off
        p = JSON_F.createParser(b);
        assertToken(JsonToken.START_OBJECT, p.nextToken());

        loc = p.currentTokenLocation();
        assertEquals(6L, loc.getByteOffset());
        assertEquals(-1L, loc.getCharOffset());
        assertEquals(1, loc.getLineNr());
        assertEquals(7, loc.getColumnNr());

        loc = p.currentLocation();
        assertEquals(7L, loc.getByteOffset());
        assertEquals(-1L, loc.getCharOffset());
        assertEquals(1, loc.getLineNr());
        assertEquals(8, loc.getColumnNr());

        p.close();
    }

    @Test
    void utf8BomWithInputOffset() throws Exception
    {
        JsonLocation loc;
        JsonParser p;

        byte[] b = withUtf8Bom("   { }".getBytes());

        // and then peel them off
        p = JSON_F.createParser(b);
        assertToken(JsonToken.START_OBJECT, p.nextToken());

        loc = p.currentTokenLocation();
        assertEquals(6L, loc.getByteOffset());
        assertEquals(-1L, loc.getCharOffset());
        assertEquals(1, loc.getLineNr());
        assertEquals(7, loc.getColumnNr());

        loc = p.currentLocation();
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

    // [core#603]
    @Test
    void bigPayload() throws IOException {
        JsonLocation loc;
        JsonParser p;

        String doc = "{\"key\":\"" + generateRandomAlpha(50000) + "\"}";

        p = createParserUsingStream(JSON_F, doc, "UTF-8");

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        loc = p.currentTokenLocation();
        assertEquals(0, loc.getByteOffset());
        assertEquals(-1L, loc.getCharOffset());
        assertEquals(1, loc.getLineNr());
        assertEquals(1, loc.getColumnNr());
        loc = p.currentLocation();
        assertEquals(1, loc.getByteOffset());
        assertEquals(-1L, loc.getCharOffset());
        assertEquals(1, loc.getLineNr());
        assertEquals(2, loc.getColumnNr());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        loc = p.currentTokenLocation();
        assertEquals(1, loc.getByteOffset());
        assertEquals(-1L, loc.getCharOffset());
        assertEquals(1, loc.getLineNr());
        assertEquals(2, loc.getColumnNr());
        loc = p.currentLocation();
        assertEquals(8, loc.getByteOffset());
        assertEquals(-1L, loc.getCharOffset());
        assertEquals(1, loc.getLineNr());
        assertEquals(9, loc.getColumnNr());

        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        loc = p.currentTokenLocation();
        assertEquals(7, loc.getByteOffset());
        assertEquals(-1L, loc.getCharOffset());
        assertEquals(1, loc.getLineNr());
        assertEquals(8, loc.getColumnNr());
        loc = p.currentLocation();
        assertEquals(8, loc.getByteOffset());
        assertEquals(-1L, loc.getCharOffset());
        assertEquals(1, loc.getLineNr());
        assertEquals(9, loc.getColumnNr());

        p.getTextCharacters();
        loc = p.currentTokenLocation();
        assertEquals(7, loc.getByteOffset());
        assertEquals(-1L, loc.getCharOffset());
        assertEquals(1, loc.getLineNr());
        assertEquals(8, loc.getColumnNr());
        loc = p.currentLocation();
        assertEquals(doc.length() - 1, loc.getByteOffset());
        assertEquals(-1L, loc.getCharOffset());
        assertEquals(1, loc.getLineNr());
        assertEquals(doc.length(), loc.getColumnNr());

        p.close();
    }

    private String generateRandomAlpha(int length) {
        StringBuilder sb = new StringBuilder(length);
        Random rnd = new Random(length);
        for (int i = 0; i < length; ++i) {
            // let's limit it not to include surrogate pairs:
            char ch = (char) ('A' + rnd.nextInt(26));
            sb.append(ch);
        }
        return sb.toString();
    }
}
