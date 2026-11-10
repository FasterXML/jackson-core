package tools.jackson.core.unittest.constraints;

import java.nio.ByteBuffer;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.async.ByteArrayFeeder;
import tools.jackson.core.async.ByteBufferFeeder;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.unittest.async.AsyncTestBase;
import tools.jackson.core.unittest.testutil.AsyncReaderWrapper;
import tools.jackson.core.util.BufferRecycler;
import tools.jackson.core.util.JsonRecyclerPools;
import tools.jackson.core.util.RecyclerPool;

import static org.junit.jupiter.api.Assertions.*;

class AsyncDecimalBufferingTest extends AsyncTestBase
{
    private static final int MAX_NUM_LEN = 1_000;
    private static final int OVERSIZED_LEN = 50_000;

    @ParameterizedTest(name = "{0}, first chunk = {1}")
    @MethodSource("oversizedNumbers")
    void oversizedNumbersRejectedBeforeFullBuffering(String prefix, int firstChunk)
            throws Exception {
        for (boolean byteBuffer : new boolean[] { false, true }) {
            RecordingConstraints constraints = new RecordingConstraints(MAX_NUM_LEN);
            JsonFactory factory = factory(constraints, JsonRecyclerPools.nonRecyclingPool());
            byte[] input = utf8Bytes(prefix + "7".repeat(OVERSIZED_LEN) + " ");
            try (JsonParser parser = parser(factory, byteBuffer)) {
                if (firstChunk > 0) {
                    feed(parser, byteBuffer, input, 0, firstChunk);
                    assertToken(JsonToken.NOT_AVAILABLE, parser.nextToken());
                }
                feed(parser, byteBuffer, input, firstChunk, input.length);
                assertThrows(StreamConstraintsException.class, parser::nextToken);
                long consumed = parser.currentLocation().getByteOffset();
                assertAll(
                        () -> assertTrue(constraints.failingLength > MAX_NUM_LEN),
                        () -> assertTrue(constraints.failingLength < OVERSIZED_LEN / 10,
                                "Length checked too late: " + constraints.failingLength),
                        () -> assertTrue(consumed < OVERSIZED_LEN / 10,
                                "Too much input consumed: " + consumed));
            }
        }
    }

    static Stream<Arguments> oversizedNumbers() {
        // The single-chunk positive integer is covered by AsyncPositiveIntegerBufferingTest.
        return Stream.of(
                Arguments.of("-", 0),
                Arguments.of("", 1),
                Arguments.of("-", 2),
                Arguments.of("+", 0),
                Arguments.of("1.", 0),
                Arguments.of("1.", 3),
                Arguments.of("1e+", 0),
                Arguments.of("1e+", 4),
                Arguments.of("-1.2E-", 0));
    }

    @Test
    void signedAndFloatingPointNumbersAtLimitStillParse() throws Exception {
        for (int length : new int[] { MAX_NUM_LEN - 1, MAX_NUM_LEN }) {
            String digits = "7".repeat(length);
            String fraction = "7".repeat(length - 1);
            String exponent = "7".repeat(length - 2);
            for (String value : new String[] {
                    "-" + digits, "+" + digits, "1." + fraction, "-1." + fraction,
                    "." + digits, "-." + digits, "1e+" + fraction, "-1.2E-" + exponent }) {
                verifyValidNumber(value, MAX_NUM_LEN);
            }
        }
    }

    @Test
    void signsAndMarkersDoNotCountTowardsSmallLimit() throws Exception {
        for (String value : new String[] {
                "12345", "-12345", "+12345", "1.2345", "-1.2345", "+1.2345",
                "123e45", "123e+45", "-123e-45", "+12.3E+45",
                ".12345", "-.12345", "+.12345" }) {
            verifyValidNumber(value, 5);
        }
    }

    @Test
    void markersAtExpansionBoundariesStillParse() throws Exception {
        String digits = "7".repeat(199);
        for (String value : new String[] {
                "-" + digits + ".1", digits + "e+1", digits + "7e1",
                "1." + "7".repeat(198) + "e+1" }) {
            verifyValidNumber(value, 201);
        }
    }

