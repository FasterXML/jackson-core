package tools.jackson.core.unittest.constraints;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonToken;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.unittest.async.AsyncTestBase;
import tools.jackson.core.unittest.testutil.AsyncReaderWrapper;
import tools.jackson.core.util.BufferRecycler;
import tools.jackson.core.util.JsonRecyclerPools;
import tools.jackson.core.util.RecyclerPool;

import static org.junit.jupiter.api.Assertions.*;

class AsyncPositiveIntegerBufferingTest extends AsyncTestBase
{
    private static final int MAX_NUM_LEN = 1_000;
    private static final int OVERSIZED_LEN = 50_000;

    @Test
    void singleChunkIntegerRejectedBeforeFullBuffering() throws Exception {
        for (boolean byteBuffer : new boolean[] { false, true }) {
            RecordingConstraints constraints = new RecordingConstraints();
            JsonFactory factory = factory(constraints, JsonRecyclerPools.nonRecyclingPool());
            byte[] input = utf8Bytes("7".repeat(OVERSIZED_LEN) + " ");

            try (AsyncReaderWrapper reader = reader(factory, byteBuffer, input.length, input)) {
                assertThrows(StreamConstraintsException.class, reader::nextToken);
                long consumed = reader.parser().currentLocation().getByteOffset();
                // Processing limits need not fail at exactly max + 1, but must
                // reject well before buffering the entire oversized value.
                assertAll(
                        () -> assertTrue(constraints.failingIntegerLength > MAX_NUM_LEN),
                        () -> assertTrue(constraints.failingIntegerLength < OVERSIZED_LEN / 10,
                                "Integer length checked too late: " + constraints.failingIntegerLength),
                        () -> assertTrue(consumed < OVERSIZED_LEN / 10,
                                "Too much input consumed before rejection: " + consumed));
            }
        }
    }

    @Test
    void integersWithinLimitStillParse() throws Exception {
        for (boolean byteBuffer : new boolean[] { false, true }) {
            for (int length : new int[] { 1, MAX_NUM_LEN - 1, MAX_NUM_LEN }) {
                for (int chunkSize : new int[] { 1, 100, OVERSIZED_LEN + 1 }) {
                    verifyInteger(factory(new RecordingConstraints(),
                            JsonRecyclerPools.nonRecyclingPool()), byteBuffer, chunkSize, length);
                }
            }
        }
    }

    @Test
    void chunkedOversizedIntegersStillRejected() throws Exception {
        for (boolean byteBuffer : new boolean[] { false, true }) {
            for (int chunkSize : new int[] { 1, 100, 4096 }) {
                RecordingConstraints constraints = new RecordingConstraints();
                JsonFactory factory = factory(constraints, JsonRecyclerPools.nonRecyclingPool());
                byte[] input = utf8Bytes("7".repeat(OVERSIZED_LEN) + " ");
                try (AsyncReaderWrapper reader = reader(factory, byteBuffer, chunkSize, input)) {
                    assertThrows(StreamConstraintsException.class, reader::nextToken);
                    assertTrue(constraints.failingIntegerLength > MAX_NUM_LEN);
                }
            }
        }
    }

    @Test
    void recycledLargeBufferStillHonorsNumberLimit() throws Exception {
        for (boolean byteBuffer : new boolean[] { false, true }) {
            RecyclerPool<BufferRecycler> pool = JsonRecyclerPools.newBoundedPool(1);
            BufferRecycler recycler = pool.acquireAndLinkPooled();
            char[] largeBuffer = new char[60_000];
            recycler.releaseCharBuffer(BufferRecycler.CHAR_TEXT_BUFFER, largeBuffer);
            recycler.releaseToPool();
            RecordingConstraints constraints = new RecordingConstraints();
            JsonFactory factory = factory(constraints, pool);

            verifyInteger(factory, byteBuffer, OVERSIZED_LEN + 1, MAX_NUM_LEN);
            // Verify that the normal parse actually used the seeded buffer.
            recycler = pool.acquireAndLinkPooled();
            assertSame(largeBuffer, recycler.allocCharBuffer(BufferRecycler.CHAR_TEXT_BUFFER));
            recycler.releaseCharBuffer(BufferRecycler.CHAR_TEXT_BUFFER, largeBuffer);
            recycler.releaseToPool();

            byte[] input = utf8Bytes("7".repeat(OVERSIZED_LEN) + " ");
            try (AsyncReaderWrapper reader = reader(factory, byteBuffer, input.length, input)) {
                assertThrows(StreamConstraintsException.class, reader::nextToken);
                assertTrue(constraints.failingIntegerLength > MAX_NUM_LEN);
            }
        }
    }

    private void verifyInteger(JsonFactory factory, boolean byteBuffer, int chunkSize, int length)
            throws Exception {
        String number = "7".repeat(length);
        try (AsyncReaderWrapper reader = reader(factory, byteBuffer, chunkSize,
                utf8Bytes(number + " "))) {
            assertToken(JsonToken.VALUE_NUMBER_INT, reader.nextToken());
            assertEquals(number, reader.currentText());
            assertEquals(new BigInteger(number), reader.getBigIntegerValue());
            assertNull(reader.nextToken());
        }
    }

    private AsyncReaderWrapper reader(JsonFactory factory, boolean byteBuffer,
            int chunkSize, byte[] input) throws Exception {
        return byteBuffer ? asyncForByteBuffer(factory, chunkSize, input, 0)
                : asyncForBytes(factory, chunkSize, input, 0);
    }

    private JsonFactory factory(RecordingConstraints constraints, RecyclerPool<BufferRecycler> pool) {
        return JsonFactory.builder().streamReadConstraints(constraints).recyclerPool(pool).build();
    }

    private static final class RecordingConstraints extends StreamReadConstraints {
        int failingIntegerLength;

        RecordingConstraints() {
            super(DEFAULT_MAX_DEPTH, DEFAULT_MAX_DOC_LEN, DEFAULT_MAX_TOKEN_COUNT,
                    MAX_NUM_LEN, DEFAULT_MAX_STRING_LEN, DEFAULT_MAX_NAME_LEN);
        }

        @Override
        public void validateIntegerLength(int length) throws StreamConstraintsException {
            try {
                super.validateIntegerLength(length);
            } catch (StreamConstraintsException e) {
                failingIntegerLength = length;
                throw e;
            }
        }
    }
}
