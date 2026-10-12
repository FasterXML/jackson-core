package tools.jackson.core.unittest.read;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.async.ByteArrayFeeder;
import tools.jackson.core.filter.FilteringParserDelegate;
import tools.jackson.core.filter.JsonPointerBasedFilter;
import tools.jackson.core.filter.TokenFilter;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonReadFeature;
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

    // Names that miss the fast quad-matching path: long, non-ASCII, escaped;
    // plus assorted value types and nesting
    @Test
    void fusedEquivalentForVariedNamesAndValues() throws Exception
    {
        List<String> trace = _assertEquivalent(JSON_F, a2q(
                "{'longPropertyName_abcdefghij':'long',"
                +"'näme':-12,"
                +"'日本':1.5e10,"
                +"'\\u0061b':12345678901234567890,"
                +"'\\u0061':'escaped',"
                +"'unknownLongPropertyName_xyz':[1,{'a':true},[],{}],"
                +"'b':'with \\\"escapes\\\" \\n',"
                +"'ab' :\t{ 'a' : [ null , false ] },"
                +"'x':-0.25,"
                +"'b':{}}"));
        // sanity check: fused branch actually taken for slow-path names
        assertTrue(trace.contains("match:3/VALUE_STRING/longPropertyName_abcdefghij"), trace.toString());
        assertTrue(trace.contains("match:4/VALUE_NUMBER_INT/näme"), trace.toString());
        assertTrue(trace.contains("match:5/VALUE_NUMBER_FLOAT/日本"), trace.toString());
    }

    // Enough content to cross input buffer boundaries in non-throttled modes too
    @Test
    void fusedEquivalentForLongDoc() throws Exception
    {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < 1000; ++i) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(a2q("'longPropertyName_abcdefghij':'value "+i+"',"
                    +"'n"+i+"':"+i+",'a':["+i+"],'näme':{'b':-"+i+".5}"));
        }
        _assertEquivalent(JSON_F, sb.append('}').toString());
    }

    @Test
    void fusedEquivalentWithTrailingCommas() throws Exception
    {
        JsonFactory f = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
                .build();
        _assertEquivalent(f, a2q("{'a':1,'b':[1,2,],'f':{'g':2,},'e':{},}"));
        _assertEquivalent(f, a2q("{'zz':1,}"));
    }

    @Test
    void fusedEquivalentWithNonStandardNames() throws Exception
    {
        JsonFactory f = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_UNQUOTED_PROPERTY_NAMES)
                .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
                .build();
        _assertEquivalent(f, "{a:1,'b':'x',zz:2,ab:[1],'c':true,d:{e:null}}");
    }

    @Test
    void fusedEquivalentWithNonStandardNumbers() throws Exception
    {
        JsonFactory f = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS)
                .enable(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS)
                .enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)
                .build();
        _assertEquivalent(f, a2q("{'a':+1,'b':.5,'zz':+2.5,'c':NaN,'d':-Infinity}"));
    }

    // Failures while decoding the value after a (matched or unknown) name
    @Test
    void fusedEquivalentForInvalidContent() throws Exception
    {
        final String[] docs = {
                "{'a':tru}", "{'zz':nul}", "{'b':fals}",
                "{'a':+1}", "{'a':.5}", "{'a':NaN}", "{'a':x}",
                "{'a':1 'b':2}", "{'a' 1}", "{'a':1,}", "{'b':'unterminated"
        };
        for (String doc : docs) {
            List<String> trace = _assertEquivalent(JSON_F, a2q(doc));
            assertTrue(trace.get(trace.size()-1).startsWith("error:"),
                    "Expected failure for "+doc+", got: "+trace);
        }
    }

    private List<String> _assertEquivalent(JsonFactory f, String doc) throws Exception
    {
        List<String> result = null;
        for (int mode : ALL_MODES) {
            List<String> fused, twoCall;
            try (JsonParser p = createParser(f, mode, doc)) {
                fused = _trace(p, true);
            }
            try (JsonParser p = createParser(f, mode, doc)) {
                twoCall = _trace(p, false);
            }
            assertEquals(twoCall, fused, "Mode "+mode+", doc: "+doc);
            result = fused;
        }
        return result;
    }

    // Records tokens, names, values and match results until end or first failure
    private List<String> _trace(JsonParser p, boolean fused)
    {
        List<String> events = new ArrayList<>();
        PropertyNameMatcher m = JSON_F.constructNameMatcher(Arrays.asList(
                Named.fromString("a"), Named.fromString("b"), Named.fromString("ab"),
                Named.fromString("longPropertyName_abcdefghij"),
                Named.fromString("näme"), Named.fromString("日本")),
                true);
        try {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            _traceObject(p, m, fused, events);
            events.add("end:"+p.nextToken());
        } catch (JacksonException e) {
            events.add("error: "+e.getOriginalMessage());
        }
        return events;
    }

    private void _traceObject(JsonParser p, PropertyNameMatcher m, boolean fused,
            List<String> events)
    {
        while (true) {
            int ix;
            if (fused) {
                ix = p.nextNameMatchAndToken(m);
            } else {
                ix = p.nextNameMatch(m);
                if (ix >= 0) {
                    p.nextToken();
                }
            }
            events.add("match:"+ix+"/"+p.currentToken()+"/"+p.currentName());
            if (ix == PropertyNameMatcher.MATCH_END_OBJECT
                    || ix == PropertyNameMatcher.MATCH_ODD_TOKEN) {
                return;
            }
            if (ix == PropertyNameMatcher.MATCH_UNKNOWN_NAME) {
                p.nextToken();
            }
            _traceValue(p, m, fused, events);
        }
    }

    private void _traceValue(JsonParser p, PropertyNameMatcher m, boolean fused,
            List<String> events)
    {
        JsonToken t = p.currentToken();
        if (t == JsonToken.START_OBJECT) {
            events.add("{");
            _traceObject(p, m, fused, events);
        } else if (t == JsonToken.START_ARRAY) {
            events.add("[");
            while ((t = p.nextToken()) != JsonToken.END_ARRAY) {
                if (t == null) {
                    events.add("eof");
                    return;
                }
                _traceValue(p, m, fused, events);
            }
            events.add("]");
        } else if (t != null && t.isNumeric()) {
            events.add(t+":"+p.getNumberValue());
        } else {
            events.add(t+":"+p.getString());
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

    // Delegate sub-classes that override `nextNameMatch()` (but not the fused
    // method) must still have their override called
    @Test
    void fusedMatchViaParserDelegateSubclass() throws Exception
    {
        for (int mode : ALL_MODES) {
            PropertyNameMatcher m = matcher();
            try (JsonParser p = new RenamingDelegate(createParser(JSON_F, mode,
                    a2q("{'legacy':'x','b':123}")))) {
                assertToken(JsonToken.START_OBJECT, p.nextToken());

                assertEquals(0, p.nextNameMatchAndToken(m));
                assertToken(JsonToken.VALUE_STRING, p.currentToken());
                assertEquals("x", p.getString());

                assertEquals(1, p.nextNameMatchAndToken(m));
                assertToken(JsonToken.VALUE_NUMBER_INT, p.currentToken());
                assertEquals(123, p.getIntValue());

                assertEquals(PropertyNameMatcher.MATCH_END_OBJECT, p.nextNameMatchAndToken(m));
            }
        }
    }

    static class RenamingDelegate extends JsonParserDelegate
    {
        RenamingDelegate(JsonParser p) { super(p); }

        @Override
        public String currentName() {
            String name = delegate.currentName();
            return "legacy".equals(name) ? "a" : name;
        }

        @Override
        public int nextNameMatch(PropertyNameMatcher matcher) {
            JsonToken t = nextToken();
            if (t == JsonToken.PROPERTY_NAME) {
                return matcher.matchName(currentName());
            }
            return (t == JsonToken.END_OBJECT) ? PropertyNameMatcher.MATCH_END_OBJECT
                    : PropertyNameMatcher.MATCH_ODD_TOKEN;
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
