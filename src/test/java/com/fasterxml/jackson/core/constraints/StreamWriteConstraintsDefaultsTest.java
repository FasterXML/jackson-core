package com.fasterxml.jackson.core.constraints;

import com.fasterxml.jackson.core.StreamWriteConstraints;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StreamWriteConstraintsDefaultsTest {
    @Test
    public void testOverride() {
        final int depth = 123;
        StreamWriteConstraints constraints = StreamWriteConstraints.builder()
                .maxNestingDepth(depth)
                .build();
        try {
            StreamWriteConstraints.overrideDefaultStreamWriteConstraints(constraints);
            assertEquals(depth, StreamWriteConstraints.defaults().getMaxNestingDepth());
        } finally {
            StreamWriteConstraints.overrideDefaultStreamWriteConstraints(null);
            assertEquals(StreamWriteConstraints.DEFAULT_MAX_DEPTH,
                    StreamWriteConstraints.defaults().getMaxNestingDepth());
        }
    }
}
