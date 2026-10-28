package tools.jackson.core.unittest.read;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.unittest.JacksonCoreTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for [jackson-core#1784]: a property name containing a 0xFF byte must be
 * rejected even when it pads to the same quads as a name already in the symbol table.
 */
class PropertyName0xFF1784Test extends JacksonCoreTestBase
{
    // Lengths 1, 3, 4, 6 and 9 give last quads of 1, 3, 4, 2 and 1 bytes
    private static final String[] NAMES = { "a", "abc", "abcd", "abcdef", "abcdefghi" };

    private final JsonFactory APOS_FACTORY = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .build();

    private final JsonFactory UNQUOTED_FACTORY = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_UNQUOTED_PROPERTY_NAMES)
            .build();

    // Valid name first in the same document, so it is in the child symbol table
    @Test
    void ffAfterSameNameInDocument() throws Exception
    {
        for (int mode : ALL_BINARY_MODES) {
            for (String name : NAMES) {
                for (int pos = 0; pos <= name.length(); ++pos) {
                    String invalid = withFF(name, pos);
                    _verifyRejected(newStreamFactory(), mode, name, invalid, "\"");
                    _verifyRejected(APOS_FACTORY, mode, name, invalid, "'");
                    if (pos > 0) { // unquoted name cannot start with 0xFF
                        _verifyRejected(UNQUOTED_FACTORY, mode, name, invalid, "");
                    }
                }
            }
        }
    }

    // Valid name in an earlier document, so it is merged into the root symbol table
    @Test
    void ffAfterSameNameInEarlierDocument() throws Exception
    {
        for (int mode : ALL_BINARY_MODES) {
            for (String name : NAMES) {
                JsonFactory f = newStreamFactory();
                try (JsonParser p = createParser(f, mode, "{\"" + name + "\":1}")) {
                    while (p.nextToken() != null) { }
                }
                try (JsonParser p = createParser(f, mode, doc("{\"" + withFF(name, name.length() - 1) + "\":2}"))) {
                    assertToken(JsonToken.START_OBJECT, p.nextToken());
                    p.nextToken();
                    fail("Should not pass, got name '" + p.currentName() + "'");
                } catch (StreamReadException e) {
                    verifyException(e, "Invalid UTF-8 start byte 0xff");
                }
            }
        }
    }

    private void _verifyRejected(JsonFactory f, int mode, String name, String invalid, String q)
        throws Exception
    {
        try (JsonParser p = createParser(f, mode, doc("{" + q + name + q + ":1," + q + invalid + q + ":2}"))) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertEquals(name, p.currentName());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            p.nextToken();
            fail("Should not pass, got name '" + p.currentName() + "'");
        } catch (StreamReadException e) {
            verifyException(e, "Invalid UTF-8 start byte 0xff");
        }
    }

    // '~' in result marks the 0xFF byte, see doc()
    static String withFF(String name, int pos) {
        return new StringBuilder(name).insert(pos, '~').toString();
    }

    static byte[] doc(String json) {
        return json.replace('~', 'ÿ').getBytes(StandardCharsets.ISO_8859_1);
    }
}
