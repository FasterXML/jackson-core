package tools.jackson.core.unittest.read;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.unittest.JacksonCoreTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for [jackson-core#1750]: {@code ALLOW_UNESCAPED_CONTROL_CHARS} must
 * apply to single-quoted string values in all parsers.
 */
class AposUnescapedControlChars1750Test extends JacksonCoreTestBase
{
    private final JsonFactory APOS_F = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .build();

    private final JsonFactory APOS_CTRL_F = APOS_F.rebuild()
            .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
            .build();

    private final static String[] VALUES = new String[] {
            "a\tb", "\t", "a\u0001b", "x\ny\rz", "é\t中", "\t\uD83D\uDE00\t",
            _longValue()
    };

    @Test
    void controlCharsInAposValueAllowed() throws Exception
    {
        for (int mode : ALL_MODES) {
            for (String value : VALUES) {
                try (JsonParser p = createParser(APOS_CTRL_F, mode, "['" + value + "']")) {
                    assertToken(JsonToken.START_ARRAY, p.nextToken());
                    assertToken(JsonToken.VALUE_STRING, p.nextToken());
                    assertEquals(value, p.getString(), "mode " + mode);
                    assertToken(JsonToken.END_ARRAY, p.nextToken());
                }
                try (JsonParser p = createParser(APOS_CTRL_F, mode, "{'a':'" + value + "'}")) {
                    assertToken(JsonToken.START_OBJECT, p.nextToken());
                    assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
                    assertToken(JsonToken.VALUE_STRING, p.nextToken());
                    assertEquals(value, p.getString(), "mode " + mode);
                    assertToken(JsonToken.END_OBJECT, p.nextToken());
                }
                // and skipping (no access to text)
                try (JsonParser p = createParser(APOS_CTRL_F, mode, "['" + value + "',true]")) {
                    assertToken(JsonToken.START_ARRAY, p.nextToken());
                    assertToken(JsonToken.VALUE_STRING, p.nextToken());
                    assertToken(JsonToken.VALUE_TRUE, p.nextToken());
                    assertToken(JsonToken.END_ARRAY, p.nextToken());
                }
            }
        }
    }

    @Test
    void controlCharsInAposNameAllowed() throws Exception
    {
        for (int mode : ALL_MODES) {
            for (String name : new String[] { "a\tb", "\t", "x\ny" }) {
                try (JsonParser p = createParser(APOS_CTRL_F, mode, "{'" + name + "':1}")) {
                    assertToken(JsonToken.START_OBJECT, p.nextToken());
                    assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
                    assertEquals(name, p.currentName(), "mode " + mode);
                    assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                    assertToken(JsonToken.END_OBJECT, p.nextToken());
                }
            }
        }
    }

    @Test
    void controlCharsInAposDisallowed() throws Exception
    {
        for (int mode : ALL_MODES) {
            for (String doc : new String[] { "['a\tb']", "{'a':'a\tb'}", "['" + _longValue() + "']" }) {
                try (JsonParser p = createParser(APOS_F, mode, doc)) {
                    p.nextToken();
                    p.nextToken();
                    p.nextToken();
                    p.getString();
                    fail("Should not pass, mode " + mode);
                } catch (StreamReadException e) {
                    verifyException(e, "Illegal unquoted character");
                    verifyException(e, "string value");
                }
            }
            try (JsonParser p = createParser(APOS_F, mode, "{'a\tb':1}")) {
                p.nextToken();
                p.nextToken();
                fail("Should not pass, mode " + mode);
            } catch (StreamReadException e) {
                verifyException(e, "Illegal unquoted character");
                verifyException(e, "name");
            }
        }
    }

    // Linefeeds in String values must update row number
    @Test
    void linefeedsUpdateRow() throws Exception
    {
        for (int mode : ALL_MODES) {
            for (String lf : new String[] { "\n", "\r", "\r\n" }) {
                // DataInput counts both '\r' and '\n' (no lookahead), same as with white space
                final int expRow = (mode == MODE_DATA_INPUT && lf.length() == 2) ? 3 : 2;
                for (char q : new char[] { '\'', '"' }) {
                    final String doc = "[" + q + "x" + lf + "y" + q + ", 1]";
                    // with and without accessing String value (decode vs skip)
                    for (boolean getText : new boolean[] { true, false }) {
                        try (JsonParser p = createParser(APOS_CTRL_F, mode, doc)) {
                            assertToken(JsonToken.START_ARRAY, p.nextToken());
                            assertToken(JsonToken.VALUE_STRING, p.nextToken());
                            if (getText) {
                                assertEquals("x" + lf + "y", p.getString());
                            }
                            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                            assertEquals(expRow, p.currentTokenLocation().getLineNr(),
                                    "mode " + mode + ", doc " + doc.replace("\r", "\\r").replace("\n", "\\n"));
                        }
                    }
                }
            }
        }
    }

    // Allowed control chars must be retained when streaming String value to Writer
    @Test
    void controlCharsStreamedToWriter() throws Exception
    {
        for (int mode : ALL_MODES) {
            for (String value : VALUES) {
                for (char q : new char[] { '\'', '"' }) {
                    try (JsonParser p = createParser(APOS_CTRL_F, mode, "[" + q + value + q + ",1]")) {
                        assertToken(JsonToken.START_ARRAY, p.nextToken());
                        assertToken(JsonToken.VALUE_STRING, p.nextToken());
                        java.io.StringWriter w = new java.io.StringWriter();
                        assertEquals(value.length(), p.readString(w), "mode " + mode);
                        assertEquals(value, w.toString(), "mode " + mode);
                        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                    }
                }
            }
        }
    }

    // Long enough to span input buffer and text buffer segment boundaries,
    // with control chars at varying offsets
    private static String _longValue() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6000; ++i) {
            switch (i % 7) {
            case 0:
                sb.append('\t');
                break;
            case 3:
                sb.append('é');
                break;
            default:
                sb.append((char) ('a' + (i % 26)));
            }
        }
        return sb.toString();
    }
}
