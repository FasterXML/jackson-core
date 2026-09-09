package tools.jackson.core.unittest.read;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.async.ByteArrayFeeder;
import tools.jackson.core.filter.FilteringParserDelegate;
import tools.jackson.core.filter.JsonPointerBasedFilter;
import tools.jackson.core.filter.TokenFilter;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.sym.PropertyNameMatcher;
import tools.jackson.core.util.JsonParserDelegate;
import tools.jackson.core.util.JsonParserSequence;
import tools.jackson.core.util.Named;
import tools.jackson.core.unittest.JacksonCoreTestBase;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@code JsonParser.nextNameMatchAndToken()}: on a non-negative
 * match the current token must be the property's value token; negative
 * results must leave the stream exactly as {@code nextNameMatch()} does.
 */
class NextNameMatchAndTokenTest extends JacksonCoreTestBase
{
    private static final String DOC = a2q(
            "{'a':'x','b':123,'c':true,'d':null,'e':[7],'f':{'g':2},'zz':5}");

    private final JsonFactory JSON_F = newStreamFactory();

    private PropertyNameMatcher matcher() {
        List<Named> names = Arrays.asList(
                Named.fromString("a"), Named.fromString("b"), Named.fromString("c"),
                Named.fromString("d"), Named.fromString("e"), Named.fromString("f"));
        return JSON_F.constructNameMatcher(names, true);
    }

    @Test
    void fusedMatchLeavesValueTokenCurrent() throws Exception
    {
        for (int mode : ALL_MODES) {
            PropertyNameMatcher m = matcher();
            try (JsonParser p = createParser(JSON_F, mode, DOC)) {
                assertToken(JsonToken.START_OBJECT, p.nextToken());

                assertEquals(0, p.nextNameMatchAndToken(m));
                assertToken(JsonToken.VALUE_STRING, p.currentToken());
                assertEquals("x", p.getString());

                assertEquals(1, p.nextNameMatchAndToken(m));
                assertToken(JsonToken.VALUE_NUMBER_INT, p.currentToken());
                assertEquals(123, p.getIntValue());

                assertEquals(2, p.nextNameMatchAndToken(m));
                assertToken(JsonToken.VALUE_TRUE, p.currentToken());

                assertEquals(3, p.nextNameMatchAndToken(m));
                assertToken(JsonToken.VALUE_NULL, p.currentToken());

                assertEquals(4, p.nextNameMatchAndToken(m));
                assertToken(JsonToken.START_ARRAY, p.currentToken());
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertEquals(7, p.getIntValue());
                assertToken(JsonToken.END_ARRAY, p.nextToken());

                assertEquals(5, p.nextNameMatchAndToken(m));
                assertToken(JsonToken.START_OBJECT, p.currentToken());
                p.skipChildren();

                assertEquals(PropertyNameMatcher.MATCH_UNKNOWN_NAME,
                        p.nextNameMatchAndToken(m));
                assertToken(JsonToken.PROPERTY_NAME, p.currentToken());
                assertEquals("zz", p.currentName());
                p.nextToken();
                p.skipChildren();

                assertEquals(PropertyNameMatcher.MATCH_END_OBJECT,
                        p.nextNameMatchAndToken(m));
                assertToken(JsonToken.END_OBJECT, p.currentToken());
            }
        }
    }

    @Test
    void fusedEquivalentToTwoCallSequence() throws Exception
    {
        for (int mode : ALL_MODES) {
            PropertyNameMatcher m1 = matcher();
            PropertyNameMatcher m2 = matcher();
            try (JsonParser fused = createParser(JSON_F, mode, DOC);
                    JsonParser twoCall = createParser(JSON_F, mode, DOC)) {
                fused.nextToken();
                twoCall.nextToken();
                // bound iteration so a regression fails instead of looping forever
                for (int i = 0, end = 20; ; ++i) {
                    assertTrue(i < end, "Failed to reach END_OBJECT in "+end+" rounds");
                    int ixF = fused.nextNameMatchAndToken(m1);
                    int ixT = twoCall.nextNameMatch(m2);
                    if (ixT >= 0) {
                        twoCall.nextToken();
                    }
                    assertEquals(ixT, ixF);
                    if (ixF == PropertyNameMatcher.MATCH_END_OBJECT) {
                        break;
                    }
                    if (ixF == PropertyNameMatcher.MATCH_UNKNOWN_NAME) {
                        fused.nextToken();
                        twoCall.nextToken();
                    }
                    assertToken(twoCall.currentToken(), fused.currentToken());
                    fused.skipChildren();
                    twoCall.skipChildren();
                }
            }
        }
    }

