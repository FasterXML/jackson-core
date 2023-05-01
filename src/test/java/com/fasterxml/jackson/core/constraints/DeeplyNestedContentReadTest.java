package com.fasterxml.jackson.core.constraints;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;

/**
 * Unit test(s) for verifying handling of new (in 2.15) StreamReadConstraints
 * wrt maximum nesting depth.
 */
public class DeeplyNestedContentReadTest
    extends com.fasterxml.jackson.core.BaseTest
{
    private final JsonFactory JSON_F = newStreamFactory();

    private final int MAX_NESTING = StreamReadConstraints.DEFAULT_MAX_DEPTH;

    private final int TESTED_NESTING = MAX_NESTING + 50;
    
    public void testDeepNestingStreaming() throws Exception
    {
        final String DOC = createDeepNestedDoc(TESTED_NESTING);
        for (int mode : ALL_STREAMING_MODES) {
            try (JsonParser p = createParser(JSON_F, mode, DOC)) {
                _testDeepNesting(p);
            }
        }
    }

    private void _testDeepNesting(JsonParser p) throws Exception
    {
        try {
            while (p.nextToken() != null) { }
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            assertEquals("Document nesting depth (1001) exceeds the maximum allowed (1000, from `StreamReadConstraints.getMaxNestingDepth()`)",
                    e.getMessage());
        }
    }

    public void testLegacyConstraintSettingTest() throws Exception
    {
        final int LOWER_MAX = 40;
        
        final String DOC = createDeepNestedDoc(LOWER_MAX + 10);
        JsonFactory f = new JsonFactory();
        f.setStreamReadConstraints(StreamReadConstraints.builder()
                .maxNestingDepth(LOWER_MAX).build());
        for (int mode : ALL_STREAMING_MODES) {
            try (JsonParser p = createParser(f, mode, DOC)) {
                _testLegacyConstraintSettingTest(p, LOWER_MAX);
            }
        }
    }

    private void _testLegacyConstraintSettingTest(JsonParser p, int maxNesting) throws Exception
    {
        try {
            while (p.nextToken() != null) { }
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            assertEquals("Document nesting depth (41) exceeds the maximum allowed (40, from `StreamReadConstraints.getMaxNestingDepth()`)",
                    e.getMessage());
        }
    }
    
    
    private String createDeepNestedDoc(final int depth) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < depth; i++) {
            sb.append("{ \"a\": [");
        }
        sb.append(" \"val\" ");
        for (int i = 0; i < depth; i++) {
            sb.append("]}");
        }
        sb.append("]");
        return sb.toString();
    }
}
