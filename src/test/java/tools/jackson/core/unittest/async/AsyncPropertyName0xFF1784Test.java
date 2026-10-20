package tools.jackson.core.unittest.async;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.unittest.testutil.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for [jackson-core#1784]: non-blocking variant of
 * {@code tools.jackson.core.unittest.read.PropertyName0xFF1784Test}.
 */
class AsyncPropertyName0xFF1784Test extends AsyncTestBase
{
    enum Variant {
        BYTE_ARRAY, BYTE_BUFFER;

        AsyncReaderWrapper wrap(JsonFactory f, int bytesPerRead, byte[] data)
        {
            switch (this) {
            case BYTE_ARRAY: return asyncForBytes(f, bytesPerRead, data, 0);
            case BYTE_BUFFER: return asyncForByteBuffer(f, bytesPerRead, data, 0);
            default: throw new IllegalStateException();
            }
        }
    }

    // Lengths 1, 3, 4, 6 and 9 give last quads of 1, 3, 4, 2 and 1 bytes
    private static final String[] NAMES = { "a", "abc", "abcd", "abcdef", "abcdefghi" };

    private static final int[] BYTES_PER_READ = { 1, 2, 3, 1000 };

    private final JsonFactory APOS_FACTORY = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .build();

    private final JsonFactory UNQUOTED_FACTORY = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_UNQUOTED_PROPERTY_NAMES)
            .build();

    @ParameterizedTest
    @EnumSource(Variant.class)
    void ffAfterSameNameInDocument(Variant v) throws Exception
    {
        for (int bpr : BYTES_PER_READ) {
            for (String name : NAMES) {
                for (int pos = 0; pos <= name.length(); ++pos) {
                    String invalid = withFF(name, pos);
                    _verifyRejected(v, newStreamFactory(), bpr, name, invalid, "\"");
                    _verifyRejected(v, APOS_FACTORY, bpr, name, invalid, "'");
                    if (pos > 0) { // unquoted name cannot start with 0xFF
                        _verifyRejected(v, UNQUOTED_FACTORY, bpr, name, invalid, "");
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
                        doc("{\"" + withFF(name, name.length() - 1) + "\":2}"))) {
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
            assertToken(JsonToken.PROPERTY_NAME, r.nextToken());
            assertEquals(name, r.currentName());
            assertToken(JsonToken.VALUE_NUMBER_INT, r.nextToken());
            r.nextToken();
            fail("Should not pass, got name '" + r.currentName() + "'");
        } catch (StreamReadException e) {
            verifyException(e, "Invalid UTF-8 start byte 0xff");
        }
    }

    // '~' in result marks the 0xFF byte, see doc()
    private static String withFF(String name, int pos) {
        return new StringBuilder(name).insert(pos, '~').toString();
    }

    private static byte[] doc(String json) {
        return json.replace('~', 'ÿ').getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
    }
}
