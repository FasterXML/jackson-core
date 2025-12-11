package tools.jackson.core.unittest.read;

import java.io.*;
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
 * {@code ArrayIndexOutOfBoundsException} when parsing JSON with long field names
 * (longer than 64 characters).
 */
public class LongName1516Test
    extends JacksonCoreTestBase
{
    /**
     * Test for the exact case mentioned in issue #1516:
     * A 65-character field name should not cause ArrayIndexOutOfBoundsException
     */
    @Test
    void longFieldName65Characters() throws Exception
    {
        _testLongFieldName65Characters(MODE_INPUT_STREAM);
        _testLongFieldName65Characters(MODE_INPUT_STREAM_THROTTLED);
        _testLongFieldName65Characters(MODE_DATA_INPUT);
    }

    private void _testLongFieldName65Characters(int mode) throws Exception
    {
        // 65 character field name as mentioned in the issue
        String fieldName = "01234567890123456789012345678901234567890123456789012345678901234";
        assertEquals(65, fieldName.length(), "Field name should be exactly 65 characters");

        String json = "{\"a\": \"123\", \"" + fieldName + "\": \"value\"}";

        JsonParser p = createParser(mode, json);

        assertToken(JsonToken.START_OBJECT, p.nextToken());

        // First field
        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("a", p.currentName());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("123", p.getString());

        // Second field with long name - this triggers the bug in 1516
        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals(fieldName, p.currentName());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("value", p.getString());

        assertToken(JsonToken.END_OBJECT, p.nextToken());
        p.close();
    }

    /**
     * Test various long field name lengths to verify buffer expansion works correctly
     */
    @Test
    void longFieldNamesVariousLengths() throws Exception
    {
        _testLongFieldNamesVariousLengths(MODE_INPUT_STREAM);
        _testLongFieldNamesVariousLengths(MODE_INPUT_STREAM_THROTTLED);
        _testLongFieldNamesVariousLengths(MODE_DATA_INPUT);
    }

    private void _testLongFieldNamesVariousLengths(int mode) throws Exception
    {
        // Test field names of various lengths that could trigger buffer boundary issues
        int[] lengths = { 60, 64, 65, 70, 80, 100, 128, 200 };

        for (int len : lengths) {
            StringBuilder fieldName = new StringBuilder(len);
            for (int i = 0; i < len; i++) {
                fieldName.append((char)('0' + (i % 10)));
            }

            String name = fieldName.toString();
            String json = "{\"" + name + "\": 42}";

            JsonParser p = createParser(mode, json);

            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertEquals(name, p.currentName(),
                "Failed for field name length: " + len);
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(42, p.getIntValue());
            assertToken(JsonToken.END_OBJECT, p.nextToken());

            p.close();
        }
    }

    /**
     * Test multiple long field names in the same document
     */
    @Test
    void multipleLongFieldNames() throws Exception
    {
        _testMultipleLongFieldNames(MODE_INPUT_STREAM);
        _testMultipleLongFieldNames(MODE_INPUT_STREAM_THROTTLED);
        _testMultipleLongFieldNames(MODE_DATA_INPUT);
    }

    private void _testMultipleLongFieldNames(int mode) throws Exception
    {
        // Create multiple 65+ character field names
        String field1 = "field1_" + "x".repeat(65);
        String field2 = "field2_" + "y".repeat(70);
        String field3 = "field3_" + "z".repeat(80);

        String json = "{\"" + field1 + "\": 1, \"" + field2 + "\": 2, \"" + field3 + "\": 3}";

        JsonParser p = createParser(mode, json);

        assertToken(JsonToken.START_OBJECT, p.nextToken());

        // Field 1
        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals(field1, p.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(1, p.getIntValue());

        // Field 2
        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals(field2, p.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(2, p.getIntValue());

        // Field 3
        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals(field3, p.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(3, p.getIntValue());

        assertToken(JsonToken.END_OBJECT, p.nextToken());
        p.close();
    }

    /**
     * Test long field names with UTF-8 multi-byte characters
     */
    @Test
    void longFieldNamesWithUTF8() throws Exception
    {
        _testLongFieldNamesWithUTF8(MODE_INPUT_STREAM);
        _testLongFieldNamesWithUTF8(MODE_INPUT_STREAM_THROTTLED);
        _testLongFieldNamesWithUTF8(MODE_DATA_INPUT);
    }

    private void _testLongFieldNamesWithUTF8(int mode) throws Exception
    {
        // 65+ character field name with UTF-8 characters
        String fieldName = "field_\u00E9\u00F1\u00FC_" + "a".repeat(60);
        assertTrue(fieldName.length() >= 65, "Field name should be at least 65 characters");

        String json = "{\"" + fieldName + "\": \"test\"}";

        // Convert to UTF-8 bytes
        byte[] jsonBytes = json.getBytes("UTF-8");

        JsonParser p = createParser(mode, jsonBytes);

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals(fieldName, p.currentName());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("test", p.getString());
        assertToken(JsonToken.END_OBJECT, p.nextToken());

        p.close();
    }

    /**
     * Test using PropertyNameMatcher with long field names (65 characters)
     * This is the code path that triggers the bug in _matchLongName()
     */
    @Test
    void longFieldNameWithMatcher65Chars() throws Exception
    {
        _testLongFieldNameWithMatcher65Chars(MODE_INPUT_STREAM);
        _testLongFieldNameWithMatcher65Chars(MODE_INPUT_STREAM_THROTTLED);
        _testLongFieldNameWithMatcher65Chars(MODE_DATA_INPUT);
    }

    private void _testLongFieldNameWithMatcher65Chars(int mode) throws Exception
    {
        JsonFactory f = newStreamFactory();

        // 65 character field name as mentioned in the issue
        String longFieldName = "01234567890123456789012345678901234567890123456789012345678901234";
        assertEquals(65, longFieldName.length(), "Field name should be exactly 65 characters");

        String json = "{\"a\": \"123\", \"" + longFieldName + "\": \"value\"}";

        // Create matcher with both field names
        PropertyNameMatcher matcher = f.constructNameMatcher(
            List.of(Named.fromString("a"), Named.fromString(longFieldName)),
            false);

        JsonParser p = createParser(f, mode, json);

        assertToken(JsonToken.START_OBJECT, p.nextToken());

        // First field
        assertEquals(0, p.nextNameMatch(matcher));
        assertToken(JsonToken.PROPERTY_NAME, p.currentToken());
        assertEquals("a", p.currentName());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("123", p.getString());

        // Second field with long name - this should trigger the bug in _matchLongName()
        assertEquals(1, p.nextNameMatch(matcher));
        assertToken(JsonToken.PROPERTY_NAME, p.currentToken());
        assertEquals(longFieldName, p.currentName());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("value", p.getString());

        assertToken(JsonToken.END_OBJECT, p.nextToken());
        p.close();
    }

    /**
     * Test using PropertyNameMatcher with multiple long field names of various lengths
     */
    @Test
    void multipleFieldNamesWithMatcher() throws Exception
    {
        _testMultipleFieldNamesWithMatcher(MODE_INPUT_STREAM);
        _testMultipleFieldNamesWithMatcher(MODE_INPUT_STREAM_THROTTLED);
        _testMultipleFieldNamesWithMatcher(MODE_DATA_INPUT);
    }

    private void _testMultipleFieldNamesWithMatcher(int mode) throws Exception
    {
        JsonFactory f = newStreamFactory();

        // Field names of different lengths, including 65+ characters
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

        JsonParser p = createParser(f, mode, json);

        assertToken(JsonToken.START_OBJECT, p.nextToken());

        // Field 1
        assertEquals(0, p.nextNameMatch(matcher));
        assertEquals(field1, p.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(1, p.getIntValue());

        // Field 2 (64 chars)
        assertEquals(1, p.nextNameMatch(matcher));
        assertEquals(field2, p.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(2, p.getIntValue());

        // Field 3 (65 chars) - triggers buffer boundary
        assertEquals(2, p.nextNameMatch(matcher));
        assertEquals(field3, p.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(3, p.getIntValue());

        // Field 4 (80 chars) - should also work
        assertEquals(3, p.nextNameMatch(matcher));
        assertEquals(field4, p.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(4, p.getIntValue());

        assertToken(JsonToken.END_OBJECT, p.nextToken());
        p.close();
    }
}
