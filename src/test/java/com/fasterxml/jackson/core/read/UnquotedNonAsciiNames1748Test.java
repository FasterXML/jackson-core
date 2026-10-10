package com.fasterxml.jackson.core.read;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        "\u00E9", "\u00E9t\u00E9", "a\u00E9b", "abc\u00E9", "\u00FCber_x",
        "\u4E2D\u6587", "x\u4E2D", "\u0430\u0431\u0432", "$\u00E9-1"
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

    // Boundary code points must still be accepted: 4-byte ones only in quoted
    // names, as supplementary chars are not valid in unquoted names
    @Test
    void validUtf8BoundariesInNames() throws Exception
    {
        List<String> docs = new ArrayList<>();
        for (String name : new String[] { "\u0080", "\u07FF", "\u0800", "\uFFDC" }) {
            docs.addAll(Arrays.asList(_docs(name)));
        }
        for (String name : new String[] { "\uD800\uDC00", "\uDBFF\uDFFF" }) {
            docs.add("{\"" + name + "\":1, \"" + name + "2\":true}");
        }
        for (String doc : docs) {
            final String name = _firstName(doc);
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

    // Non-ASCII chars that are not Java identifier parts must be rejected by
    // all parsers, as `ReaderBasedJsonParser` does: including supplementary
    // chars (checked as surrogate pair chars) and names already in symbol table
    @Test
    void nonIdentifierCharsInUnquotedNames() throws Exception
    {
        final String[] docs = {
            "{\u00D7:1}", "{a\u00D7b:1}", "{key\u00A0:1}", "{x\u3000:1}", "{a\u2028:1}",
            "{\uD83D\uDE00:1}", "{a\uD835\uDCB3:1}",
            "{\"a\u00D7b\":1, a\u00D7b:2}"
        };
        for (String doc : docs) {
            for (int mode : ALL_MODES) {
                try (JsonParser p = createParser(UNQUOTED_F, mode, doc)) {
                    _verifyRejected(p::nextToken, doc + ", mode " + mode);
                }
            }
            byte[] b = doc.getBytes(StandardCharsets.UTF_8);
            for (int bytesPerRead : new int[] { 1, 3, 100 }) {
                try (AsyncReaderWrapper p = AsyncTestBase.asyncForBytes(UNQUOTED_F, bytesPerRead, b, 0)) {
                    _verifyRejected(p::nextToken, doc + ", async");
                }
                try (AsyncReaderWrapper p = AsyncTestBase.asyncForByteBuffer(UNQUOTED_F, bytesPerRead, b, 0)) {
                    _verifyRejected(p::nextToken, doc + ", async ByteBuffer");
                }
            }
        }
    }

    private void _verifyRejected(IOSupplier<JsonToken> next, String desc)
        throws IOException
    {
        try {
            while (next.get() != null) { } // skip valid tokens
            fail("Should not pass: " + desc);
        } catch (JsonEOFException e) {
            fail("Should not report EOF (" + desc + "): " + e.getMessage());
        } catch (JsonParseException e) {
            verifyException(e, "Unexpected character (");
        }
    }

    @SuppressWarnings("serial")
    static class SymbolCountingFactory extends JsonFactory
    {
        public int byteSymbolCount() { return _byteSymbolCanonicalizer.size(); }

        @Override // needed for DataInput support
        public String getFormatName() { return FORMAT_NAME_JSON; }
    }

    // Rejected unquoted names must not be added to (root) symbol table
    @Test
    void invalidUnquotedNamesNotCanonicalized() throws Exception
    {
        final String doc = "{ok:1, a\u00D7b:2}";
        for (int mode : ALL_BINARY_MODES) {
            SymbolCountingFactory f = _symbolCountingFactory();
            try (JsonParser p = createParser(f, mode, doc)) {
                _verifyRejected(p::nextToken, doc + ", mode " + mode);
            }
            assertEquals(1, f.byteSymbolCount(), "mode " + mode);
        }
        byte[] b = doc.getBytes(StandardCharsets.UTF_8);
        for (int bytesPerRead : new int[] { 1, 100 }) {
            SymbolCountingFactory f = _symbolCountingFactory();
            try (AsyncReaderWrapper p = AsyncTestBase.asyncForBytes(f, bytesPerRead, b, 0)) {
                _verifyRejected(p::nextToken, doc + ", async");
            }
            assertEquals(1, f.byteSymbolCount(), "async");
            f = _symbolCountingFactory();
            try (AsyncReaderWrapper p = AsyncTestBase.asyncForByteBuffer(f, bytesPerRead, b, 0)) {
                _verifyRejected(p::nextToken, doc + ", async ByteBuffer");
            }
            assertEquals(1, f.byteSymbolCount(), "async ByteBuffer");
        }
    }

    @SuppressWarnings("deprecation")
    private static SymbolCountingFactory _symbolCountingFactory() {
        SymbolCountingFactory f = new SymbolCountingFactory();
        f.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);
        return f;
    }

    // Invalid middle byte must be reported as is, not with neighboring bytes
    @Test
    void invalidMiddleByteInName() throws Exception
    {
        _testInvalid(new int[] { 0xC3, 0x41 }, "Invalid UTF-8 middle byte 0x41");
        _testInvalid(new int[] { 0xE2, 0x82, 0x41 }, "Invalid UTF-8 middle byte 0x41");
        _testInvalid(new int[] { 0xF0, 0x9F, 0x98, 0x41 }, "Invalid UTF-8 middle byte 0x41");
    }

    // Async parsers must report decoded UTF-8 char if unquoted names not enabled
    @Test
    void nonAsciiNameStartNotAllowedAsync() throws Exception
    {
        final JsonFactory f = newStreamFactory();
        final String doc = "{\u00E9:1}";
        byte[] b = doc.getBytes(StandardCharsets.UTF_8);
        for (AsyncReaderWrapper p : new AsyncReaderWrapper[] {
                AsyncTestBase.asyncForBytes(f, 100, b, 0),
                AsyncTestBase.asyncForByteBuffer(f, 100, b, 0) }) {
            try {
                assertToken(JsonToken.START_OBJECT, p.nextToken());
                p.nextToken();
                fail("Should not pass: " + doc);
            } catch (JsonParseException e) {
                verifyException(e, "Unexpected character ('\u00E9' (code 233)");
            } finally {
                p.close();
            }
        }
    }

    // Async parsers must report decoded UTF-8 char for unexpected value,
    // with or without whitespace after separator
    @Test
    void nonAsciiUnexpectedValueAsync() throws Exception
    {
        final JsonFactory f = newStreamFactory();
        for (String doc : new String[] { "[\u00D7]", "[1,\u00D7]", "[1, \u00D7]", "{\"a\":\u00D7}", "{\"a\": \u00D7}" }) {
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
            verifyException(e, "Unexpected character ('\u00D7' (code 215");
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

    // Name of first property of `{name:...}` or `{"name":...}`
    private static String _firstName(String doc) {
        int start = (doc.charAt(1) == '"') ? 2 : 1;
        int end = doc.indexOf((start == 2) ? '"' : ':', start);
        return doc.substring(start, end);
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
