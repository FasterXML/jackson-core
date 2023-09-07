package com.fasterxml.jackson.core.constraints;

import org.junit.Test;

import com.fasterxml.jackson.core.StreamReadConstraints;

import static org.junit.Assert.assertEquals;

public class StreamReadConstraintsDefaultsTest
{
    @Test
    public void testOverride() {
        final long maxDocLen = 10_000_000L;
        final int numLen = 1234;
        final int strLen = 12345;
        final int depth = 123;
        StreamReadConstraints constraints = StreamReadConstraints.builder()
                .maxDocumentLength(maxDocLen)
                .maxNumberLength(numLen)
                .maxStringLength(strLen)
                .maxNestingDepth(depth)
                .build();
        try {
            StreamReadConstraints.overrideDefaultStreamReadConstraints(constraints);
            assertEquals(maxDocLen, StreamReadConstraints.defaults().getMaxDocumentLength());
            assertEquals(depth, StreamReadConstraints.defaults().getMaxNestingDepth());
            assertEquals(strLen, StreamReadConstraints.defaults().getMaxStringLength());
            assertEquals(numLen, StreamReadConstraints.defaults().getMaxNumberLength());
        } finally {
            StreamReadConstraints.overrideDefaultStreamReadConstraints(null);
            assertEquals(StreamReadConstraints.DEFAULT_MAX_DOC_LEN,
                    StreamReadConstraints.defaults().getMaxDocumentLength());
            assertEquals(StreamReadConstraints.DEFAULT_MAX_DEPTH,
                    StreamReadConstraints.defaults().getMaxNestingDepth());
            assertEquals(StreamReadConstraints.DEFAULT_MAX_STRING_LEN,
                    StreamReadConstraints.defaults().getMaxStringLength());
            assertEquals(StreamReadConstraints.DEFAULT_MAX_NUM_LEN,
                    StreamReadConstraints.defaults().getMaxNumberLength());
        }
    }
}
