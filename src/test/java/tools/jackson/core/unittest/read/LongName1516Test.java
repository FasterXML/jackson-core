package tools.jackson.core.unittest.read;

import java.util.List;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.sym.PropertyNameMatcher;
import tools.jackson.core.unittest.*;
import tools.jackson.core.util.Named;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reproduction test for:
 * <a href="https://github.com/FasterXML/jackson-core/issues/1516">Issue #1516</a>
 * <p>
 * Buffer overflow bug in {@code UTF8StreamJsonParser._matchLongName()} that causes
 * {@code ArrayIndexOutOfBoundsException} when parsing JSON with long names
 * (longer than 64 characters).
 */
public class LongName1516Test
    extends JacksonCoreTestBase
{
    /**
     * Test for the exact case mentioned in issue #1516:
     * A 65-character name should not cause ArrayIndexOutOfBoundsException
     */
    @Test
    void longName65Characters() throws Exception
    {
        _testLongName65Characters(MODE_READER);
        _testLongName65Characters(MODE_INPUT_STREAM);
        _testLongName65Characters(MODE_INPUT_STREAM_THROTTLED);
        _testLongName65Characters(MODE_DATA_INPUT);
    }

    private void _testLongName65Characters(int mode) throws Exception
    {
        // 65 character name as mentioned in the issue
        String longName = "01234567890123456789012345678901234567890123456789012345678901234";
        assertEquals(65, longName.length(), "Name should be exactly 65 characters");

        String json = "{\"a\": \"123\", \"" + longName + "\": \"value\"}";

        try (JsonParser p = createParser(mode, json)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
    
            // First property
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertEquals("a", p.currentName());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals("123", p.getString());
    
            // Second property with long name - this triggers the bug in 1516
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertEquals(longName, p.currentName());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals("value", p.getString());
    
            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }
    }

    /**
     * Test various long name lengths to verify buffer expansion works correctly
     */
    @Test
    void longNamesVariousLengths() throws Exception
    {
        _testLongNamesVariousLengths(MODE_READER);
        _testLongNamesVariousLengths(MODE_INPUT_STREAM);
        _testLongNamesVariousLengths(MODE_INPUT_STREAM_THROTTLED);
        _testLongNamesVariousLengths(MODE_DATA_INPUT);
    }

    private void _testLongNamesVariousLengths(int mode) throws Exception
    {
        // Test names of various lengths that could trigger buffer boundary issues
        int[] lengths = { 60, 64, 65, 70, 80, 100, 128, 200 };

        for (int len : lengths) {
            StringBuilder nameB = new StringBuilder(len);
            for (int i = 0; i < len; i++) {
                nameB.append((char)('0' + (i % 10)));
            }

            String name = nameB.toString();
            String json = "{\"" + name + "\": 42}";

            try (JsonParser p = createParser(mode, json)) {
                assertToken(JsonToken.START_OBJECT, p.nextToken());
                assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
                assertEquals(name, p.currentName(),
                    "Failed for name length: " + len);
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertEquals(42, p.getIntValue());
                assertToken(JsonToken.END_OBJECT, p.nextToken());
            }
        }
    }

    /**
     * Test multiple long names in the same document
     */
    @Test
    void multipleLongNames() throws Exception
    {
        _testMultipleLongNames(MODE_READER);
        _testMultipleLongNames(MODE_INPUT_STREAM);
        _testMultipleLongNames(MODE_INPUT_STREAM_THROTTLED);
        _testMultipleLongNames(MODE_DATA_INPUT);
    }

    private void _testMultipleLongNames(int mode) throws Exception
    {
        // Create multiple 65+ character names
        String name1 = "field1_" + "x".repeat(65);
        String name2 = "field2_" + "y".repeat(70);
        String name3 = "field3_" + "z".repeat(80);

        String json = "{\"" + name1 + "\": 1, \"" + name2 + "\": 2, \"" + name3 + "\": 3}";

        JsonParser p = createParser(mode, json);

        assertToken(JsonToken.START_OBJECT, p.nextToken());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals(name1, p.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(1, p.getIntValue());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals(name2, p.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(2, p.getIntValue());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals(name3, p.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(3, p.getIntValue());

        assertToken(JsonToken.END_OBJECT, p.nextToken());
        p.close();
    }

    /**
     * Test long names with UTF-8 multi-byte characters
     */
    @Test
    void longNamesWithUTF8() throws Exception
    {
        _testLongNamesWithUTF8(MODE_READER);
        _testLongNamesWithUTF8(MODE_INPUT_STREAM);
        _testLongNamesWithUTF8(MODE_INPUT_STREAM_THROTTLED);
        _testLongNamesWithUTF8(MODE_DATA_INPUT);
    }

    private void _testLongNamesWithUTF8(int mode) throws Exception
    {
        // 65+ character name with UTF-8 characters
        String name = "field_\u00E9\u00F1\u00FC_" + "a".repeat(60);
        assertTrue(name.length() >= 65, "Name should be at least 65 characters");

        String json = "{\"" + name + "\": \"test\"}";

        // Convert to UTF-8 bytes
        byte[] jsonBytes = utf8Bytes(json);

        try (JsonParser p = createParser(mode, jsonBytes)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertEquals(name, p.currentName());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals("test", p.getString());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }
    }

    /**
     * Test using PropertyNameMatcher with long names (65 characters)
     * This is the code path that triggers the bug in _matchLongName()
     */
    @Test
    void longNameWithMatcher65Chars() throws Exception
    {
        _testLongNameWithMatcher65Chars(MODE_READER);
        _testLongNameWithMatcher65Chars(MODE_INPUT_STREAM);
        _testLongNameWithMatcher65Chars(MODE_INPUT_STREAM_THROTTLED);
        _testLongNameWithMatcher65Chars(MODE_DATA_INPUT);
    }

    private void _testLongNameWithMatcher65Chars(int mode) throws Exception
    {
        JsonFactory f = newStreamFactory();

        // 65 character name as mentioned in the issue
        String longName = "01234567890123456789012345678901234567890123456789012345678901234";
        assertEquals(65, longName.length(), "Name should be exactly 65 characters");

        String json = "{\"a\": \"123\", \"" + longName + "\": \"value\"}";

        // Create matcher with both names
        PropertyNameMatcher matcher = f.constructNameMatcher(
            List.of(Named.fromString("a"), Named.fromString(longName)),
            false);

        try (JsonParser p = createParser(f, mode, json)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
    
            assertEquals(0, p.nextNameMatch(matcher));
            assertToken(JsonToken.PROPERTY_NAME, p.currentToken());
            assertEquals("a", p.currentName());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals("123", p.getString());
    
            // Second property with long name - this should trigger the bug in _matchLongName()
            assertEquals(1, p.nextNameMatch(matcher));
            assertToken(JsonToken.PROPERTY_NAME, p.currentToken());
            assertEquals(longName, p.currentName());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals("value", p.getString());
    
            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }
    }

    /**
     * Test using PropertyNameMatcher with multiple long names of various lengths
     */
    @Test
    void multipleNamesWithMatcher() throws Exception
    {
        _testMultipleNamesWithMatcher(MODE_READER);
        _testMultipleNamesWithMatcher(MODE_INPUT_STREAM);
        _testMultipleNamesWithMatcher(MODE_INPUT_STREAM_THROTTLED);
        _testMultipleNamesWithMatcher(MODE_DATA_INPUT);
    }

    private void _testMultipleNamesWithMatcher(int mode) throws Exception
    {
        JsonFactory f = newStreamFactory();

        // Names of different lengths, including 65+ characters
        String field1 = "shortField";
        String field2 = "field64chars_" + "x".repeat(52); // 64 chars
        String field3 = "field65chars_" + "y".repeat(52); // 65 chars
        String field4 = "field80chars_" + "z".repeat(67); // 80 chars

        String json = "{\"" + field1 + "\": 1, \"" + field2 + "\": 2, \""
                     + field3 + "\": 3, \"" + field4 + "\": 4}";

        PropertyNameMatcher matcher = f.constructNameMatcher(
            List.of(Named.fromString(field1), Named.fromString(field2),
                   Named.fromString(field3), Named.fromString(field4)),
            false);

        try (JsonParser p = createParser(f, mode, json)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
    
            assertEquals(0, p.nextNameMatch(matcher));
            assertEquals(field1, p.currentName());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(1, p.getIntValue());
    
            // Name 2 (64 chars)
            assertEquals(1, p.nextNameMatch(matcher));
            assertEquals(field2, p.currentName());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(2, p.getIntValue());
    
            // Name 3 (65 chars) - triggers buffer boundary
            assertEquals(2, p.nextNameMatch(matcher));
            assertEquals(field3, p.currentName());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(3, p.getIntValue());
    
            // Name 4 (80 chars) - should also work
            assertEquals(3, p.nextNameMatch(matcher));
            assertEquals(field4, p.currentName());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(4, p.getIntValue());
    
            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }
    }
}
