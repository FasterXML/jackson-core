package com.fasterxml.jackson.core.read;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamReadException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for [jackson-core#363]: UTF-8 parser should reject 3-byte UTF-8 sequences
 * that encode surrogate code points (U+D800 to U+DFFF), which are illegal in UTF-8.
 */
class UTF8SurrogateValidation363Test extends JUnit5TestBase
{
    private final JsonFactory FACTORY = newStreamFactory();

    /**
     * Test that parser rejects 3-byte UTF-8 sequence encoding U+D800 (start of surrogate range).
     * In UTF-8, U+D800 would be encoded as: ED A0 80
     */
    @Test
    void rejectSurrogateD800InString() throws Exception
    {
        // JSON: {"value":"X"}
        // where X is the invalid 3-byte sequence ED A0 80 (U+D800)
        byte[] doc = new byte[] {
            '{', '"', 'v', 'a', 'l', 'u', 'e', '"', ':',
            '"',
            (byte) 0xED, (byte) 0xA0, (byte) 0x80, // Invalid: U+D800 surrogate
            '"',
            '}'
        };

        try (JsonParser p = FACTORY.createParser(doc)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            assertEquals("value", p.currentName());

            // This should fail when trying to read the string value
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            p.getText(); // Actual parsing happens here  (lazy parsing)
            fail("Should have thrown an exception for surrogate code point in UTF-8");
        } catch (StreamReadException e) {
            verifyException(e, "Invalid UTF-8");
        }
    }

    /**
     * Test that parser rejects 3-byte UTF-8 sequence encoding U+DFFF (end of surrogate range).
     * In UTF-8, U+DFFF would be encoded as: ED BF BF
     */
    @Test
    void rejectSurrogateDFFFInString() throws Exception
    {
        // JSON: {"value":"X"}
        // where X is the invalid 3-byte sequence ED BF BF (U+DFFF)
        byte[] doc = new byte[] {
            '{', '"', 'v', 'a', 'l', 'u', 'e', '"', ':',
            '"',
            (byte) 0xED, (byte) 0xBF, (byte) 0xBF, // Invalid: U+DFFF surrogate
            '"',
            '}'
        };

        try (JsonParser p = FACTORY.createParser(doc)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            assertEquals("value", p.currentName());

            // This should fail when trying to read the string value
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            p.getText(); // Actual parsing happens here  (lazy parsing)
            fail("Should have thrown an exception for surrogate code point in UTF-8");
        } catch (StreamReadException e) {
            verifyException(e, "Invalid UTF-8");
        }
    }

    /**
     * Test that parser rejects 3-byte UTF-8 sequence encoding U+DABC (middle of surrogate range).
     * In UTF-8, U+DABC would be encoded as: ED AA BC
     */
    @Test
    void rejectSurrogateMiddleInString() throws Exception
    {
        // JSON: {"value":"X"}
        // where X is the invalid 3-byte sequence ED AA BC (U+DABC)
        byte[] doc = new byte[] {
            '{', '"', 'v', 'a', 'l', 'u', 'e', '"', ':',
            '"',
            (byte) 0xED, (byte) 0xAA, (byte) 0xBC, // Invalid: U+DABC surrogate
            '"',
            '}'
        };

        try (JsonParser p = FACTORY.createParser(doc)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            assertEquals("value", p.currentName());

            // This should fail when trying to read the string value
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            p.getText(); // Actual parsing happens here  (lazy parsing)
            fail("Should have thrown an exception for surrogate code point in UTF-8");
        } catch (StreamReadException e) {
            verifyException(e, "Invalid UTF-8");
        }
    }

    /**
     * Test that parser rejects surrogate in field name as well.
     */
    @Test
    void rejectSurrogateInFieldName() throws Exception
    {
        // JSON: {"X":"value"}
        // where X is the invalid 3-byte sequence ED A0 80 (U+D800)
        byte[] doc = new byte[] {
            '{', '"',
            (byte) 0xED, (byte) 0xA0, (byte) 0x80, // Invalid: U+D800 surrogate
            '"', ':', '"', 'v', 'a', 'l', 'u', 'e', '"',
            '}'
        };

        try (JsonParser p = FACTORY.createParser(doc)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());

            // This should fail when trying to read the field name
            // (no lazy parsing for names)
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            fail("Should have thrown an exception for surrogate code point in UTF-8");
        } catch (StreamReadException e) {
            verifyException(e, "Invalid UTF-8");
        }
    }

    /**
     * Sanity check: valid 3-byte UTF-8 sequences just before surrogate range should work.
     * U+D7FF is the last valid code point before the surrogate range.
     * In UTF-8: ED 9F BF
     */
    @Test
    void acceptValidBeforeSurrogateRange() throws Exception
    {
        // JSON: {"value":"X"}
        // where X is the valid 3-byte sequence ED 9F BF (U+D7FF)
        byte[] doc = new byte[] {
            '{', '"', 'v', 'a', 'l', 'u', 'e', '"', ':',
            '"',
            (byte) 0xED, (byte) 0x9F, (byte) 0xBF, // Valid: U+D7FF (just before surrogates)
            '"',
            '}'
        };

        try (JsonParser p = FACTORY.createParser(doc)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            assertEquals("value", p.currentName());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals("\uD7FF", p.getText());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }
    }

    /**
     * Sanity check: valid 3-byte UTF-8 sequences just after surrogate range should work.
     * U+E000 is the first valid code point after the surrogate range.
     * In UTF-8: EE 80 80
     */
    @Test
    void acceptValidAfterSurrogateRange() throws Exception
    {
        // JSON: {"value":"X"}
        // where X is the valid 3-byte sequence EE 80 80 (U+E000)
        byte[] doc = new byte[] {
            '{', '"', 'v', 'a', 'l', 'u', 'e', '"', ':',
            '"',
            (byte) 0xEE, (byte) 0x80, (byte) 0x80, // Valid: U+E000 (just after surrogates)
            '"',
            '}'
        };

        try (JsonParser p = FACTORY.createParser(doc)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            assertEquals("value", p.currentName());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals("\uE000", p.getText());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }
    }
}
