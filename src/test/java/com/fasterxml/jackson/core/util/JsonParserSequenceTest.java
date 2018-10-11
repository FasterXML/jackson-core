package com.fasterxml.jackson.core.util;

import com.fasterxml.jackson.core.BaseTest;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectReadContext;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.json.ReaderBasedJsonParser;
import com.fasterxml.jackson.core.json.UTF8StreamJsonParser;
import com.fasterxml.jackson.core.sym.ByteQuadsCanonicalizer;
import com.fasterxml.jackson.core.sym.CharsToNameCanonicalizer;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Unit tests for class {@link JsonParserSequence}.
 *
 * @date 2017-09-18
 * @see JsonParserSequence
 **/
@SuppressWarnings("resource")
public class JsonParserSequenceTest extends BaseTest
{
    @Test
    public void testClose() throws IOException {
        IOContext ioContext = new IOContext(new BufferRecycler(), this, true);
        ReaderBasedJsonParser readerBasedJsonParser = new ReaderBasedJsonParser(
                ObjectReadContext.empty(),
                ioContext,
                2, 0, null, CharsToNameCanonicalizer.createRoot());
        JsonParserSequence jsonParserSequence = JsonParserSequence.createFlattened(true, readerBasedJsonParser, readerBasedJsonParser);

        assertFalse(jsonParserSequence.isClosed());

        jsonParserSequence.close();

        assertTrue(jsonParserSequence.isClosed());
        assertNull(jsonParserSequence.nextToken());
    }

    @Test
    public void testSkipChildren() throws IOException {
        JsonParser[] jsonParserArray = new JsonParser[3];
        IOContext ioContext = new IOContext(new BufferRecycler(), jsonParserArray, true);
        byte[] byteArray = new byte[8];
        InputStream byteArrayInputStream = new ByteArrayInputStream(byteArray, 0, (byte) 58);
        UTF8StreamJsonParser uTF8StreamJsonParser = new UTF8StreamJsonParser(ObjectReadContext.empty(),
                ioContext,
                0, 0, byteArrayInputStream, ByteQuadsCanonicalizer.createRoot(),
                byteArray, -1, (byte) 9, true);
        JsonParserDelegate jsonParserDelegate = new JsonParserDelegate(jsonParserArray[0]);
        JsonParserSequence jsonParserSequence = JsonParserSequence.createFlattened(true, uTF8StreamJsonParser, jsonParserDelegate);
        JsonParserSequence jsonParserSequenceTwo = (JsonParserSequence) jsonParserSequence.skipChildren();

        assertEquals(2, jsonParserSequenceTwo.containedParsersCount());
    }
}