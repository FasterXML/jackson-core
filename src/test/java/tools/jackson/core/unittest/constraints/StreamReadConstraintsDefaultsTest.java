package tools.jackson.core.unittest.constraints;

import org.junit.jupiter.api.Test;

import tools.jackson.core.StreamReadConstraints;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
