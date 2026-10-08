package com.fasterxml.jackson.core.read;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.json.JsonReadFeature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for [jackson-core#1750]: {@code ALLOW_UNESCAPED_CONTROL_CHARS} must
 * apply to single-quoted string values in all parsers.
 */
class AposUnescapedControlChars1750Test extends JUnit5TestBase
{
    private final JsonFactory APOS_F = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .build();

    private final JsonFactory APOS_CTRL_F = APOS_F.rebuild()
            .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
            .build();

    @Test
    void controlCharsInAposValueAllowed() throws Exception
    {
        for (int mode : ALL_MODES) {
            for (String value : new String[] { "a\tb", "\t", "a\u0001b", "x\ny\rz", "é\t中" }) {
                try (JsonParser p = createParser(APOS_CTRL_F, mode, "['" + value + "']")) {
                    assertToken(JsonToken.START_ARRAY, p.nextToken());
                    assertToken(JsonToken.VALUE_STRING, p.nextToken());
                    assertEquals(value, p.getText(), "mode " + mode);
                    assertToken(JsonToken.END_ARRAY, p.nextToken());
                }
                try (JsonParser p = createParser(APOS_CTRL_F, mode, "{'a':'" + value + "'}")) {
                    assertToken(JsonToken.START_OBJECT, p.nextToken());
                    assertToken(JsonToken.FIELD_NAME, p.nextToken());
                    assertToken(JsonToken.VALUE_STRING, p.nextToken());
                    assertEquals(value, p.getText(), "mode " + mode);
                    assertToken(JsonToken.END_OBJECT, p.nextToken());
                }
            }
        }
    }

    @Test
    void controlCharsInAposValueDisallowed() throws Exception
    {
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(APOS_F, mode, "['a\tb']")) {
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                p.nextToken();
                p.getText();
                fail("Should not pass, mode " + mode);
            } catch (StreamReadException e) {
                verifyException(e, "Illegal unquoted character");
            }
        }
    }
}
