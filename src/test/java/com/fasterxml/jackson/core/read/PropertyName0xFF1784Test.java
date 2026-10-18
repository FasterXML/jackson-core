package com.fasterxml.jackson.core.read;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.json.JsonReadFeature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for [jackson-core#1784]: a property name containing 0xFF bytes must be
 * rejected even when it pads to the same quads as a name already in the symbol table.
 */
class PropertyName0xFF1784Test extends JUnit5TestBase
{
    private static final String[] NAMES = { "a", "ab", "abc", "abcd", "admin", "abcdef", "abcdefghi" };

    private final JsonFactory APOS_FACTORY = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .build();

    private final JsonFactory UNQUOTED_FACTORY = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
            .build();

    @Test
    void ffAfterSameNameInDocument() throws Exception
    {
        for (int mode : ALL_BINARY_MODES) {
            for (String name : NAMES) {
                for (int count = 1; count <= 3; ++count) {
                    for (int pos = 0; pos <= name.length(); ++pos) {
                        String invalid = withFF(name, pos, count);
                        _verifyRejected(newStreamFactory(), mode, name, invalid, "\"");
                        _verifyRejected(APOS_FACTORY, mode, name, invalid, "'");
                        if (pos > 0) {
                            _verifyRejected(UNQUOTED_FACTORY, mode, name, invalid, "");
                        }
                    }
                }
            }
        }
    }

    @Test
    void ffAfterSameNameInEarlierDocument() throws Exception
    {
        for (int mode : ALL_BINARY_MODES) {
            for (String name : NAMES) {
                JsonFactory f = newStreamFactory();
                try (JsonParser p = createParser(f, mode, "{\"" + name + "\":1}")) {
                    while (p.nextToken() != null) { }
                }
                try (JsonParser p = createParser(f, mode, doc("{\"" + withFF(name, name.length() - 1, 1) + "\":2}"))) {
                    assertToken(JsonToken.START_OBJECT, p.nextToken());
                    p.nextToken();
                    fail("Should not pass, got name '" + p.currentName() + "'");
                } catch (StreamReadException e) {
                    verifyException(e, "Invalid UTF-8 start byte 0xff");
                }
            }
        }
    }

    private void _verifyRejected(JsonFactory f, int mode, String name, String invalid, String q)
        throws Exception
    {
        try (JsonParser p = createParser(f, mode, doc("{" + q + name + q + ":1," + q + invalid + q + ":2}"))) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            assertEquals(name, p.currentName());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            p.nextToken();
            fail("Should not pass, got name '" + p.currentName() + "'");
        } catch (StreamReadException e) {
            verifyException(e, "Invalid UTF-8 start byte 0xff");
        }
    }

    // '~' in result marks positions of 0xFF bytes, see doc()
    static String withFF(String name, int pos, int count) {
        StringBuilder sb = new StringBuilder(name);
        for (int i = 0; i < count; ++i) {
            sb.insert(pos, '~');
        }
        return sb.toString();
    }

    static byte[] doc(String json) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (byte b : json.getBytes(StandardCharsets.US_ASCII)) {
            bytes.write((b == '~') ? 0xFF : b);
        }
        return bytes.toByteArray();
    }
}
