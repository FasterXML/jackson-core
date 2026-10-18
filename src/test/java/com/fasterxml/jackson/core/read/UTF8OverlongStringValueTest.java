package com.fasterxml.jackson.core.read;

import java.io.ByteArrayOutputStream;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.json.JsonReadFeature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests that byte-based parsers reject overlong (non-shortest form) 2- and 3-byte
 * UTF-8 sequences in String values, instead of decoding them.
 */
class UTF8OverlongStringValueTest extends JUnit5TestBase
{
    private final JsonFactory FACTORY = newStreamFactory();

    private final JsonFactory APOS_FACTORY = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .build();

    private final JsonFactory COMMENTS_FACTORY = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
            .enable(JsonReadFeature.ALLOW_YAML_COMMENTS)
            .build();

    static final int[][] OVERLONG = {
        { 0xC0, 0x80 }, // U+0000
        { 0xC0, 0xAF }, // '/'
        { 0xC1, 0xA1 }, // 'a'
        { 0xC1, 0xBF }, // U+007F
        { 0xE0, 0x80, 0x80 }, // U+0000
        { 0xE0, 0x80, 0xAF }, // '/'
        { 0xE0, 0x9F, 0xBF }, // U+07FF
    };

    // Shortest-form boundary characters: must still be accepted
    static final int[][] VALID = {
        { 0xC2, 0x80 }, // U+0080
        { 0xDF, 0xBF }, // U+07FF
        { 0xE0, 0xA0, 0x80 }, // U+0800
        { 0xEF, 0xBF, 0xBF }, // U+FFFF
    };

    static final String[] VALID_DECODED = { "\u0080", "߿", "ࠀ", "￿" };

    // Long prefix so that sequence is not in first input buffer
    private static final String LONG_PREFIX = String.join("", java.util.Collections.nCopies(9000, "x"));

    @Test
    void rejectOverlongInValue() throws Exception
    {
        for (int mode : ALL_BINARY_MODES) {
            for (String prefix : new String[] { "", LONG_PREFIX }) {
                for (int[] seq : OVERLONG) {
                    byte[] doc = doc("{\"a\":\"" + prefix, seq, "\"}");
                    try (JsonParser p = createParser(FACTORY, mode, doc)) {
                        assertToken(JsonToken.START_OBJECT, p.nextToken());
                        assertToken(JsonToken.FIELD_NAME, p.nextToken());
                        assertToken(JsonToken.VALUE_STRING, p.nextToken());
                        p.getText();
                        fail("Should not pass for mode " + mode + ", sequence " + hex(seq));
                    } catch (StreamReadException e) {
                        verifyException(e, "Invalid UTF-8: overlong " + seq.length + "-byte encoding");
                    }
                }
            }
        }
    }

    @Test
    void rejectOverlongInSkippedValue() throws Exception
    {
        for (int mode : ALL_BINARY_MODES) {
            for (String prefix : new String[] { "", LONG_PREFIX }) {
                for (int[] seq : OVERLONG) {
                    byte[] doc = doc("[\"" + prefix, seq, "\",1]");
                    try (JsonParser p = createParser(FACTORY, mode, doc)) {
                        assertToken(JsonToken.START_ARRAY, p.nextToken());
                        assertToken(JsonToken.VALUE_STRING, p.nextToken());
                        // skip String value without accessing it
                        p.nextToken();
                        fail("Should not pass for mode " + mode + ", sequence " + hex(seq));
                    } catch (StreamReadException e) {
                        verifyException(e, "Invalid UTF-8: overlong " + seq.length + "-byte encoding");
                    }
                }
            }
        }
    }

    @Test
    void rejectOverlongInAposValue() throws Exception
    {
        for (int mode : ALL_BINARY_MODES) {
            for (int[] seq : OVERLONG) {
                byte[] doc = doc("{\"a\":'", seq, "'}");
                try (JsonParser p = createParser(APOS_FACTORY, mode, doc)) {
                    assertToken(JsonToken.START_OBJECT, p.nextToken());
                    assertToken(JsonToken.FIELD_NAME, p.nextToken());
                    assertToken(JsonToken.VALUE_STRING, p.nextToken());
                    p.getText();
                    fail("Should not pass for mode " + mode + ", sequence " + hex(seq));
                } catch (StreamReadException e) {
                    verifyException(e, "Invalid UTF-8: overlong " + seq.length + "-byte encoding");
                }
            }
        }
    }

    @Test
    void rejectOverlongInComment() throws Exception
    {
        for (int mode : ALL_BINARY_MODES) {
            for (int[] seq : OVERLONG) {
                for (String[] comment : new String[][] {
                        { "/* ", " */" }, { "// ", "\n" }, { "# ", "\n" } }) {
                    byte[] doc = doc("[" + comment[0], seq, comment[1] + "1]");
                    try (JsonParser p = createParser(COMMENTS_FACTORY, mode, doc)) {
                        assertToken(JsonToken.START_ARRAY, p.nextToken());
                        p.nextToken();
                        fail("Should not pass for mode " + mode + ", sequence " + hex(seq));
                    } catch (StreamReadException e) {
                        verifyException(e, "Invalid UTF-8: overlong " + seq.length + "-byte encoding");
                    }
                }
            }
        }
    }

    @Test
    void acceptShortestFormBoundaries() throws Exception
    {
        for (int mode : ALL_BINARY_MODES) {
            for (int i = 0; i < VALID.length; ++i) {
                byte[] doc = doc("[\"", VALID[i], "\",\"", VALID[i], "\"]");
                try (JsonParser p = createParser(FACTORY, mode, doc)) {
                    assertToken(JsonToken.START_ARRAY, p.nextToken());
                    assertToken(JsonToken.VALUE_STRING, p.nextToken());
                    assertEquals(VALID_DECODED[i], p.getText());
                    // second one skipped
                    assertToken(JsonToken.VALUE_STRING, p.nextToken());
                    assertToken(JsonToken.END_ARRAY, p.nextToken());
                }
            }
        }
    }

    static byte[] doc(Object... parts) {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        for (Object part : parts) {
            if (part instanceof String) {
                byte[] bytes = ((String) part).getBytes(java.nio.charset.StandardCharsets.UTF_8);
                b.write(bytes, 0, bytes.length);
            } else {
                for (int x : (int[]) part) {
                    b.write(x);
                }
            }
        }
        return b.toByteArray();
    }

    static String hex(int[] seq) {
        StringBuilder sb = new StringBuilder();
        for (int x : seq) {
            sb.append(String.format("%02X ", x));
        }
        return sb.toString().trim();
    }
}
