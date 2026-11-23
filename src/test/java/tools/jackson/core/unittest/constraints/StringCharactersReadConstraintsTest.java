package tools.jackson.core.unittest.constraints;

import java.io.StringWriter;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.unittest.async.AsyncTestBase;
import tools.jackson.core.unittest.testutil.AsyncReaderWrapper;
import tools.jackson.core.util.BufferRecycler;
import tools.jackson.core.util.JsonRecyclerPools;
import tools.jackson.core.util.RecyclerPool;

import static org.junit.jupiter.api.Assertions.*;

// Tests to verify that `maxStringLength` is applied by `getStringCharacters()`
// (and so by `JsonGenerator.copyCurrentEvent()`) same as by `getString()`:
// Reader-based parser may share its input buffer (String / char[] input) and
// any parser may hold the value in a single recycled segment, neither of
// which goes through `TextBuffer` size checks
class StringCharactersReadConstraintsTest extends AsyncTestBase
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
            JsonFactory f = factory(limit, JsonRecyclerPools.nonRecyclingPool());
            for (String value : new String[] { repeat('x', limit - 1), repeat('x', limit) }) {
                forAllParsers(f, q(value), p -> {
                    assertToken(JsonToken.VALUE_STRING, p.nextToken());
                    assertEquals(value, p.currentTextViaCharacters());
                    assertEquals(q(value), copyCurrentEvent(p.parser()));
                });
            }
        }
    }

    @Test
    void oversizedStringsRejected() throws Exception {
        for (int limit : new int[] { SHORT_LIMIT, LONG_LIMIT }) {
            JsonFactory f = factory(limit, JsonRecyclerPools.nonRecyclingPool());
            // Just one char over: segments completed during nextToken() stay within limit
            int len = limit + 1;
            for (String doc : new String[] {
                    q(repeat('x', len)), "\"\\u0078" + repeat('x', len - 1) + "\"" }) {
                forAllParsers(f, doc, p -> {
                    assertToken(JsonToken.VALUE_STRING, p.nextToken());
                    assertThrows(StreamConstraintsException.class,
                            () -> p.parser().getStringCharacters());
                    assertThrows(StreamConstraintsException.class,
                            () -> copyCurrentEvent(p.parser()));
                });
            }
        }
    }

    // Recycled segment bigger than the limit (here from a parser of a factory
    // without the limit, sharing the pool): whole value fits in one segment
    @Test
    void oversizedStringInRecycledSegmentRejected() throws Exception {
        RecyclerPool<BufferRecycler> pool = JsonRecyclerPools.newConcurrentDequePool();
        JsonFactory lenient = JsonFactory.builder().recyclerPool(pool).build();
        // Escape forces copy into TextBuffer, so segment grows to fit the value
        String bigDoc = "\"" + repeat('y', 50_000) + "\\n\"";
        try (JsonParser p = lenient.createParser(testObjectReadContext(), bigDoc)) {
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals(50_001, p.getString().length());
        }

        JsonFactory f = factory(SHORT_LIMIT, pool);
        String value = repeat('x', 2_000);
        forAllParsers(f, q(value), p -> {
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertThrows(StreamConstraintsException.class,
                    () -> p.parser().getStringCharacters());
            assertThrows(StreamConstraintsException.class, () -> p.currentText());
        });
    }

    private void forAllParsers(JsonFactory f, String doc, ParserCheck check) throws Exception {
        for (int mode : ALL_MODES) {
            try (AsyncReaderWrapper p = syncWrapper(createParser(f, mode, doc))) {
                runCheck("sync mode " + mode, p, check);
            }
        }
        // Whole content as input buffer: value may be shared, not copied
        try (AsyncReaderWrapper p = syncWrapper(f.createParser(testObjectReadContext(), doc))) {
            runCheck("String input", p, check);
        }
        char[] chars = doc.toCharArray();
        try (AsyncReaderWrapper p = syncWrapper(f.createParser(testObjectReadContext(),
                chars, 0, chars.length))) {
            runCheck("char[] input", p, check);
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

    private String copyCurrentEvent(JsonParser p) {
        StringWriter w = new StringWriter();
        try (JsonGenerator g = sharedStreamFactory().createGenerator(ObjectWriteContext.empty(), w)) {
            g.copyCurrentEvent(p);
        }
        return w.toString();
    }

    private String repeat(char ch, int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, ch);
        return new String(chars);
    }

    private JsonFactory factory(int limit, RecyclerPool<BufferRecycler> pool) {
        return JsonFactory.builder()
                .recyclerPool(pool)
                .streamReadConstraints(StreamReadConstraints.builder()
                        .maxStringLength(limit).build())
                .build();
    }
}
