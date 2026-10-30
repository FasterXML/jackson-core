package com.fasterxml.jackson.core.constraints;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import com.fasterxml.jackson.core.async.ByteBufferFeeder;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;
import com.fasterxml.jackson.core.testsupport.MockDataInput;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

// [core#1047]: Add max-name-length constraints
class LargeDocReadTest extends AsyncTestBase
{
    private final JsonFactory JSON_F_DEFAULT = newStreamFactory();

    private final JsonFactory JSON_F_DOC_10K = JsonFactory.builder()
            .streamReadConstraints(StreamReadConstraints.builder().maxDocumentLength(10_000L).build())
            .build();

    private final JsonFactory JSON_F_MAX_TOKENS_1K = JsonFactory.builder()
        .streamReadConstraints(StreamReadConstraints.builder().maxTokenCount(1_000L).build())
        .build();

    // Test name that is below default max name
    @Test
    void largeNameBytes() throws Exception {
        final String doc = generateJSON(StreamReadConstraints.defaults().getMaxNameLength() - 100);
        try (JsonParser p = createParserUsingStream(JSON_F_DEFAULT, doc, "UTF-8")) {
            consumeTokens(p);
        }
    }

    @Test
    void largeNameChars() throws Exception {
        final String doc = generateJSON(StreamReadConstraints.defaults().getMaxNameLength() - 100);
        try (JsonParser p = createParserUsingReader(JSON_F_DEFAULT, doc)) {
            consumeTokens(p);
        }
    }

    @Test
    void largeNameWithSmallLimitBytes() throws Exception
    {
        final String doc = generateJSON(12_000);
        try (JsonParser p = createParserUsingStream(JSON_F_DOC_10K, doc, "UTF-8")) {
            consumeTokens(p);
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            verifyMaxDocLen(JSON_F_DOC_10K, e);
        }
        // [core#1548] validate for fixed buffer too
        try (JsonParser p = JSON_F_DOC_10K.createParser(utf8Bytes(doc))) {
            consumeTokens(p);
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            verifyMaxDocLen(JSON_F_DOC_10K, e);
        }
    }

    @Test
    void largeNameWithSmallLimitChars() throws Exception
    {
        final String doc = generateJSON(12_000);
        try (JsonParser p = createParserUsingReader(JSON_F_DOC_10K, doc)) {
            consumeTokens(p);
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            verifyMaxDocLen(JSON_F_DOC_10K, e);
        }
        // [core#1548] validate for fixed buffer too
        try (JsonParser p = JSON_F_DOC_10K.createParser(doc.toCharArray())) {
            consumeTokens(p);
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            verifyMaxDocLen(JSON_F_DOC_10K, e);
        }
    }

    @Test
    void largeNameWithSmallLimitAsync() throws Exception
    {
        final byte[] doc = utf8Bytes(generateJSON(12_000));

        // first with byte[] backend
        try (AsyncReaderWrapper p = asyncForBytes(JSON_F_DOC_10K, 1000, doc, 1)) {
            consumeAsync(p);
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            verifyMaxDocLen(JSON_F_DOC_10K, e);
        }

        // then with byte buffer
        try (AsyncReaderWrapper p = asyncForByteBuffer(JSON_F_DOC_10K, 1000, doc, 1)) {
            consumeAsync(p);
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            verifyMaxDocLen(JSON_F_DOC_10K, e);
        }
    }

    // [core#1642] maxDocumentLength must also be enforced when the caller feeds
    // the whole document via a single feedInput() call (e.g. pre-buffered input),
    // not just when input arrives split across multiple feedInput() calls.
    @Test
    void largeNameWithSmallLimitAsyncSingleFeed() throws Exception
    {
        final byte[] doc = utf8Bytes(generateJSON(12_000));

        // first with byte[] backend: bytesPerRead >= doc.length so the whole
        // document goes through in exactly one feedInput() call
        try (AsyncReaderWrapper p = asyncForBytes(JSON_F_DOC_10K, doc.length, doc, 1)) {
            consumeAsync(p);
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            verifyMaxDocLen(JSON_F_DOC_10K, e);
        }

        // then with byte buffer backend, same single-call condition
        try (AsyncReaderWrapper p = asyncForByteBuffer(JSON_F_DOC_10K, doc.length, doc, 1)) {
            consumeAsync(p);
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            verifyMaxDocLen(JSON_F_DOC_10K, e);
        }
    }

