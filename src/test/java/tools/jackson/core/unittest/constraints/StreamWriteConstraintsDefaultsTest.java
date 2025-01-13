package tools.jackson.core.unittest.constraints;

import org.junit.jupiter.api.Test;

import tools.jackson.core.StreamWriteConstraints;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StreamWriteConstraintsDefaultsTest {
    @Test
    void override() {
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
