package com.fasterxml.jackson.core.constraints;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