    // [core#1688]: delegating parsers must not lose state via default delegation

    @Test
    void fusedMatchViaParserDelegate() throws Exception
    {
        for (int mode : ALL_MODES) {
            PropertyNameMatcher m = matcher();
            try (JsonParser p = new JsonParserDelegate(createParser(JSON_F, mode, DOC))) {
                assertToken(JsonToken.START_OBJECT, p.nextToken());

                assertEquals(0, p.nextNameMatchAndToken(m));
                assertToken(JsonToken.VALUE_STRING, p.currentToken());
                assertEquals("x", p.getString());

                assertEquals(1, p.nextNameMatchAndToken(m));
                assertToken(JsonToken.VALUE_NUMBER_INT, p.currentToken());
                assertEquals(123, p.getIntValue());
            }
        }
    }

    @Test
    void fusedMatchViaParserSequence() throws Exception
    {
        PropertyNameMatcher m = matcher();
        JsonParser p1 = JSON_F.createParser(ObjectReadContext.empty(), a2q("{'a':'x'}"));
        JsonParser p2 = JSON_F.createParser(ObjectReadContext.empty(), a2q("{'b':123}"));
        try (JsonParser p = JsonParserSequence.createFlattened(false, p1, p2)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());

            assertEquals(0, p.nextNameMatchAndToken(m));
            assertToken(JsonToken.VALUE_STRING, p.currentToken());
            assertEquals("x", p.getString());

            assertEquals(PropertyNameMatcher.MATCH_END_OBJECT, p.nextNameMatchAndToken(m));

            // and this is where the second parser must be switched to
            assertEquals(PropertyNameMatcher.MATCH_ODD_TOKEN, p.nextNameMatchAndToken(m));
            assertToken(JsonToken.START_OBJECT, p.currentToken());

            assertEquals(1, p.nextNameMatchAndToken(m));
            assertToken(JsonToken.VALUE_NUMBER_INT, p.currentToken());
            assertEquals(123, p.getIntValue());

            assertEquals(PropertyNameMatcher.MATCH_END_OBJECT, p.nextNameMatchAndToken(m));
        }
    }

    @Test
    void fusedMatchViaFilteringDelegate() throws Exception
    {
        PropertyNameMatcher m = matcher();
        JsonParser p0 = JSON_F.createParser(ObjectReadContext.empty(),
                a2q("{'skip':1,'ob':{'a':'x','b':123}}"));
        try (JsonParser p = new FilteringParserDelegate(p0,
                new JsonPointerBasedFilter("/ob"),
                TokenFilter.Inclusion.ONLY_INCLUDE_ALL, false)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());

            assertEquals(0, p.nextNameMatchAndToken(m));
            assertToken(JsonToken.VALUE_STRING, p.currentToken());
            assertEquals("x", p.getString());

            assertEquals(1, p.nextNameMatchAndToken(m));
            assertToken(JsonToken.VALUE_NUMBER_INT, p.currentToken());
            assertEquals(123, p.getIntValue());

            assertEquals(PropertyNameMatcher.MATCH_END_OBJECT, p.nextNameMatchAndToken(m));
            assertToken(JsonToken.END_OBJECT, p.currentToken());
            assertNull(p.nextToken());
        }
    }

    // Non-blocking parsers cannot guarantee the value token is available even
    // on a match: verify the documented `NOT_AVAILABLE` behavior
    @Test
    void fusedMatchWithNonBlockingParser() throws Exception
    {
        PropertyNameMatcher m = matcher();
        final byte[] doc = utf8Bytes(a2q("{'b':1234}"));
        try (JsonParser p = JSON_F.createNonBlockingByteArrayParser(ObjectReadContext.empty())) {
            ByteArrayFeeder feeder = (ByteArrayFeeder) p.nonBlockingInputFeeder();
            // just `{"b":`, that is, name but no value yet
            feeder.feedInput(doc, 0, 5);
            assertToken(JsonToken.START_OBJECT, p.nextToken());

            assertEquals(1, p.nextNameMatchAndToken(m));
            assertToken(JsonToken.NOT_AVAILABLE, p.currentToken());

            feeder.feedInput(doc, 5, doc.length);
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(1234, p.getIntValue());
        }
    }
}
