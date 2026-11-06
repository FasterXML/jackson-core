package tools.jackson.core.unittest.constraints;

import java.io.StringWriter;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.unittest.async.AsyncTestBase;
import tools.jackson.core.unittest.testutil.AsyncReaderWrapper;
import tools.jackson.core.util.JsonRecyclerPools;

import static org.junit.jupiter.api.Assertions.*;

class StringWriterReadConstraintsTest extends AsyncTestBase
{
    // Fits in a single TextBuffer segment and parser input buffer
    private final static int SHORT_LIMIT = 100;

    // Exceeds parser input buffers so content spans multiple TextBuffer segments
    private final static int LONG_LIMIT = 10_000;

    // Async chunk sizes: byte-by-byte, small odd chunks, whole document at once
    private final static int[] ASYNC_BYTES_PER_READ = { 1, 7, Integer.MAX_VALUE };

    @FunctionalInterface
    interface ParserCheck {
        void check(AsyncReaderWrapper p) throws Exception;
    }

    @Test
    void stringsWithinLimit() throws Exception {
        for (int limit : new int[] { SHORT_LIMIT, LONG_LIMIT }) {
            JsonFactory f = factory(limit);
            for (String value : new String[] { repeat('x', limit - 1), repeat('x', limit) }) {
                forAllParsers(f, q(value), p -> {
                    assertToken(JsonToken.VALUE_STRING, p.nextToken());
                    assertEquals(value, p.currentTextViaWriter());
                    assertEquals(value, p.currentText());
                });
            }
        }
    }

    @Test
    void oversizedStringsRejectedBeforeWriting() throws Exception {
        for (int limit : new int[] { SHORT_LIMIT, LONG_LIMIT }) {
            JsonFactory f = factory(limit);
            // Just one char over: segments completed during nextToken() stay within limit
            int len = limit + 1;
            for (String doc : new String[] {
                    q(repeat('x', len)), "\"\\u0078" + repeat('x', len - 1) + "\"" }) {
                forAllParsers(f, doc, p -> {
                    assertToken(JsonToken.VALUE_STRING, p.nextToken());
                    StringWriter w = new StringWriter();
                    assertThrows(StreamConstraintsException.class, () -> p.parser().getString(w));
                    assertEquals("", w.toString());
                });
            }
        }
    }

    @Test
    void namesExemptButNumbersCheckedLikeGetString() throws Exception {
        JsonFactory f = factory(19);
        String name = repeat('a', 20);
        // Escape forces Reader name decoding through TextBuffer.getTextBuffer().
        String doc = "{\"\\u0061" + name.substring(1) + "\":-1234567890123456789}";
        forAllParsers(f, doc, p -> {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            StringWriter w = new StringWriter();
            assertEquals(20, p.parser().getString(w));
            assertEquals(name, w.toString());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(-1234567890123456789L, p.getLongValue());
            // 20-char number text exceeds maxStringLength: rejected same as getString()
            w.getBuffer().setLength(0);
            assertThrows(StreamConstraintsException.class, () -> p.parser().getString(w));
            assertEquals("", w.toString());
            assertThrows(StreamConstraintsException.class, () -> p.currentText());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
        });
    }

    private void forAllParsers(JsonFactory f, String doc, ParserCheck check) throws Exception {
        for (int mode : ALL_MODES) {
            try (AsyncReaderWrapper p = syncWrapper(createParser(f, mode, doc))) {
                runCheck("sync mode " + mode, p, check);
            }
        }
        byte[] input = utf8Bytes(doc);
        for (int bytesPerRead : ASYNC_BYTES_PER_READ) {
            try (AsyncReaderWrapper p = asyncForBytes(f, bytesPerRead, input, 0)) {
                runCheck("async byte[], bytesPerRead " + bytesPerRead, p, check);
            }
            try (AsyncReaderWrapper p = asyncForByteBuffer(f, bytesPerRead, input, 0)) {
                runCheck("async ByteBuffer, bytesPerRead " + bytesPerRead, p, check);
            }
        }
    }

    private void runCheck(String desc, AsyncReaderWrapper p, ParserCheck check) throws Exception {
        try {
            check.check(p);
        } catch (AssertionError | Exception e) {
            throw new AssertionError("Failed for " + desc + ": " + e, e);
        }
    }

    // Lets blocking parsers share checks with async ones
    private AsyncReaderWrapper syncWrapper(JsonParser p) {
        return new AsyncReaderWrapper(p) {
            @Override
            public JsonToken nextToken() {
                return _streamReader.nextToken();
            }
        };
    }

    private String repeat(char ch, int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, ch);
        return new String(chars);
    }

    private JsonFactory factory(int limit) {
        return JsonFactory.builder()
                .recyclerPool(JsonRecyclerPools.nonRecyclingPool())
                .streamReadConstraints(StreamReadConstraints.builder()
                        .maxStringLength(limit).maxNumberLength(limit).build())
                .build();
    }
}
