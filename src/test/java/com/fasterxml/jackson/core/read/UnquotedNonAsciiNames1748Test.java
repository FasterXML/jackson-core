package com.fasterxml.jackson.core.read;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for [jackson-core#1748]: unquoted property names with non-ASCII
 * characters must be accepted by the UTF-8 byte-based parsers too.
 */
class UnquotedNonAsciiNames1748Test extends JUnit5TestBase
{
    private final JsonFactory UNQUOTED_F = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
            .build();

    private final static String[] NAMES = {
        "é", "été", "aéb", "abcé", "über_x",
        "中文", "x中", "абв", "$é-1"
    };

    @Test
    void unquotedNonAsciiNames() throws Exception
    {
        for (int mode : ALL_MODES) {
            for (String name : NAMES) {
                for (String doc : _docs(name)) {
                    try (JsonParser p = createParser(UNQUOTED_F, mode, doc)) {
                        _verify(p::nextToken, p::currentName, name, doc);
                    }
                }
            }
        }
    }

    @Test
    void unquotedNonAsciiNamesAsync() throws Exception
    {
        for (int bytesPerRead : new int[] { 1, 2, 3, 100 }) {
            for (String name : NAMES) {
                for (String doc : _docs(name)) {
                    byte[] b = doc.getBytes(StandardCharsets.UTF_8);
                    try (AsyncReaderWrapper p = AsyncTestBase.asyncForBytes(UNQUOTED_F, bytesPerRead, b, 0)) {
                        _verify(p::nextToken, p::currentName, name, doc);
                    }
                    try (AsyncReaderWrapper p = AsyncTestBase.asyncForByteBuffer(UNQUOTED_F, bytesPerRead, b, 0)) {
                        _verify(p::nextToken, p::currentName, name, doc);
                    }
                }
            }
        }
    }

    @FunctionalInterface
    interface IOSupplier<T> {
        T get() throws IOException;
    }

    private void _verify(IOSupplier<JsonToken> next, IOSupplier<String> currName,
            String name, String doc) throws IOException
    {
        assertToken(JsonToken.START_OBJECT, next.get());
        assertToken(JsonToken.FIELD_NAME, next.get());
        assertEquals(name, currName.get(), doc);
        assertToken(JsonToken.VALUE_NUMBER_INT, next.get());
        assertToken(JsonToken.FIELD_NAME, next.get());
        assertEquals(name + "2", currName.get(), doc);
        assertToken(JsonToken.VALUE_TRUE, next.get());
        assertToken(JsonToken.END_OBJECT, next.get());
    }

    // Both with and without whitespace after separators (async parsers
    // have separate code paths for these)
    private static String[] _docs(String name) {
        return new String[] {
            "{" + name + ":1, " + name + "2 :true}",
            "{" + name + ":1," + name + "2:true}"
        };
    }
}
