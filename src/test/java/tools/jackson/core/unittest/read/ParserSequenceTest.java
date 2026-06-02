package tools.jackson.core.unittest.read;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.TreeNode;
import tools.jackson.core.unittest.*;
import tools.jackson.core.util.JsonParserSequence;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("resource")
class ParserSequenceTest
    extends JacksonCoreTestBase
{
    @Test
    void simple() throws Exception
    {
        JsonParser p1 = JSON_FACTORY.createParser(ObjectReadContext.empty(), "[ 1 ]");
        JsonParser p2 = JSON_FACTORY.createParser(ObjectReadContext.empty(), "[ 2 ]");
        JsonParserSequence seq = JsonParserSequence.createFlattened(false, p1, p2);
        assertEquals(2, seq.containedParsersCount());

        assertFalse(p1.isClosed());
        assertFalse(p2.isClosed());
        assertFalse(seq.isClosed());
        assertToken(JsonToken.START_ARRAY, seq.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, seq.nextToken());
        assertEquals(1, seq.getIntValue());
        assertToken(JsonToken.END_ARRAY, seq.nextToken());
        assertFalse(p1.isClosed());
        assertFalse(p2.isClosed());
        assertFalse(seq.isClosed());
        assertToken(JsonToken.START_ARRAY, seq.nextToken());

        // first parser ought to be closed now
        assertTrue(p1.isClosed());
        assertFalse(p2.isClosed());
        assertFalse(seq.isClosed());

        assertToken(JsonToken.VALUE_NUMBER_INT, seq.nextToken());
        assertEquals(2, seq.getIntValue());
        assertToken(JsonToken.END_ARRAY, seq.nextToken());
        assertTrue(p1.isClosed());
        assertFalse(p2.isClosed());
        assertFalse(seq.isClosed());

        assertNull(seq.nextToken());
        assertTrue(p1.isClosed());
        assertTrue(p2.isClosed());
        assertTrue(seq.isClosed());

        seq.close();
    }

    @Test
    void multiLevel() throws Exception
    {
        JsonParser p1 = JSON_FACTORY.createParser(ObjectReadContext.empty(), "[ 1 ] ");
        JsonParser p2 = JSON_FACTORY.createParser(ObjectReadContext.empty(), " 5");
        JsonParser p3 = JSON_FACTORY.createParser(ObjectReadContext.empty(), " { } ");
        JsonParserSequence seq1 = JsonParserSequence.createFlattened(true, p1, p2);
        JsonParserSequence seq = JsonParserSequence.createFlattened(false, seq1, p3);
        assertEquals(3, seq.containedParsersCount());

        assertToken(JsonToken.START_ARRAY, seq.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, seq.nextToken());
        assertToken(JsonToken.END_ARRAY, seq.nextToken());

        assertToken(JsonToken.VALUE_NUMBER_INT, seq.nextToken());

        assertToken(JsonToken.START_OBJECT, seq.nextToken());
        assertToken(JsonToken.END_OBJECT, seq.nextToken());

        assertNull(seq.nextToken());
        assertTrue(p1.isClosed());
        assertTrue(p2.isClosed());
        assertTrue(p3.isClosed());
        assertTrue(seq.isClosed());
    }

    // for [jackson-core#296]
    @Test
    void initializationDisabled() throws Exception
    {
        // // First, with old legacy settings

        JsonParser p1 = JSON_FACTORY.createParser(ObjectReadContext.empty(), "1 2");
        JsonParser p2 = JSON_FACTORY.createParser(ObjectReadContext.empty(), "3 true");
        assertToken(JsonToken.VALUE_NUMBER_INT, p1.nextToken());
        assertEquals(1, p1.getIntValue());
        assertToken(JsonToken.VALUE_NUMBER_INT, p2.nextToken());
        assertEquals(3, p2.getIntValue());

        // with legacy settings, will see neither '1' nor '3'

        JsonParserSequence seq = JsonParserSequence.createFlattened(false, p1, p2);
        assertToken(JsonToken.VALUE_NUMBER_INT, seq.nextToken());
        assertEquals(2, seq.getIntValue());
        assertToken(JsonToken.VALUE_TRUE, seq.nextToken());
        assertNull(seq.nextToken());
        seq.close();
    }

    // for [jackson-core#296]
    @Test
    void initializationEnabled() throws Exception
    {
        // // and then with new "check for current":
        JsonParser p1 = JSON_FACTORY.createParser(ObjectReadContext.empty(), "1 2");
        JsonParser p2 = JSON_FACTORY.createParser(ObjectReadContext.empty(), "3 true");
        assertToken(JsonToken.VALUE_NUMBER_INT, p1.nextToken());
        assertEquals(1, p1.getIntValue());
        assertToken(JsonToken.VALUE_NUMBER_INT, p2.nextToken());
        assertEquals(3, p2.getIntValue());

        // with new settings, both '1' and '3' will be visible

        JsonParserSequence seq = JsonParserSequence.createFlattened(true, p1, p2);
        assertToken(JsonToken.VALUE_NUMBER_INT, seq.nextToken());
        assertEquals(1, seq.getIntValue());
        assertToken(JsonToken.VALUE_NUMBER_INT, seq.nextToken());
        assertEquals(2, seq.getIntValue());
        assertToken(JsonToken.VALUE_NUMBER_INT, seq.nextToken());
        assertEquals(3, seq.getIntValue());
        assertToken(JsonToken.VALUE_TRUE, seq.nextToken());
        assertNull(seq.nextToken());
        seq.close();
    }

    // [jackson-core#1616]: read methods (readValueAsTree / readValueAs) must
    // drive databind through the sequence's own token stream, so that parsers
    // beyond the first one are actually used.
    @Test
    void readValueAsTreeUsesAllParsers() throws Exception
    {
        CountingReadContext ctxt = new CountingReadContext();
        // First parser yields 3 tokens, second yields 2: 5 total
        JsonParser p1 = JSON_FACTORY.createParser(ctxt, "1 2 3");
        JsonParser p2 = JSON_FACTORY.createParser(ctxt, "4 5");
        JsonParserSequence seq = JsonParserSequence.createFlattened(false, p1, p2);

        seq.readValueAsTree();
        assertEquals(5, ctxt.tokenCount,
                "readValueAsTree() must consume tokens from all parsers in sequence");
        seq.close();
    }

    // [jackson-core#1616]: same, but mirroring the actual failing scenario more
    // closely -- as in `AsPropertyTypeDeserializer`, the sequence is built with
    // `checkForExistingToken=true` over a first parser that already points at a
    // token (a `TokenBuffer` in real usage). The existing-token path must still
    // continue into the remaining parsers.
    @Test
    void readValueAsTreeUsesAllParsersWithExistingToken() throws Exception
    {
        CountingReadContext ctxt = new CountingReadContext();
        JsonParser p1 = JSON_FACTORY.createParser(ctxt, "1 2 3");
        JsonParser p2 = JSON_FACTORY.createParser(ctxt, "4 5");
        // advance first parser so it already points at a token before sequencing
        assertToken(JsonToken.VALUE_NUMBER_INT, p1.nextToken());
        JsonParserSequence seq = JsonParserSequence.createFlattened(true, p1, p2);

        seq.readValueAsTree();
        assertEquals(5, ctxt.tokenCount,
                "readValueAsTree() must consume tokens from all parsers in sequence");
        seq.close();
    }

    // [jackson-core#1616]: the same routing must apply to the non-tree
    // `readValueAs(...)` overloads, not just `readValueAsTree()`.
    @Test
    void readValueAsUsesAllParsers() throws Exception
    {
        CountingReadContext ctxt = new CountingReadContext();
        JsonParser p1 = JSON_FACTORY.createParser(ctxt, "1 2 3");
        JsonParser p2 = JSON_FACTORY.createParser(ctxt, "4 5");
        JsonParserSequence seq = JsonParserSequence.createFlattened(false, p1, p2);

        seq.readValueAs(Object.class);
        assertEquals(5, ctxt.tokenCount,
                "readValueAs(Class) must consume tokens from all parsers in sequence");
        seq.close();
    }

    // Helper context whose databind callbacks drain (and count) every token of
    // the parser handed to them; mimics how real databind drives the parser.
    static class CountingReadContext extends ObjectReadContext.Base {
        int tokenCount;

        private void _drain(JsonParser p) {
            tokenCount = 0;
            while (p.nextToken() != null) {
                ++tokenCount;
            }
        }

        @Override
        public <T extends TreeNode> T readTree(JsonParser p) {
            _drain(p);
            return null;
        }

        @Override
        public <T> T readValue(JsonParser p, Class<T> valueType) {
            _drain(p);
            return null;
        }
    }
}
