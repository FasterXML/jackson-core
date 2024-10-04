package com.fasterxml.jackson.core.constraints;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JUnit5TestBase;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.json.async.NonBlockingByteBufferJsonParser;
import com.fasterxml.jackson.core.json.async.NonBlockingJsonParser;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Set of basic unit tests for verifying that the token count
 * functionality works as expected.
 */
public class TokenCountTest extends JUnit5TestBase {
    private final static JsonFactory JSON_FACTORY = JsonFactory.builder()
        .streamReadConstraints(StreamReadConstraints.builder().maxTokenCount(Long.MAX_VALUE).build())
        .build();
    private final static String ARRAY_DOC = a2q("{ 'nums': [1,2,3,4,5,6,7,8,9,10] }");
    private final static String SHORT_ARRAY_DOC = a2q("{ 'nums': [1,2,3] }");

    @Test
    void arrayDoc() throws Exception
    {
        for (int mode : ALL_MODES) {
            _testArrayDoc(mode);
        }
    }

    @Test
    void arrayDocNonBlockingArray() throws Exception
    {
        final byte[] input = ARRAY_DOC.getBytes("UTF-8");
        try (NonBlockingJsonParser p = (NonBlockingJsonParser) JSON_FACTORY.createNonBlockingByteArrayParser()) {
            p.feedInput(input, 0, input.length);
            p.endOfInput();
            _testArrayDoc(p);
        }
    }

    @Test
    void arrayDocNonBlockingBuffer() throws Exception
    {
        final byte[] input = ARRAY_DOC.getBytes("UTF-8");
        try (NonBlockingByteBufferJsonParser p = (NonBlockingByteBufferJsonParser) JSON_FACTORY.createNonBlockingByteBufferParser()) {
            p.feedInput(ByteBuffer.wrap(input, 0, input.length));
            p.endOfInput();
            _testArrayDoc(p);
        }
    }

    @Test
    void shortArrayDoc() throws Exception
    {
        for (int mode : ALL_MODES) {
            _testShortArrayDoc(mode);
        }
    }

    @Test
    void shortArrayDocNonBlockingArray() throws Exception
    {
        final byte[] input = SHORT_ARRAY_DOC.getBytes("UTF-8");
        try (NonBlockingJsonParser p = (NonBlockingJsonParser) JSON_FACTORY.createNonBlockingByteArrayParser()) {
            p.feedInput(input, 0, input.length);
            p.endOfInput();
            _testShortArrayDoc(p);
        }
    }

    @Test
    void shortArrayDocNonBlockingBuffer() throws Exception
    {
        final byte[] input = SHORT_ARRAY_DOC.getBytes("UTF-8");
        try (NonBlockingByteBufferJsonParser p = (NonBlockingByteBufferJsonParser)
            JSON_FACTORY.createNonBlockingByteBufferParser()) {
            p.feedInput(ByteBuffer.wrap(input, 0, input.length));
            p.endOfInput();
            _testShortArrayDoc(p);
        }
    }

    @Test
    void sampleDoc() throws Exception
    {
        for (int mode : ALL_MODES) {
            _testSampleDoc(mode);
        }
    }

    @Test
    void sampleDocNonBlockingArray() throws Exception
    {
        final byte[] input = SAMPLE_DOC_JSON_SPEC.getBytes("UTF-8");
        try (NonBlockingJsonParser p = (NonBlockingJsonParser) JSON_FACTORY.createNonBlockingByteArrayParser()) {
            p.feedInput(input, 0, input.length);
            p.endOfInput();
            _testSampleDoc(p);
        }
    }

    @Test
    void sampleDocNonBlockingBuffer() throws Exception
    {
        final byte[] input = SAMPLE_DOC_JSON_SPEC.getBytes("UTF-8");
        try (NonBlockingByteBufferJsonParser p = (NonBlockingByteBufferJsonParser)
            JSON_FACTORY.createNonBlockingByteBufferParser()) {
            p.feedInput(ByteBuffer.wrap(input, 0, input.length));
            p.endOfInput();
            _testSampleDoc(p);
        }
    }

    private void _testArrayDoc(int mode) throws Exception
    {
        try (JsonParser p = createParser(JSON_FACTORY, mode, ARRAY_DOC)) {
            _testArrayDoc(p);
        }
    }

    private void _testArrayDoc(JsonParser p) throws Exception
    {
        assertEquals(0, p.currentTokenCount());
        consumeTokens(p);
        assertEquals(15, p.currentTokenCount());
    }

    private void _testShortArrayDoc(int mode) throws Exception
    {
        try (JsonParser p = createParser(JSON_FACTORY, mode, SHORT_ARRAY_DOC)) {
            _testShortArrayDoc(p);
        }
    }

    private void _testShortArrayDoc(JsonParser p) throws Exception
    {
        assertEquals(0, p.currentTokenCount());
        consumeTokens(p);
        assertEquals(8, p.currentTokenCount());
    }

    private void _testSampleDoc(int mode) throws Exception
    {
        try (JsonParser p = createParser(JSON_FACTORY, mode, SAMPLE_DOC_JSON_SPEC)) {
            _testSampleDoc(p);
        }
    }

    private void _testSampleDoc(JsonParser p) throws Exception
    {
        assertEquals(0, p.currentTokenCount());
        consumeTokens(p);
        assertEquals(27, p.currentTokenCount());
    }

    private void consumeTokens(JsonParser p) throws Exception {
        while (p.nextToken() != null) {
            ;
        }
    }
}
