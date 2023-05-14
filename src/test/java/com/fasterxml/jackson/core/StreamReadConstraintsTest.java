package com.fasterxml.jackson.core;

public class StreamReadConstraintsTest extends BaseTest {

    public void testSuccessfulBuild() {
        // default
        _verifyDefaultValues(StreamReadConstraints.builder().build());
        _verifyDefaultValues(_builderWith(0, 0, 0));

        // custom
        _verifyValues(_builderWith(500, 10, 50), 500, 10, 50);
        _verifyValues(_builderWith(0, 20, 100), StreamReadConstraints.DEFAULT_MAX_STRING_LEN, 20, 100);
    }

    public void testFailingBuild() {
        _verifyExceptionWithMessage("maxStringLen", -100, "Cannot set maxStringLen to a negative value");
        _verifyExceptionWithMessage("maxNestingDepth", -5, "Cannot set maxNestingDepth to a negative value");
        _verifyExceptionWithMessage("maxNumberLength", -50, "Cannot set maxNumberLength to a negative value");
    }

    private StreamReadConstraints _builderWith(int maxStringLength, int maxNestingDepth, int maxNumberLength) {
        return StreamReadConstraints.builder()
                .maxStringLength(maxStringLength)
                .maxNestingDepth(maxNestingDepth)
                .maxNumberLength(maxNumberLength)
                .build();
    }

    private void _verifyDefaultValues(StreamReadConstraints constraints) {
        _verifyValues(constraints, StreamReadConstraints.DEFAULT_MAX_STRING_LEN,
                StreamReadConstraints.DEFAULT_MAX_DEPTH,
                StreamReadConstraints.DEFAULT_MAX_NUM_LEN);
    }

    private void _verifyValues(StreamReadConstraints constraints, int expectedStringLength, int expectedNestingDepth,
                               int expectedNumberLength) {
        assertEquals(expectedStringLength, constraints.getMaxStringLength());
        assertEquals(expectedNestingDepth, constraints.getMaxNestingDepth());
        assertEquals(expectedNumberLength, constraints.getMaxNumberLength());
    }

    private void _verifyExceptionWithMessage(String propertyName, int value, String expectedMessage) {
        try {
            StreamReadConstraints.builder()
                    .maxStringLength("maxStringLen".equals(propertyName) ? value : StreamReadConstraints.DEFAULT_MAX_STRING_LEN)
                    .maxNestingDepth("maxNestingDepth".equals(propertyName) ? value : StreamReadConstraints.DEFAULT_MAX_DEPTH) 
                    .maxNumberLength("maxNumberLength".equals(propertyName) ? value : StreamReadConstraints.DEFAULT_MAX_NUM_LEN)
                    .build();
            fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) {
            verifyException(e, expectedMessage);
        }
    }
}
