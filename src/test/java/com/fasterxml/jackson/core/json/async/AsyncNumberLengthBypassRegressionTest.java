package com.fasterxml.jackson.core.json.async;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonFactoryBuilder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Regression test for the sibling of <a href="https://github.com/FasterXML/jackson-core/pull/1555">#1555</a>
 * (parent advisory GHSA-72hv-8253-57qq).
 *<p>
 * The original fix wired {@code validateIntegerLength} / {@code validateFPLength} into the
 * "value-complete" branches via {@code _setIntLength} / {@code _setFractLength} /
 * {@code _setExpLength}. The fraction path's {@code NOT_AVAILABLE} exits in
 * {@link NonBlockingUtf8JsonParserBase} were patched at the same time; the integer path
 * was not.
 *<p>
 * Without this fix, an attacker feeding digit-only chunks (no terminator, no
 * {@code endOfInput()}) to a non-blocking parser accumulates {@code _textBuffer} past the
 * configured {@code maxNumberLength}, bounded only by {@code maxStringLength}. The
 * documented {@code maxNumberLength} should be enforced uniformly across sync and async
 * parsers.
 */
class AsyncNumberLengthBypassRegressionTest
    extends com.fasterxml.jackson.core.JUnit5TestBase
{
    private static final int MAX_NUM_LEN = 1000;
    // Chunk size kept modest so the test runs quickly under CI but still exceeds
    // maxNumberLength after the very first chunk.
    private static final int CHUNK_SIZE = 4 * 1024;
    // Hard cap on chunks fed: well past maxNumberLength but bounded so a regressed
    // build cannot OOM the CI machine.
    private static final int MAX_CHUNKS = 32;

    private final JsonFactory STRICT_F = new JsonFactoryBuilder()
            .streamReadConstraints(StreamReadConstraints.builder()
                    .maxNumberLength(MAX_NUM_LEN)
                    .build())
            .build();

    /**
     * Streams the integer portion of a number across many chunks, never sending a
     * terminator or {@code endOfInput()}. Each chunk is followed by a
     * {@link JsonToken#NOT_AVAILABLE} return. Asserts that
     * {@link StreamConstraintsException} is raised promptly (within the first few
     * chunks past {@code maxNumberLength}), not deferred until the value is finally
     * closed.
     */
    @Test
    void integerPath_streamingChunks_rejectsBeyondMaxNumberLength() throws Exception {
        try (JsonParser ap = STRICT_F.createNonBlockingByteArrayParser()) {
            ByteArrayFeeder feeder = (ByteArrayFeeder) ap;

            // Preamble: open an object and field name; no terminator for the value.
            byte[] preamble = utf8Bytes("{\"v\":");
            feeder.feedInput(preamble, 0, preamble.length);

            // Drain tokens up to the point where the parser is starved.
            JsonToken t;
            while ((t = ap.nextToken()) != JsonToken.NOT_AVAILABLE) {
                // Expect START_OBJECT and FIELD_NAME, then NOT_AVAILABLE.
                if (t == null) {
                    fail("Parser ended unexpectedly while draining preamble");
                }
            }

            // Now stream digit-only chunks. No terminator, no endOfInput().
            byte[] digits = new byte[CHUNK_SIZE];
            for (int i = 0; i < digits.length; i++) {
                digits[i] = (byte) ('1' + (i % 9));
            }

            int chunksFed = 0;
            try {
                for (int c = 0; c < MAX_CHUNKS; c++) {
                    feeder.feedInput(digits, 0, digits.length);
                    chunksFed++;
                    JsonToken tt = ap.nextToken();
                    if (tt != JsonToken.NOT_AVAILABLE) {
                        fail("Expected NOT_AVAILABLE while streaming integer digits, got: " + tt);
                    }
                }
                // Reaching here means the parser silently accumulated
                // CHUNK_SIZE * MAX_CHUNKS bytes (well past MAX_NUM_LEN) without
                // raising. That is the bug this test guards against.
                fail("Async parser silently buffered " + (CHUNK_SIZE * MAX_CHUNKS)
                        + " digits of an integer value while maxNumberLength="
                        + MAX_NUM_LEN
                        + "; expected StreamConstraintsException to be raised promptly");
            } catch (StreamConstraintsException e) {
                // Expected: validator must fire on a NOT_AVAILABLE exit once the
                // accumulated integer length exceeds maxNumberLength.
                String msg = String.valueOf(e.getMessage());
                assertTrue(msg.contains("Number value length"),
                        "Unexpected message: " + msg);
                assertTrue(msg.contains("exceeds the maximum allowed"),
                        "Unexpected message: " + msg);
                // Must fire promptly, not after the full MAX_CHUNKS were accepted.
                // (CHUNK_SIZE > MAX_NUM_LEN, so failure must occur on the first
                // chunk past the limit; chunksFed == 1 in practice.)
                assertTrue(chunksFed <= 2,
                        "StreamConstraintsException raised too late: after " + chunksFed
                                + " chunks of " + CHUNK_SIZE
                                + " digits (maxNumberLength=" + MAX_NUM_LEN + ")");
            }
        }
    }

    /**
     * Companion: the fraction path was already correctly patched in #1555. This test
     * pins that behavior so a future refactor cannot regress it.
     */
    @Test
    void fractionPath_streamingChunks_rejectsBeyondMaxNumberLength() throws Exception {
        try (JsonParser ap = STRICT_F.createNonBlockingByteArrayParser()) {
            ByteArrayFeeder feeder = (ByteArrayFeeder) ap;

            byte[] preamble = utf8Bytes("{\"v\":0.");
            feeder.feedInput(preamble, 0, preamble.length);
            JsonToken t;
            while ((t = ap.nextToken()) != JsonToken.NOT_AVAILABLE) {
                if (t == null) {
                    fail("Parser ended unexpectedly while draining preamble");
                }
            }

            byte[] digits = new byte[CHUNK_SIZE];
            for (int i = 0; i < digits.length; i++) {
                digits[i] = (byte) ('1' + (i % 9));
            }

            int chunksFed = 0;
            try {
                for (int c = 0; c < MAX_CHUNKS; c++) {
                    feeder.feedInput(digits, 0, digits.length);
                    chunksFed++;
                    JsonToken tt = ap.nextToken();
                    if (tt != JsonToken.NOT_AVAILABLE) {
                        fail("Expected NOT_AVAILABLE while streaming fraction digits, got: " + tt);
                    }
                }
                fail("Async parser silently buffered " + (CHUNK_SIZE * MAX_CHUNKS)
                        + " fraction digits while maxNumberLength=" + MAX_NUM_LEN);
            } catch (StreamConstraintsException e) {
                String msg = String.valueOf(e.getMessage());
                assertTrue(msg.contains("Number value length"),
                        "Unexpected message: " + msg);
                assertTrue(chunksFed <= 2,
                        "StreamConstraintsException raised too late: after " + chunksFed
                                + " chunks of " + CHUNK_SIZE
                                + " digits (maxNumberLength=" + MAX_NUM_LEN + ")");
            }
        }
    }
}
