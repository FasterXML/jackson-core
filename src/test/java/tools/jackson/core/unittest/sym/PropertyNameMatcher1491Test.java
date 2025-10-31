package tools.jackson.core.unittest.sym;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.sym.PropertyNameMatcher;
import tools.jackson.core.util.Named;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

// [core#1491]: PropertyNameMatcher collision
public class PropertyNameMatcher1491Test
    extends tools.jackson.core.unittest.JacksonCoreTestBase
{
    private final String KEY_1 = "aaaabbbbcccc";
    private final String KEY_2 = "aaaabbbbcccc2";

    private final Named NAMED_1 = Named.fromString(KEY_1);
    private final Named NAMED_2 = Named.fromString(KEY_2);

    private final String DOC_1491 = """
{
"%s": "v3",
"%s": "v4"
}
""".formatted(KEY_1, KEY_2);

    @Test
    void test1491ViaRegularParser() throws Exception {
        _testViaRegularParser(MODE_INPUT_STREAM);
        _testViaRegularParser(MODE_INPUT_STREAM_THROTTLED);
        _testViaRegularParser(MODE_READER);
    }

    private void _testViaRegularParser(int mode) throws Exception
    {
        try (JsonParser p = createParser(mode, DOC_1491)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertEquals(KEY_1, p.currentName());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals("v3", p.getString());
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertEquals(KEY_2, p.currentName());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals("v4", p.getString());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }
    }

    @Test
    void test1491ViaMatcher() throws Exception {
        JsonFactory f = newStreamFactory();
        _testViaMatcher(f, MODE_INPUT_STREAM);
        _testViaMatcher(f, MODE_INPUT_STREAM_THROTTLED);
        _testViaMatcher(f, MODE_READER);
    }

    private void _testViaMatcher(JsonFactory f, int mode) throws Exception
    {
        PropertyNameMatcher matcher = f.constructNameMatcher(List.of(NAMED_1, NAMED_2),
                false);

        try (JsonParser p = createParser(mode, DOC_1491)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());

            assertEquals(0, p.nextNameMatch(matcher));
            assertToken(JsonToken.PROPERTY_NAME, p.currentToken());
            assertEquals(KEY_1, p.currentName());

            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals("v3", p.getString());

            assertEquals(1, p.nextNameMatch(matcher));
            assertToken(JsonToken.PROPERTY_NAME, p.currentToken());
            assertEquals(KEY_2, p.currentName());

            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals("v4", p.getString());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }
    }
}
