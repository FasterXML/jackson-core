package com.fasterxml.jackson.core.constraints;

import java.io.IOException;

import com.fasterxml.jackson.core.*;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;

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
