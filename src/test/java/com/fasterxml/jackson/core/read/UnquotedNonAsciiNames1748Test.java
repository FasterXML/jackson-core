package com.fasterxml.jackson.core.read;

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
                _testName(mode, name);
            }
        }
    }

    @Test
    void unquotedNonAsciiNamesAsync() throws Exception
    {
        for (int bytesPerRead : new int[] { 1, 2, 3, 100 }) {
            for (String name : NAMES) {
                byte[] doc = _doc(name).getBytes(StandardCharsets.UTF_8);
                _verify(AsyncTestBase.asyncForBytes(UNQUOTED_F, bytesPerRead, doc, 0), name);
                _verify(AsyncTestBase.asyncForByteBuffer(UNQUOTED_F, bytesPerRead, doc, 0), name);
            }
        }
    }

    private void _testName(int mode, String name) throws Exception
    {
        try (JsonParser p = createParser(UNQUOTED_F, mode, _doc(name))) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            assertEquals(name, p.currentName(), "mode " + mode);
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            assertEquals(name + "2", p.currentName(), "mode " + mode);
            assertToken(JsonToken.VALUE_TRUE, p.nextToken());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }
    }

    private void _verify(AsyncReaderWrapper p, String name) throws Exception
    {
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals(name, p.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals(name + "2", p.currentName());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        p.close();
    }

    private static String _doc(String name) {
        return "{" + name + ":1, " + name + "2 :true}";
    }
}
