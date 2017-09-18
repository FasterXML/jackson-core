package com.fasterxml.jackson.core.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.json.ReaderBasedJsonParser;
import com.fasterxml.jackson.core.json.UTF8StreamJsonParser;
import com.fasterxml.jackson.core.sym.ByteQuadsCanonicalizer;
import com.fasterxml.jackson.core.sym.CharsToNameCanonicalizer;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

/**
 * Unit tests for class {@link JsonParserSequence}.
 *
 * @date 2017-09-18
 * @see JsonParserSequence
 **/
public class JsonParserSequenceTest {
    @Test
    public void testClose() throws IOException {
        BufferRecycler bufferRecycler = new BufferRecycler();
        TestDelegates.BogusCodec testDelegates_BogusCodec = new TestDelegates.BogusCodec();
        IOContext iOContext = new IOContext(bufferRecycler, bufferRecycler, true);
        CharsToNameCanonicalizer charsToNameCanonicalizer = CharsToNameCanonicalizer.createRoot();
        ReaderBasedJsonParser readerBasedJsonParser = new ReaderBasedJsonParser(iOContext, 2, null, testDelegates_BogusCodec, charsToNameCanonicalizer);
        JsonParserSequence jsonParserSequence = JsonParserSequence.createFlattened(true, readerBasedJsonParser, readerBasedJsonParser);

        assertFalse(jsonParserSequence.isClosed());

        jsonParserSequence.close();

        assertTrue(jsonParserSequence.isClosed());
        assertNull(jsonParserSequence.nextToken());
    }

    @Test
    public void testSkipChildren() throws IOException {
        JsonParser[] jsonParserArray = new JsonParser[3];
        BufferRecycler bufferRecycler = new BufferRecycler();
        IOContext iOContext = new IOContext(bufferRecycler, bufferRecycler, true);
        byte[] byteArray = new byte[8];
        InputStream byteArrayInputStream = new ByteArrayInputStream(byteArray, 0, (byte) 58);
        TestDelegates.BogusCodec testDelegates_BogusCodec = new TestDelegates.BogusCodec();
        ByteQuadsCanonicalizer byteQuadsCanonicalizer = ByteQuadsCanonicalizer.createRoot();
        UTF8StreamJsonParser uTF8StreamJsonParser = new UTF8StreamJsonParser(iOContext, 0, byteArrayInputStream, testDelegates_BogusCodec, byteQuadsCanonicalizer, byteArray, -1, (byte) 9, true);
        JsonParserDelegate jsonParserDelegate = new JsonParserDelegate(jsonParserArray[0]);
        JsonParserSequence jsonParserSequence = JsonParserSequence.createFlattened(true, uTF8StreamJsonParser, jsonParserDelegate);
        JsonParserSequence jsonParserSequenceTwo = (JsonParserSequence) jsonParserSequence.skipChildren();

        assertEquals(2, jsonParserSequenceTwo.containedParsersCount());
    }
}