package com.fasterxml.jackson.core.async;

import java.io.IOException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for [jackson-core#1784]: non-blocking variant of
 * {@code com.fasterxml.jackson.core.read.PropertyName0xFF1784Test}.
 */
class AsyncPropertyName0xFF1784Test extends AsyncTestBase
{
    enum Variant {
        BYTE_ARRAY, BYTE_BUFFER;

        AsyncReaderWrapper wrap(JsonFactory f, int bytesPerRead, byte[] data)
            throws IOException
        {
            switch (this) {
            case BYTE_ARRAY: return asyncForBytes(f, bytesPerRead, data, 0);
            case BYTE_BUFFER: return asyncForByteBuffer(f, bytesPerRead, data, 0);
            default: throw new IllegalStateException();
            }
        }
    }

    private static final String[] NAMES = { "a", "ab", "abc", "abcd", "admin", "abcdef", "abcdefghi" };

    private static final int[] BYTES_PER_READ = { 1, 2, 3, 1000 };

    private final JsonFactory APOS_FACTORY = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .build();

    private final JsonFactory UNQUOTED_FACTORY = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
            .build();

    @ParameterizedTest
    @EnumSource(Variant.class)
    void ffAfterSameNameInDocument(Variant v) throws Exception
    {
        for (int bpr : BYTES_PER_READ) {
            for (String name : NAMES) {
                for (int count = 1; count <= 3; ++count) {
                    for (int pos = 0; pos <= name.length(); ++pos) {
                        String invalid = withFF(name, pos, count);
                        _verifyRejected(v, newStreamFactory(), bpr, name, invalid, "\"");
                        _verifyRejected(v, APOS_FACTORY, bpr, name, invalid, "'");
                        if (pos > 0) {
                            _verifyRejected(v, UNQUOTED_FACTORY, bpr, name, invalid, "");
                        }
                    }
                }
            }
        }
    }

    @ParameterizedTest
    @EnumSource(Variant.class)
    void ffAfterSameNameInEarlierDocument(Variant v) throws Exception
    {
        for (int bpr : BYTES_PER_READ) {
            for (String name : NAMES) {
                JsonFactory f = newStreamFactory();
                try (AsyncReaderWrapper r = v.wrap(f, bpr, doc("{\"" + name + "\":1}"))) {
                    while (r.nextToken() != null) { }
                }
                try (AsyncReaderWrapper r = v.wrap(f, bpr,
                        doc("{\"" + withFF(name, name.length() - 1, 1) + "\":2}"))) {
                    assertToken(JsonToken.START_OBJECT, r.nextToken());
                    r.nextToken();
                    fail("Should not pass, got name '" + r.currentName() + "'");
                } catch (StreamReadException e) {
                    verifyException(e, "Invalid UTF-8 start byte 0xff");
                }
            }
        }
    }

    private void _verifyRejected(Variant v, JsonFactory f, int bpr, String name, String invalid, String q)
        throws Exception
    {
        try (AsyncReaderWrapper r = v.wrap(f, bpr, doc("{" + q + name + q + ":1," + q + invalid + q + ":2}"))) {
            assertToken(JsonToken.START_OBJECT, r.nextToken());
            assertToken(JsonToken.FIELD_NAME, r.nextToken());
            assertEquals(name, r.currentName());
            assertToken(JsonToken.VALUE_NUMBER_INT, r.nextToken());
            r.nextToken();
            fail("Should not pass, got name '" + r.currentName() + "'");
        } catch (StreamReadException e) {
            verifyException(e, "Invalid UTF-8 start byte 0xff");
        }
    }

    // '~' in result marks positions of 0xFF bytes, see doc()
    private static String withFF(String name, int pos, int count) {
        StringBuilder sb = new StringBuilder(name);
        for (int i = 0; i < count; ++i) {
            sb.insert(pos, '~');
        }
        return sb.toString();
    }

    private static byte[] doc(String json) {
        byte[] b = json.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        for (int i = 0; i < b.length; ++i) {
            if (b[i] == '~') {
                b[i] = (byte) 0xFF;
            }
        }
        return b;
    }
}