    // [core#1642] Boundary check: a single feedInput() call carrying EXACTLY
    // maxDocumentLength bytes must still parse successfully -- validateDocumentLength()
    // rejects only len > maxDocumentLength, so the limit itself is inclusive.
    // This pins down "bytes fed, not consumed" semantics and guards against a
    // future off-by-one in the single-feed fix.
    @Test
    void largeNameWithSmallLimitAsyncSingleFeedAtBoundary() throws Exception
    {
        final long limit = JSON_F_DOC_10K.streamReadConstraints().getMaxDocumentLength();
        final byte[] doc = utf8Bytes(generateExactLengthJSON((int) limit));
        assertEquals(limit, doc.length);

        // first with byte[] backend: bytesPerRead >= doc.length so the whole
        // document goes through in exactly one feedInput() call
        try (AsyncReaderWrapper p = asyncForBytes(JSON_F_DOC_10K, doc.length, doc, 1)) {
            consumeAsync(p);
        }

        // then with byte buffer backend, same single-call condition
        try (AsyncReaderWrapper p = asyncForByteBuffer(JSON_F_DOC_10K, doc.length, doc, 1)) {
            consumeAsync(p);
        }
    }

    // [core#1642] Same boundary, but reached across MANY feedInput() calls: bytes
    // fed must accumulate to exactly maxDocumentLength and still parse, verifying
    // the single-feed fix did not start double-counting incrementally fed buffers.
    @Test
    void largeNameWithSmallLimitAsyncMultiFeedAtBoundary() throws Exception
    {
        final long limit = JSON_F_DOC_10K.streamReadConstraints().getMaxDocumentLength();
        final byte[] doc = utf8Bytes(generateExactLengthJSON((int) limit));
        assertEquals(limit, doc.length);

        // 1000 bytes per call, so exactly 10 feedInput() calls totalling the limit
        try (AsyncReaderWrapper p = asyncForBytes(JSON_F_DOC_10K, 1000, doc, 1)) {
            consumeAsync(p);
        }
        try (AsyncReaderWrapper p = asyncForByteBuffer(JSON_F_DOC_10K, 1000, doc, 1)) {
            consumeAsync(p);
        }
    }

    // [core#1642] A rejected feedInput() must not corrupt the running byte count:
    // validation happens BEFORE any state is updated, so a caller that catches the
    // StreamConstraintsException and keeps feeding still gets an accurate total
    // (the rejected call's predecessor must not be counted twice).
    @Test
    void docLengthCountIntactAfterRejectedFeedBytes() throws Exception
    {
        try (JsonParser p = JSON_F_DOC_10K.createNonBlockingByteArrayParser()) {
            final ByteArrayFeeder feeder = (ByteArrayFeeder) p.getNonBlockingInputFeeder();

            // 5000 fed, well under the 10000 limit
            feeder.feedInput(whitespace(5000), 0, 5000);
            assertToken(JsonToken.NOT_AVAILABLE, p.nextToken());

            // would reach 11000: rejected, and must leave the count at 5000
            try {
                feeder.feedInput(whitespace(6000), 0, 6000);
                fail("expected StreamConstraintsException");
            } catch (StreamConstraintsException e) {
                verifyMaxDocLen(JSON_F_DOC_10K, e);
            }

            // 5000 more == 10000 total: at the limit, so must still be accepted
            feeder.feedInput(whitespace(5000), 0, 5000);
            assertToken(JsonToken.NOT_AVAILABLE, p.nextToken());

            // and one byte past it must report the true total, not an inflated one
            try {
                feeder.feedInput(whitespace(1), 0, 1);
                fail("expected StreamConstraintsException");
            } catch (StreamConstraintsException e) {
                verifyException(e, "Document length (10001)");
            }
        }
    }

