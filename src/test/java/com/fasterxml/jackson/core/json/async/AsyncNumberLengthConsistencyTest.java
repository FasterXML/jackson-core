package com.fasterxml.jackson.core.json.async;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonFactoryBuilder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests that {@code maxNumberLength} is enforced consistently across sync and async
 * parsers when digit-only input is fed across multiple chunks (no terminator, no
 * {@code endOfInput()}).
 */
class AsyncNumberLengthConsistencyTest
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
     * Streams the integer portion of a number across many chunks. Asserts that
     * {@link StreamConstraintsException} is raised promptly once the accumulated
     * digit length exceeds {@code maxNumberLength}, rather than only at value
     * completion.
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
                // Reaching here means the parser accepted more than CHUNK_SIZE *
                // MAX_CHUNKS digits without raising — i.e. maxNumberLength was not
                // enforced on the streaming integer path.
                fail("Async parser accepted " + (CHUNK_SIZE * MAX_CHUNKS)
                        + " integer digits with maxNumberLength=" + MAX_NUM_LEN
                        + "; expected StreamConstraintsException");
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
     * Companion to the integer-path test: pins the fraction-path streaming behavior
     * so a future refactor cannot regress it.
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
                fail("Async parser accepted " + (CHUNK_SIZE * MAX_CHUNKS)
                        + " fraction digits with maxNumberLength=" + MAX_NUM_LEN);
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

    /**
     * Pins the headline cross-chunk scenario directly: with a chunk size well below
     * {@code maxNumberLength}, the limit is only crossed after several streaming
     * suspensions, so this exercises accumulation inside {@code _finishNumberIntegralPart}
     * (not just a single oversized first chunk). Asserts the exception fires at the
     * expected boundary — after more than one chunk, but as soon as the limit is passed.
     */
    @Test
    void integerPath_smallChunksAccumulate_rejectAtBoundary() throws Exception {
        final int smallChunk = 100; // < MAX_NUM_LEN, so several chunks are needed
        // Limit is crossed once accumulated digits exceed MAX_NUM_LEN; with 100-digit
        // chunks that is the 11th chunk (1000 ok at chunk 10, 1100 > 1000 at chunk 11).
        final int expectedFailChunk = (MAX_NUM_LEN / smallChunk) + 1;

        try (JsonParser ap = STRICT_F.createNonBlockingByteArrayParser()) {
            ByteArrayFeeder feeder = (ByteArrayFeeder) ap;

            byte[] preamble = utf8Bytes("{\"v\":");
            feeder.feedInput(preamble, 0, preamble.length);
            JsonToken t;
            while ((t = ap.nextToken()) != JsonToken.NOT_AVAILABLE) {
                if (t == null) {
                    fail("Parser ended unexpectedly while draining preamble");
                }
            }

            byte[] digits = new byte[smallChunk];
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
                fail("Async parser accepted " + (smallChunk * MAX_CHUNKS)
                        + " integer digits with maxNumberLength=" + MAX_NUM_LEN
                        + "; expected StreamConstraintsException");
            } catch (StreamConstraintsException e) {
                String msg = String.valueOf(e.getMessage());
                assertTrue(msg.contains("Number value length"),
                        "Unexpected message: " + msg);
                // Must accumulate across several chunks before firing...
                assertTrue(chunksFed > 1,
                        "Exception fired on a single chunk; cross-chunk accumulation not exercised");
                // ...and fire as soon as the limit is passed, not arbitrarily later.
                assertTrue(chunksFed <= expectedFailChunk + 1,
                        "StreamConstraintsException raised too late: after " + chunksFed
                                + " chunks of " + smallChunk + " (expected ~" + expectedFailChunk
                                + ", maxNumberLength=" + MAX_NUM_LEN + ")");
            }
        }
    }

    /**
     * Guards against the validator becoming over-eager: an integer whose length is
     * just below {@code maxNumberLength}, fed across many small chunks (each smaller
     * than the limit, so accumulation crosses several streaming-suspension points),
     * must still parse cleanly to the expected value.
     */
    @Test
    void integerPath_justUnderMaxNumberLength_parsesCleanly() throws Exception {
        // One short of the limit, so the validator must NOT fire.
        final int digitCount = MAX_NUM_LEN - 1;
        StringBuilder sb = new StringBuilder(digitCount);
        for (int i = 0; i < digitCount; i++) {
            sb.append((char) ('1' + (i % 9)));
        }
        final String number = sb.toString();

        try (JsonParser ap = STRICT_F.createNonBlockingByteArrayParser()) {
            ByteArrayFeeder feeder = (ByteArrayFeeder) ap;

            byte[] preamble = utf8Bytes("{\"v\":");
            feeder.feedInput(preamble, 0, preamble.length);
            JsonToken t;
            while ((t = ap.nextToken()) != JsonToken.NOT_AVAILABLE) {
                if (t == null) {
                    fail("Parser ended unexpectedly while draining preamble");
                }
            }

            // Feed the digits in small chunks (well under maxNumberLength) so the
            // integer is accumulated across multiple _finishNumberIntegralPart resumes.
            byte[] digits = utf8Bytes(number);
            final int smallChunk = 100;
            for (int off = 0; off < digits.length; off += smallChunk) {
                int len = Math.min(smallChunk, digits.length - off);
                feeder.feedInput(digits, off, off + len);
                assertEquals(JsonToken.NOT_AVAILABLE, ap.nextToken(),
                        "Expected NOT_AVAILABLE while streaming sub-limit integer digits");
            }

            // Terminate the value and signal end of input.
            byte[] tail = utf8Bytes("}");
            feeder.feedInput(tail, 0, tail.length);
            feeder.endOfInput();

            assertEquals(JsonToken.VALUE_NUMBER_INT, ap.nextToken(),
                    "Sub-limit integer should parse without StreamConstraintsException");
            assertEquals(number, ap.getText());
            assertEquals(JsonToken.END_OBJECT, ap.nextToken());
        }
    }

    /**
     * Negative-number variant of the cross-chunk rejection test: a leading {@code '-'}
     * routes through {@code _startNegativeNumber()} and the {@code negMod == -1} branch
     * of {@code _finishNumberIntegralPart}, so the sign must be excluded from the
     * validated digit length. With chunk size below {@code maxNumberLength}, the limit
     * is only crossed after accumulating across several streaming suspensions.
     */
    @Test
    void negativeIntegerPath_smallChunksAccumulate_rejectAtBoundary() throws Exception {
        final int smallChunk = 100; // < MAX_NUM_LEN, so several chunks are needed
        // Sign is excluded from the validated length, so the limit is crossed once the
        // accumulated *digit* count exceeds MAX_NUM_LEN: 1000 ok at chunk 10, 1100 > 1000
        // at chunk 11 (same boundary as the positive case).
        final int expectedFailChunk = (MAX_NUM_LEN / smallChunk) + 1;

        try (JsonParser ap = STRICT_F.createNonBlockingByteArrayParser()) {
            ByteArrayFeeder feeder = (ByteArrayFeeder) ap;

            // Note trailing '-': routes the value through the negative-number path.
            byte[] preamble = utf8Bytes("{\"v\":-");
            feeder.feedInput(preamble, 0, preamble.length);
            JsonToken t;
            while ((t = ap.nextToken()) != JsonToken.NOT_AVAILABLE) {
                if (t == null) {
                    fail("Parser ended unexpectedly while draining preamble");
                }
            }

            byte[] digits = new byte[smallChunk];
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
                        fail("Expected NOT_AVAILABLE while streaming negative integer digits, got: " + tt);
                    }
                }
                fail("Async parser accepted " + (smallChunk * MAX_CHUNKS)
                        + " negative integer digits with maxNumberLength=" + MAX_NUM_LEN
                        + "; expected StreamConstraintsException");
            } catch (StreamConstraintsException e) {
                String msg = String.valueOf(e.getMessage());
                assertTrue(msg.contains("Number value length"),
                        "Unexpected message: " + msg);
                // Must accumulate across several chunks before firing...
                assertTrue(chunksFed > 1,
                        "Exception fired on a single chunk; cross-chunk accumulation not exercised");
                // ...and fire at the same boundary as the positive case (sign excluded
                // from the validated length), not one chunk earlier.
                assertTrue(chunksFed >= expectedFailChunk,
                        "StreamConstraintsException raised too early (sign likely counted): after "
                                + chunksFed + " chunks of " + smallChunk
                                + " (expected ~" + expectedFailChunk + ", maxNumberLength=" + MAX_NUM_LEN + ")");
                assertTrue(chunksFed <= expectedFailChunk + 1,
                        "StreamConstraintsException raised too late: after " + chunksFed
                                + " chunks of " + smallChunk + " (expected ~" + expectedFailChunk
                                + ", maxNumberLength=" + MAX_NUM_LEN + ")");
            }
        }
    }
}
