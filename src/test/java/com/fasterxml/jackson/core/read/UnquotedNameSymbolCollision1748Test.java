package com.fasterxml.jackson.core.read;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [core#1748]: Unquoted names must not share symbol table entries with quoted names
// that have leading `\u0000` escapes (see [core#148] for quoted names)
class UnquotedNameSymbolCollision1748Test extends JUnit5TestBase
{
    private final JsonFactory UNQUOTED_F = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
            .build();

    private final static String[][] CASES = {
        // doc, first name, second name
        { "{ab:1, \"\\u0000\\u0000ab\":2}", "ab", "\u0000\u0000ab" },
        { "{\"\\u0000\\u0000ab\":1, ab:2}", "\u0000\u0000ab", "ab" },
        { "{abcdef:1, \"abcd\\u0000\\u0000ef\":2}", "abcdef", "abcd\u0000\u0000ef" },
        { "{\"\\u0000\\u00E9\":1, \u00E9:2}", "\u0000\u00E9", "\u00E9" }
    };

    @Test
    void unquotedVsQuotedNames() throws Exception
    {
        for (String[] c : CASES) {
            for (int mode : ALL_MODES) {
                try (JsonParser p = createParser(UNQUOTED_F, mode, c[0])) {
                    _verify(p::nextToken, p::currentName, c, "mode " + mode);
                }
            }
            byte[] b = c[0].getBytes(StandardCharsets.UTF_8);
            for (int bytesPerRead : new int[] { 1, 3, 100 }) {
                try (AsyncReaderWrapper p = AsyncTestBase.asyncForBytes(UNQUOTED_F, bytesPerRead, b, 0)) {
                    _verify(p::nextToken, p::currentName, c, "async");
                }
                try (AsyncReaderWrapper p = AsyncTestBase.asyncForByteBuffer(UNQUOTED_F, bytesPerRead, b, 0)) {
                    _verify(p::nextToken, p::currentName, c, "async ByteBuffer");
                }
            }
        }
    }

    @FunctionalInterface
    interface IOSupplier<T> {
        T get() throws java.io.IOException;
    }

    private void _verify(IOSupplier<JsonToken> next, IOSupplier<String> currName,
            String[] c, String desc) throws Exception
    {
        assertToken(JsonToken.START_OBJECT, next.get());
        assertToken(JsonToken.FIELD_NAME, next.get());
        assertEquals(c[1], currName.get(), c[0] + ", " + desc);
        assertToken(JsonToken.VALUE_NUMBER_INT, next.get());
        assertToken(JsonToken.FIELD_NAME, next.get());
        assertEquals(c[2], currName.get(), c[0] + ", " + desc);
        assertToken(JsonToken.VALUE_NUMBER_INT, next.get());
        assertToken(JsonToken.END_OBJECT, next.get());
    }
}
