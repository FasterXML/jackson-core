package com.fasterxml.jackson.core.constraints;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;

/**
 * Set of additional unit for verifying array parsing, specifically
 * edge cases.
 */
public class DeeplyNestedArrayReadTest
    extends com.fasterxml.jackson.core.BaseTest
{
    public void testDeepNesting() throws Exception
    {
        final String DOC = createDeepNestedDoc(1050);
        try (JsonParser jp = createParserUsingStream(new JsonFactory(), DOC, "UTF-8")) {
            while (jp.nextToken() != null) { }
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            assertEquals("Depth (1001) exceeds the maximum allowed nesting depth (1000)", e.getMessage());
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