    @Test
    void numbersOneDigitOverLimitStillRejected() throws Exception {
        String digits = "7".repeat(MAX_NUM_LEN + 1);
        for (String value : new String[] {
                "-" + digits, "+" + digits, "+." + digits,
                "1." + "7".repeat(MAX_NUM_LEN), "-1.2e+" + "7".repeat(MAX_NUM_LEN - 1) }) {
            for (boolean byteBuffer : new boolean[] { false, true }) {
                for (int chunkSize : new int[] { 1, OVERSIZED_LEN }) {
                    RecordingConstraints constraints = new RecordingConstraints(MAX_NUM_LEN);
                    JsonFactory factory = factory(constraints, JsonRecyclerPools.nonRecyclingPool());
                    byte[] input = utf8Bytes(value + " ");
                    try (AsyncReaderWrapper reader = byteBuffer
                            ? asyncForByteBuffer(factory, chunkSize, input, 0)
                            : asyncForBytes(factory, chunkSize, input, 0)) {
                        assertThrows(StreamConstraintsException.class, reader::nextToken);
                        assertTrue(constraints.failingLength > MAX_NUM_LEN);
                    }
                }
            }
        }
    }

    @Test
    void reusedLargeBuffersStillRejectOversizedNumbers() throws Exception {
        // Complements the recycled integer buffer test with an FP value using all parts.
        for (boolean byteBuffer : new boolean[] { false, true }) {
            RecyclerPool<BufferRecycler> pool = JsonRecyclerPools.newBoundedPool(1);
            BufferRecycler recycler = pool.acquireAndLinkPooled();
            recycler.releaseCharBuffer(BufferRecycler.CHAR_TEXT_BUFFER, new char[60_000]);
            recycler.releaseToPool();
            RecordingConstraints constraints = new RecordingConstraints(MAX_NUM_LEN);
            JsonFactory factory = factory(constraints, pool);
            byte[] input = utf8Bytes("-1.2E-" + "7".repeat(OVERSIZED_LEN) + " ");
            try (JsonParser parser = parser(factory, byteBuffer)) {
                feed(parser, byteBuffer, input, 0, input.length);
                assertThrows(StreamConstraintsException.class, parser::nextToken);
                assertTrue(constraints.failingLength > MAX_NUM_LEN);
            }
        }
    }

    private void verifyValidNumber(String value, int maxNumberLength) throws Exception {
        for (boolean byteBuffer : new boolean[] { false, true }) {
            for (int chunkSize : new int[] { 1, OVERSIZED_LEN }) {
                JsonFactory factory = factory(new RecordingConstraints(maxNumberLength),
                        JsonRecyclerPools.nonRecyclingPool());
                byte[] input = utf8Bytes(value + " ");
                try (AsyncReaderWrapper reader = byteBuffer
                        ? asyncForByteBuffer(factory, chunkSize, input, 0)
                        : asyncForBytes(factory, chunkSize, input, 0)) {
                    JsonToken expected = value.indexOf('.') >= 0 || value.indexOf('e') >= 0
                            || value.indexOf('E') >= 0 ? JsonToken.VALUE_NUMBER_FLOAT
                                    : JsonToken.VALUE_NUMBER_INT;
                    assertToken(expected, reader.nextToken());
                    assertEquals(value, reader.currentText());
                    assertEquals(value.length(), reader.parser().getStringLength());
                    assertNull(reader.nextToken());
                }
            }
        }
    }

    private JsonFactory factory(RecordingConstraints constraints, RecyclerPool<BufferRecycler> pool) {
        return JsonFactory.builder().streamReadConstraints(constraints).recyclerPool(pool)
                .enable(JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS,
                        JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS).build();
    }

    private JsonParser parser(JsonFactory factory, boolean byteBuffer) {
        return byteBuffer ? factory.createNonBlockingByteBufferParser(ObjectReadContext.empty())
                : factory.createNonBlockingByteArrayParser(ObjectReadContext.empty());
    }

    private void feed(JsonParser parser, boolean byteBuffer, byte[] input, int start, int end) {
        if (byteBuffer) {
            ((ByteBufferFeeder) parser.nonBlockingInputFeeder())
                    .feedInput(ByteBuffer.wrap(input, start, end - start));
        } else {
            ((ByteArrayFeeder) parser.nonBlockingInputFeeder()).feedInput(input, start, end);
        }
    }

    private static final class RecordingConstraints extends StreamReadConstraints {
        int failingLength;

        RecordingConstraints(int maxNumberLength) {
            super(DEFAULT_MAX_DEPTH, DEFAULT_MAX_DOC_LEN, DEFAULT_MAX_TOKEN_COUNT,
                    maxNumberLength, DEFAULT_MAX_STRING_LEN, DEFAULT_MAX_NAME_LEN);
        }

        @Override
        public void validateIntegerLength(int length) throws StreamConstraintsException {
            try {
                super.validateIntegerLength(length);
            } catch (StreamConstraintsException e) {
                failingLength = length;
                throw e;
            }
        }

        @Override
        public void validateFPLength(int length) throws StreamConstraintsException {
            try {
                super.validateFPLength(length);
            } catch (StreamConstraintsException e) {
                failingLength = length;
                throw e;
            }
        }
    }
}