    // [core#1642] as above, for the ByteBuffer-backed parser
    @Test
    void docLengthCountIntactAfterRejectedFeedByteBuffer() throws Exception
    {
        try (JsonParser p = JSON_F_DOC_10K.createNonBlockingByteBufferParser()) {
            final ByteBufferFeeder feeder = (ByteBufferFeeder) p.getNonBlockingInputFeeder();

            feeder.feedInput(ByteBuffer.wrap(whitespace(5000)));
            assertToken(JsonToken.NOT_AVAILABLE, p.nextToken());

            try {
                feeder.feedInput(ByteBuffer.wrap(whitespace(6000)));
                fail("expected StreamConstraintsException");
            } catch (StreamConstraintsException e) {
                verifyMaxDocLen(JSON_F_DOC_10K, e);
            }

            feeder.feedInput(ByteBuffer.wrap(whitespace(5000)));
            assertToken(JsonToken.NOT_AVAILABLE, p.nextToken());

            try {
                feeder.feedInput(ByteBuffer.wrap(whitespace(1)));
                fail("expected StreamConstraintsException");
            } catch (StreamConstraintsException e) {
                verifyException(e, "Document length (10001)");
            }
        }
    }

    // [core#1570] Should fail fast when DataInput used with maxDocumentLength set
    @Test
    void dataInputWithDocLengthLimitFails() throws Exception
    {
        final String doc = generateJSON(100);
        try (JsonParser p = JSON_F_DOC_10K.createParser(new MockDataInput(doc))) {
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            verifyException(e, "DataInput");
            verifyException(e, "maxDocumentLength");
        }
    }

    // [core#1570] DataInput without maxDocumentLength should still work
    @Test
    void dataInputWithoutDocLengthLimitWorks() throws Exception
    {
        final String doc = generateJSON(100);
        try (JsonParser p = JSON_F_DEFAULT.createParser(new MockDataInput(doc))) {
            consumeTokens(p);
        }
    }

    @Test
    void tokenLimitBytes() throws Exception {
        final String doc = generateJSON(StreamReadConstraints.defaults().getMaxNameLength() - 100);
        try (JsonParser p = createParserUsingStream(JSON_F_MAX_TOKENS_1K, doc, "UTF-8")) {
            consumeTokens(p);
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            assertEquals("Token count (1001) exceeds the maximum allowed (1000, from `StreamReadConstraints.getMaxTokenCount()`)",
                    e.getMessage());
        }
    }

    private void consumeTokens(JsonParser p) throws IOException {
        while (p.nextToken() != null) {
            ;
        }
    }

    private void consumeAsync(AsyncReaderWrapper w) throws IOException {
        while (w.nextToken() != null) {
            ;
        }
    }

    // Builds a valid JSON array whose UTF-8 byte length is exactly {@code exactLen},
    // using trailing whitespace padding before the closing bracket (all-ASCII content,
    // so char length == byte length).
    private String generateExactLengthJSON(final int exactLen) {
        final StringBuilder sb = new StringBuilder();
        sb.append('[');
        while (sb.length() < exactLen - 10) {
            sb.append("1,");
        }
        sb.append('1');
        while (sb.length() < exactLen - 1) {
            sb.append(' ');
        }
        sb.append(']');
        return sb.toString();
    }

    // Content that is valid-but-tokenless, so buffers can be fed and fully consumed
    // without producing tokens: lets tests exercise feedInput() accounting directly.
    private byte[] whitespace(final int len) {
        final byte[] b = new byte[len];
        Arrays.fill(b, (byte) ' ');
        return b;
    }

    private String generateJSON(final int docLen) {
        final StringBuilder sb = new StringBuilder();
        sb.append("[");

        int i = 0;
        while (docLen > sb.length()) {
            sb.append(++i).append(",\n");
        }
        sb.append("true ] ");
        return sb.toString();
    }

    private void verifyMaxDocLen(JsonFactory f, StreamConstraintsException e) {
        verifyException(e, "Document length");
        verifyException(e, "exceeds the maximum allowed ("
                +f.streamReadConstraints().getMaxDocumentLength()
                );
    }
}
