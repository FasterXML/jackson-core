package com.fasterxml.jackson.core.constraints;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    // Limit must be exact, not off by up to a quartet
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
