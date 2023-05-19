package com.fasterxml.jackson.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestStreamReadConstraints {
    @Test
    public void testOverride() {
        final int numLen = 1234;
        final int strLen = 12345;
        final int depth = 123;
        StreamReadConstraints constraints = StreamReadConstraints.builder()
                .maxNumberLength(numLen)
                .maxStringLength(strLen)
                .maxNestingDepth(depth)
                .build();
        try {
            StreamReadConstraints.overrideDefaultStreamReadConstraints(constraints);
            assertEquals(depth, StreamReadConstraints.defaults().getMaxNestingDepth());
            assertEquals(strLen, StreamReadConstraints.defaults().getMaxStringLength());
            assertEquals(numLen, StreamReadConstraints.defaults().getMaxNumberLength());
        } finally {
            StreamReadConstraints.overrideDefaultStreamReadConstraints(null);
            assertEquals(StreamReadConstraints.DEFAULT_MAX_DEPTH,
                    StreamReadConstraints.defaults().getMaxNestingDepth());
            assertEquals(StreamReadConstraints.DEFAULT_MAX_STRING_LEN,
                    StreamReadConstraints.defaults().getMaxStringLength());
            assertEquals(StreamReadConstraints.DEFAULT_MAX_NUM_LEN,
                    StreamReadConstraints.defaults().getMaxNumberLength());
        }
    }
}
