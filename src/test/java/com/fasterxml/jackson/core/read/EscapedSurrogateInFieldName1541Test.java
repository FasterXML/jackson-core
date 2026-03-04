package com.fasterxml.jackson.core.read;

import java.io.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for [jackson-core#1541]: JSON-escaped surrogate pairs (\ud83d\udc4d) in
 * field names should be decoded correctly by the UTF-8 stream parser.
 */
class EscapedSurrogateInFieldName1541Test extends JUnit5TestBase
{
    private final JsonFactory FACTORY = newStreamFactory();

    /**
     * Test that JSON-escaped surrogate pair in field name is accepted when
     * parsing from a byte stream (UTF-8 parser path).
     */
    @Test
    void escapedSurrogatePairInFieldNameBytes() throws Exception
    {
        // JSON: {"\ud83d\udc4d":"value"}
        byte[] doc = new byte[] {
            '{', '"',
            '\\', 'u', 'd', '8', '3', 'd',  // JSON escape: \ud83d (high surrogate)
            '\\', 'u', 'd', 'c', '4', 'd',  // JSON escape: \udc4d (low surrogate)
            '"', ':', '"', 'v', 'a', 'l', 'u', 'e', '"',
            '}'
        };

        try (JsonParser p = FACTORY.createParser(doc)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            // The escaped surrogate pair should decode to U+1F44D (thumbs up emoji)
            assertEquals("\uD83D\uDC4D", p.currentName());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals("value", p.getText());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }
    }

    /**
     * Test that JSON-escaped surrogate pair in field name is accepted when
     * parsing from an InputStream (streaming UTF-8 parser path).
     */
    @Test
    void escapedSurrogatePairInFieldNameStream() throws Exception
    {
        // JSON: {"\ud83d\udc4d":"value"}
        byte[] doc = new byte[] {
            '{', '"',
            '\\', 'u', 'd', '8', '3', 'd',
            '\\', 'u', 'd', 'c', '4', 'd',
            '"', ':', '"', 'v', 'a', 'l', 'u', 'e', '"',
            '}'
        };

        try (JsonParser p = FACTORY.createParser(new ByteArrayInputStream(doc))) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            assertEquals("\uD83D\uDC4D", p.currentName());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals("value", p.getText());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }
    }

    /**
     * Test using the exact repro from issue #1566: write supplementary character
     * with generator, then read it back with streaming parser.
     */
    @Test
    void surrogateRoundTripFromIssue1566() throws Exception
    {
        // U+1F44D is a supplementary character, stored in Java as surrogate pair \uD83D\uDC4D
        String fieldName = "\uD83D\uDC4D";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (JsonGenerator gen = FACTORY.createGenerator(out, JsonEncoding.UTF8)) {
            gen.writeStartObject();
            gen.writeStringField(fieldName, "value");
            gen.writeEndObject();
        }

        byte[] json = out.toByteArray();

        try (JsonParser parser = FACTORY.createParser(new ByteArrayInputStream(json))) {
            assertToken(JsonToken.START_OBJECT, parser.nextToken());
            assertToken(JsonToken.FIELD_NAME, parser.nextToken());
            assertEquals(fieldName, parser.currentName());
            assertToken(JsonToken.VALUE_STRING, parser.nextToken());
            assertEquals("value", parser.getText());
            assertToken(JsonToken.END_OBJECT, parser.nextToken());
        }
    }

    /**
     * Test that JSON-escaped surrogate pair in string value still works
     * (this was already working before, but let's verify it's not broken).
     */
    @Test
    void escapedSurrogatePairInStringValue() throws Exception
    {
        // JSON: {"key":"\ud83d\udc4d"}
        byte[] doc = new byte[] {
            '{', '"', 'k', 'e', 'y', '"', ':', '"',
            '\\', 'u', 'd', '8', '3', 'd',
            '\\', 'u', 'd', 'c', '4', 'd',
            '"',
            '}'
        };

        try (JsonParser p = FACTORY.createParser(new ByteArrayInputStream(doc))) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            assertEquals("key", p.currentName());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals("\uD83D\uDC4D", p.getText());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }
    }
}
