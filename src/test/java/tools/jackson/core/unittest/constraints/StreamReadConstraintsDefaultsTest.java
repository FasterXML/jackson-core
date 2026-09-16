package tools.jackson.core.unittest.constraints;

import org.junit.jupiter.api.Test;

import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.exc.StreamConstraintsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class StreamReadConstraintsDefaultsTest
{
    @Test
    void override() {
        final long maxDocLen = 10_000_000L;
        final int numLen = 1234;
        final int strLen = 12345;
        final int depth = 123;
        final int nameLen = 2000;
        StreamReadConstraints constraints = StreamReadConstraints.builder()
                .maxDocumentLength(maxDocLen)
                .maxNumberLength(numLen)
                .maxStringLength(strLen)
                .maxNameLength(nameLen)
                .maxNestingDepth(depth)
                .build();
        try {
            StreamReadConstraints.overrideDefaultStreamReadConstraints(constraints);
            assertEquals(maxDocLen, StreamReadConstraints.defaults().getMaxDocumentLength());
            assertEquals(depth, StreamReadConstraints.defaults().getMaxNestingDepth());
            assertEquals(strLen, StreamReadConstraints.defaults().getMaxStringLength());
            assertEquals(nameLen, StreamReadConstraints.defaults().getMaxNameLength());
            assertEquals(numLen, StreamReadConstraints.defaults().getMaxNumberLength());
        } finally {
            StreamReadConstraints.overrideDefaultStreamReadConstraints(null);
            assertEquals(StreamReadConstraints.DEFAULT_MAX_DOC_LEN,
                    StreamReadConstraints.defaults().getMaxDocumentLength());
            assertEquals(StreamReadConstraints.DEFAULT_MAX_DEPTH,
                    StreamReadConstraints.defaults().getMaxNestingDepth());
            assertEquals(StreamReadConstraints.DEFAULT_MAX_STRING_LEN,
                    StreamReadConstraints.defaults().getMaxStringLength());
            assertEquals(StreamReadConstraints.DEFAULT_MAX_NAME_LEN,
                    StreamReadConstraints.defaults().getMaxNameLength());
            assertEquals(StreamReadConstraints.DEFAULT_MAX_NUM_LEN,
                    StreamReadConstraints.defaults().getMaxNumberLength());
        }
    }

    // Accessor for maximum BigInteger (BigDecimal) scale magnitude
    @Test
    void maxBigIntegerScale() throws Exception
    {
        final StreamReadConstraints constraints = StreamReadConstraints.defaults();
        final int limit = constraints.getMaxBigIntegerScale();

        assertEquals(100_000, limit);

        // Within limit (both signs) must pass:
        constraints.validateBigIntegerScale(limit);
        constraints.validateBigIntegerScale(-limit);

        // But just past it must fail, and message should refer to the accessor:
        for (int scale : new int[] { limit + 1, -(limit + 1) }) {
            try {
                constraints.validateBigIntegerScale(scale);
                fail("Should not pass, scale "+scale);
            } catch (StreamConstraintsException e) {
                final String msg = e.getMessage();
                assertTrue(msg.contains("BigDecimal scale ("+scale+")"),
                        "Unexpected message: "+msg);
                assertTrue(msg.contains("StreamReadConstraints.getMaxBigIntegerScale()"),
                        "Unexpected message: "+msg);
            }
        }
    }
}
