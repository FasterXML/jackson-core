package com.fasterxml.jackson.core.read;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.io.JsonEOFException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

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

    // Invalid UTF-8 in names must be rejected, quoted or not
    @Test
    void invalidUtf8InNames() throws Exception
    {
        _testInvalid(new int[] { 0xC0, 0xBA }, "overlong 2-byte");
        _testInvalid(new int[] { 0xC1, 0xBF }, "overlong 2-byte");
        _testInvalid(new int[] { 0xE0, 0x80, 0xBA }, "overlong 3-byte");
        _testInvalid(new int[] { 0xF0, 0x80, 0x80, 0xBA }, "overlong 4-byte");
        _testInvalid(new int[] { 0xF4, 0x90, 0x80, 0x80 }, "beyond U+10FFFF");
        _testInvalid(new int[] { 0xF7, 0xBF, 0xBF, 0xBF }, "beyond U+10FFFF");
        _testInvalid(new int[] { 0xC3 }, "incomplete multi-byte sequence");
        _testInvalid(new int[] { 0xE2, 0x82 }, "incomplete multi-byte sequence");
    }

    // Boundary code points must still be accepted
    @Test
    void validUtf8BoundariesInNames() throws Exception
    {
        for (String name : new String[] { "\u0080", "\u07FF", "\u0800", "\uFFFD",
                "\uD800\uDC00", "\uDBFF\uDFFF" }) {
            for (String doc : _docs(name)) {
                for (int mode : ALL_BINARY_MODES) {
                    try (JsonParser p = createParser(UNQUOTED_F, mode, doc)) {
                        _verify(p::nextToken, p::currentName, name, doc);
                    }
                }
                byte[] b = doc.getBytes(StandardCharsets.UTF_8);
                for (int bytesPerRead : new int[] { 1, 2, 3, 100 }) {
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

    // Async parsers must report decoded UTF-8 char for unexpected value,
    // with or without whitespace after separator
    @Test
    void nonAsciiUnexpectedValueAsync() throws Exception
    {
        final JsonFactory f = newStreamFactory();
        for (String doc : new String[] { "[×]", "[1,×]", "[1, ×]", "{\"a\":×}", "{\"a\": ×}" }) {
            byte[] b = doc.getBytes(StandardCharsets.UTF_8);
            try (AsyncReaderWrapper p = AsyncTestBase.asyncForBytes(f, 100, b, 0)) {
                _verifyUnexpectedValue(p::nextToken, doc);
            }
            try (AsyncReaderWrapper p = AsyncTestBase.asyncForByteBuffer(f, 100, b, 0)) {
                _verifyUnexpectedValue(p::nextToken, doc);
            }
        }
    }

    private void _verifyUnexpectedValue(IOSupplier<JsonToken> next, String doc)
        throws IOException
    {
        try {
            while (next.get() != null) { } // skip valid tokens
            fail("Should not pass: " + doc);
        } catch (JsonParseException e) {
            verifyException(e, "Unexpected character ('×' (code 215");
        }
    }

    private void _testInvalid(int[] seq, String expMsg) throws Exception
    {
        for (boolean quoted : new boolean[] { false, true }) {
            byte[] doc = _invalidDoc(seq, quoted);
            String desc = expMsg + (quoted ? " (quoted)" : " (unquoted)");
            for (int mode : ALL_BINARY_MODES) {
                try (JsonParser p = createParser(UNQUOTED_F, mode, doc)) {
                    _verifyInvalid(p::nextToken, expMsg, desc + ", mode " + mode);
                }
            }
            for (int bytesPerRead : new int[] { 1, 3, 100 }) {
                try (AsyncReaderWrapper p = AsyncTestBase.asyncForBytes(UNQUOTED_F, bytesPerRead, doc, 0)) {
                    _verifyInvalid(p::nextToken, expMsg, desc + ", async");
                }
                try (AsyncReaderWrapper p = AsyncTestBase.asyncForByteBuffer(UNQUOTED_F, bytesPerRead, doc, 0)) {
                    _verifyInvalid(p::nextToken, expMsg, desc + ", async ByteBuffer");
                }
            }
        }
    }

    private void _verifyInvalid(IOSupplier<JsonToken> next, String expMsg, String desc)
        throws IOException
    {
        assertToken(JsonToken.START_OBJECT, next.get());
        try {
            JsonToken t = next.get();
            fail("Should not pass (" + desc + "), got " + t);
        } catch (JsonEOFException e) {
            fail("Should not report EOF (" + desc + "): " + e.getMessage());
        } catch (JsonParseException e) {
            verifyException(e, expMsg);
        }
    }

    // `{a<seq>:1}` or `{"a<seq>":1}`: sequence ends the name
    private static byte[] _invalidDoc(int[] seq, boolean quoted)
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write('{');
        if (quoted) {
            out.write('"');
        }
        out.write('a');
        for (int b : seq) {
            out.write(b);
        }
        if (quoted) {
            out.write('"');
        }
        out.write(':');
        out.write('1');
        out.write('}');
        return out.toByteArray();
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
