package tools.jackson.core.unittest.constraints;

import java.io.*;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.core.json.JsonFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Nesting Depth Constraint Bypass in UTF8DataInputJsonParser
 */
class DeeplyNestedContentViaDataInputTest
{
    private static final int TEST_NESTING_DEPTH = 5000;

    private final JsonFactory factory = new JsonFactory();

    // [core#1553] Regression; works in 2.x
    @Test
    void dataInputParserBypassesNestingDepth() throws Exception {
        byte[] data = buildNestedArrays(TEST_NESTING_DEPTH);
        DataInput di = new DataInputStream(new ByteArrayInputStream(data));
        int maxDepth = 0;
        try (JsonParser p = factory.createParser(ObjectReadContext.empty(), di)) {
            while (p.nextToken() != null) {
                if (p.currentToken() == JsonToken.START_ARRAY) {
                    maxDepth++;
                }
            }
            fail("DataInput parser must reject nesting depth " + TEST_NESTING_DEPTH+", got "+maxDepth);
        } catch (StreamConstraintsException e) {
            assertTrue(e.getMessage().contains("Document nesting depth"),
                    "Unexpected exception message: " + e.getMessage());
        }
    }

    private byte[] buildNestedArrays(int depth) {
        StringBuilder sb = new StringBuilder(depth * 2);
        for (int i = 0; i < depth; i++) sb.append('[');
        for (int i = 0; i < depth; i++) sb.append(']');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
