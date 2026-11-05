package com.fasterxml.jackson.core.constraints;

import java.io.ByteArrayInputStream;
import java.io.StringReader;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.core.filter.FilteringParserDelegate;
import com.fasterxml.jackson.core.filter.TokenFilter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Tests to verify that `maxStringLength` is applied when a String value is
// decoded straight from input as Base64 (`getBinaryValue()`), without
// being accessed as a String first
class LargeBinaryValueReadTest extends JUnit5TestBase
{
    private final static int MAX_STRING_LEN = 1000;

    private final JsonFactory JSON_F_STRING_1000 = JsonFactory.builder()
            .streamReadConstraints(StreamReadConstraints.builder()
                    .maxStringLength(MAX_STRING_LEN).build())
            .build();

    @Test
    void largeBinaryWithSmallLimit() throws Exception
    {
        // 30k bytes -> 40k Base64 characters, well above limit
        final String doc = _doc(_binary(30_000));
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(JSON_F_STRING_1000, mode, doc)) {
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                assertToken(JsonToken.VALUE_STRING, p.nextToken());
                StreamConstraintsException e = assertThrows(StreamConstraintsException.class,
                        () -> p.getBinaryValue());
                verifyException(e, "String value length");
                verifyException(e, "exceeds the maximum allowed");
            }
        }
    }

    @Test
    void binaryAtLimit() throws Exception
    {
        // 750 bytes -> exactly 1000 Base64 characters: must still be decodable
        final byte[] binary = _binary(750);
        final String doc = _doc(binary);
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(JSON_F_STRING_1000, mode, doc)) {
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                assertToken(JsonToken.VALUE_STRING, p.nextToken());
                assertArrayEquals(binary, p.getBinaryValue());
                assertToken(JsonToken.END_ARRAY, p.nextToken());
            }
        }
    }

    // Limit must not be off by up to a quartet (padded values, however, can get
    // up to 2 characters over: length is derived from decoded length)
    @Test
    void binaryJustOverLimit() throws Exception
    {
        // 753 bytes -> 1004 characters; unpadded 751, 752 -> 1002, 1003
        final Base64Variant noPadding = Base64Variants.MODIFIED_FOR_URL;
        final String[] values = {
                Base64Variants.MIME_NO_LINEFEEDS.encode(_binary(753)),
                noPadding.encode(_binary(751)),
                noPadding.encode(_binary(752))
        };
        final Base64Variant[] variants = { Base64Variants.MIME_NO_LINEFEEDS, noPadding, noPadding };
        for (int i = 0; i < values.length; ++i) {
            final Base64Variant variant = variants[i];
            final String doc = "[\"" + values[i] + "\"]";
            for (int mode : ALL_MODES) {
                try (JsonParser p = createParser(JSON_F_STRING_1000, mode, doc)) {
                    assertToken(JsonToken.START_ARRAY, p.nextToken());
                    assertToken(JsonToken.VALUE_STRING, p.nextToken());
                    StreamConstraintsException e = assertThrows(StreamConstraintsException.class,
                            () -> p.getBinaryValue(variant));
                    verifyException(e, "exceeds the maximum allowed");
                }
            }
        }
    }

    @Test
    void binaryWithLimitNotMultipleOfFour() throws Exception
    {
        final JsonFactory f = JsonFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder()
                        .maxStringLength(1002).build())
                .build();
        final Base64Variant noPadding = Base64Variants.MODIFIED_FOR_URL;
        final byte[] atLimit = _binary(751); // 1002 characters
        final String okDoc = "[\"" + noPadding.encode(atLimit) + "\"]";
        final String tooLongDoc = "[\"" + noPadding.encode(_binary(752)) + "\"]"; // 1003
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(f, mode, okDoc)) {
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                assertToken(JsonToken.VALUE_STRING, p.nextToken());
                assertArrayEquals(atLimit, p.getBinaryValue(noPadding));
            }
            try (JsonParser p = createParser(f, mode, tooLongDoc)) {
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                assertToken(JsonToken.VALUE_STRING, p.nextToken());
                assertThrows(StreamConstraintsException.class, () -> p.getBinaryValue(noPadding));
            }
        }
    }

    @Test
    void binaryWithZeroLimit() throws Exception
    {
        final JsonFactory f = JsonFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder()
                        .maxStringLength(0).build())
                .build();
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(f, mode, "[\"\",\"QUJD\"]")) {
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                assertToken(JsonToken.VALUE_STRING, p.nextToken());
                assertArrayEquals(new byte[0], p.getBinaryValue());
                assertToken(JsonToken.VALUE_STRING, p.nextToken());
                assertThrows(StreamConstraintsException.class, () -> p.getBinaryValue());
            }
        }
    }

    // White space does not end up in decoded content, is not counted
    @Test
    void binaryAtLimitWithWhitespace() throws Exception
    {
        final byte[] binary = _binary(750);
        final String encoded = Base64Variants.MIME_NO_LINEFEEDS.encode(binary);
        final StringBuilder sb = new StringBuilder("[\"");
        for (int i = 0; i < encoded.length(); i += 4) {
            sb.append(encoded, i, i + 4).append("     ");
        }
        final String doc = sb.append("\"]").toString();
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(JSON_F_STRING_1000, mode, doc)) {
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                assertToken(JsonToken.VALUE_STRING, p.nextToken());
                assertArrayEquals(binary, p.getBinaryValue(Base64Variants.MIME_NO_LINEFEEDS));
                assertToken(JsonToken.END_ARRAY, p.nextToken());
            }
        }
    }

    // After failure neither retry nor text access may expose rest of the value, and
    // parsing has to resume after it. 30k bytes fails mid-value for most input
    // sources; 751 only once the whole value is decoded (closing quote consumed)
    @Test
    void accessAfterFailure() throws Exception
    {
        for (int len : new int[] { 751, 30_000 }) {
            final String doc = "[\"" + Base64Variants.getDefaultVariant().encode(_binary(len)) + "\",123]";
            for (int mode : ALL_MODES) {
                try (JsonParser p = createParser(JSON_F_STRING_1000, mode, doc)) {
                    assertToken(JsonToken.START_ARRAY, p.nextToken());
                    assertToken(JsonToken.VALUE_STRING, p.nextToken());
                    assertThrows(StreamConstraintsException.class, () -> p.getBinaryValue());

                    assertNull(p.currentToken());
                    assertNull(p.getText());
                    JsonParseException e = assertThrows(JsonParseException.class,
                            () -> p.getBinaryValue());
                    verifyException(e, "not VALUE_STRING");

                    assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                    assertEquals(123, p.getIntValue());
                    assertToken(JsonToken.END_ARRAY, p.nextToken());
                }
            }
        }
    }

    @Test
    void valueWithinLimitAccessibleAsTextAfterBinary() throws Exception
    {
        final byte[] binary = _binary(300);
        final String encoded = Base64Variants.getDefaultVariant().encode(binary);
        final String doc = "[\"" + encoded + "\",\"" + encoded + "\"]";
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(JSON_F_STRING_1000, mode, doc)) {
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                assertToken(JsonToken.VALUE_STRING, p.nextToken());
                assertArrayEquals(binary, p.getBinaryValue());
                // and a second access is served from what was decoded
                assertArrayEquals(binary, p.getBinaryValue());
                assertToken(JsonToken.VALUE_STRING, p.nextToken());
                assertEquals(encoded, p.getText());
                assertArrayEquals(binary, p.getBinaryValue());
                assertToken(JsonToken.END_ARRAY, p.nextToken());
            }
        }
    }

    // Limit must fire well before end of value: invalid character at the end would
    // otherwise be hit first. In-memory input has no input buffer reloads to check on.
    @Test
    void failsBeforeEndOfValue() throws Exception
    {
        final String doc = "[\"" + Base64Variants.getDefaultVariant().encode(_binary(30_000)) + "!\"]";
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(JSON_F_STRING_1000, mode, doc)) {
                _verifyFailsOnLimit(p);
            }
        }
        try (JsonParser p = JSON_F_STRING_1000.createParser(doc.getBytes("UTF-8"))) {
            _verifyFailsOnLimit(p);
        }
        try (JsonParser p = JSON_F_STRING_1000.createParser(doc.toCharArray())) {
            _verifyFailsOnLimit(p);
        }
    }

    private void _verifyFailsOnLimit(JsonParser p) throws Exception {
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        StreamConstraintsException e = assertThrows(StreamConstraintsException.class,
                () -> p.getBinaryValue());
        verifyException(e, "String value length (at least ");
        verifyException(e, "exceeds the maximum allowed (1000");
    }

    // Input reload within an escape must not prevent checks: backslash placed as the
    // last character of every input buffer (default sizes of 8000 bytes, 4000 chars),
    // so that no reload happens outside of escape decoding
    @Test
    void failsWithEscapesStraddlingBuffers() throws Exception
    {
        final String byteDoc = _docWithEscapesAtBoundaries(8000);
        try (JsonParser p = JSON_F_STRING_1000.createParser(
                new ByteArrayInputStream(byteDoc.getBytes("UTF-8")))) {
            _verifyFailsOnLimit(p);
        }
        final String charDoc = _docWithEscapesAtBoundaries(4000);
        try (JsonParser p = JSON_F_STRING_1000.createParser(new StringReader(charDoc))) {
            _verifyFailsOnLimit(p);
        }
    }

    // Invalid character at the end, to fail if limit is not hit before it
    private static String _docWithEscapesAtBoundaries(int bufferLen) {
        final StringBuilder sb = new StringBuilder("[\"");
        while (sb.length() < (bufferLen * 20)) {
            final int boundary = ((sb.length() / bufferLen) + 1) * bufferLen;
            while (sb.length() < (boundary - 1)) {
                sb.append('A');
            }
            sb.append("\\u0041");
        }
        return sb.append("!\"]").toString();
    }

    // Decoding of an escape may reload input (buffers of varying size with throttled
    // modes), which must not confuse tracking of where to check next
    @Test
    void binaryWithEscapesAcrossBuffers() throws Exception
    {
        final byte[] binary = _binary(100_000);
        final String encoded = Base64Variants.getDefaultVariant().encode(binary);
        final StringBuilder sb = new StringBuilder("[\"");
        final int last = encoded.length() - 1;
        for (int i = 0; i <= last; ++i) {
            // to get all positions within quartets; and escaped padding at the end
            if ((i % 7) == 0 || i == last) {
                sb.append(String.format("\\u%04X", (int) encoded.charAt(i)));
            } else {
                sb.append(encoded.charAt(i));
            }
        }
        final String doc = sb.append("\"]").toString();
        final JsonFactory f = new JsonFactory();
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(f, mode, doc)) {
                _verifyBinaryRead(p, binary);
            }
        }
        try (JsonParser p = f.createParser(doc.getBytes("UTF-8"))) {
            _verifyBinaryRead(p, binary);
        }
        try (JsonParser p = f.createParser(doc.toCharArray())) {
            _verifyBinaryRead(p, binary);
        }
    }

    private void _verifyBinaryRead(JsonParser p, byte[] expected) throws Exception {
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertArrayEquals(expected, p.getBinaryValue());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
    }

    @Test
    void filteredParserAfterFailure() throws Exception
    {
        final String doc = _doc(_binary(30_000));
        for (int mode : ALL_MODES) {
            try (JsonParser p = new FilteringParserDelegate(
                    createParser(JSON_F_STRING_1000, mode, doc),
                    TokenFilter.INCLUDE_ALL, TokenFilter.Inclusion.INCLUDE_ALL_AND_PATH, true)) {
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                assertToken(JsonToken.VALUE_STRING, p.nextToken());
                assertThrows(StreamConstraintsException.class, () -> p.getBinaryValue());
                assertNull(p.currentToken());
                assertFalse(p.hasCurrentToken());
                assertNull(p.getText());
                assertThrows(JsonParseException.class, () -> p.getBinaryValue());
                assertToken(JsonToken.END_ARRAY, p.nextToken());
            }
        }
    }

    private static String _doc(byte[] binary) {
        return "[\"" + Base64Variants.getDefaultVariant().encode(binary) + "\"]";
    }

    private static byte[] _binary(int len) {
        final byte[] b = new byte[len];
        for (int i = 0; i < len; ++i) {
            b[i] = (byte) i;
        }
        return b;
    }
}
