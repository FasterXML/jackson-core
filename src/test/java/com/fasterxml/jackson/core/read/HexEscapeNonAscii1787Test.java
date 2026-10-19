package com.fasterxml.jackson.core.read;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamReadException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for [jackson-core#1787]: non-ASCII chars must not be accepted as hex
 * digits of a {@code \}{@code u} escape (they used to alias to ASCII ones with
 * the same low byte, e.g. U+2042 to 'B', with char-based input).
 */
class HexEscapeNonAscii1787Test extends JUnit5TestBase
{
    private final JsonFactory FACTORY = newStreamFactory();

    @Test
    void nonAsciiHexDigitsRejected() throws Exception
    {
        // U+2042 -> 'B', U+0141 -> 'A', U+0130 -> '0'
        _testBroken("\\u\u2042\u2042\u2042\u2042", "'\u2042' (code 8258 / 0x2042)");
        _testBroken("\\u\u0141BCD", "'\u0141' (code 321 / 0x141)");
        _testBroken("\\u00\u01300", "'\u0130' (code 304 / 0x130)");
        _testBroken("\\u\uD83D\uDE00", null);
    }

    @Test
    void asciiHexDigitsAccepted() throws Exception
    {
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(FACTORY, mode, "{\"\\u00e9\":\"\\uABcd\"}")) {
                assertToken(JsonToken.START_OBJECT, p.nextToken());
                assertToken(JsonToken.FIELD_NAME, p.nextToken());
                assertEquals("\u00e9", p.currentName());
                assertToken(JsonToken.VALUE_STRING, p.nextToken());
                assertEquals("\uABCD", p.getText());
                assertToken(JsonToken.END_OBJECT, p.nextToken());
            }
        }
    }

    private void _testBroken(String escape, String expCharDesc) throws Exception
    {
        for (int mode : ALL_MODES) {
            for (String doc : new String[] { "[\"" + escape + "\"]", "{\"" + escape + "\":1}" }) {
                try (JsonParser p = createParser(FACTORY, mode, doc)) {
                    while (p.nextToken() != null) {
                        p.getText();
                    }
                    fail("Should not pass for mode " + mode + ", doc: " + doc);
                } catch (StreamReadException e) {
                    verifyException(e, "expected a hex-digit for character escape sequence");
                    if (expCharDesc != null) {
                        verifyException(e, "Unexpected character (" + expCharDesc + ")");
                    }
                }
            }
        }
    }
}
