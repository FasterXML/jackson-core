package tools.jackson.core.unittest.read;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.unittest.JacksonCoreTestBase;

// [core#1795]: control characters between tokens must be rejected by all parsers,
// including the `DataInput`-based one
class ControlCharBetweenTokens1795Test
    extends JacksonCoreTestBase
{
    private final JsonFactory JSON_F = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
            .build();

    private final JsonFactory RS_F = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_RS_CONTROL_CHAR)
            .build();

    private final JsonFactory COMMENTS_F = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
            .enable(JsonReadFeature.ALLOW_YAML_COMMENTS)
            .build();

    @Test
    void ctrlCharBetweenArrayValues() throws Exception {
        _verifyRejected("[1,\u0001 2]");
    }

    @Test
    void ctrlCharBeforeRootValue() throws Exception {
        _verifyRejected("\u0001[1]");
    }

    @Test
    void ctrlCharAfterRootValue() throws Exception {
        _verifyRejected("[1]\u0001");
    }

    @Test
    void ctrlCharAfterRootScalar() throws Exception {
        for (String doc : new String[] { "123\u0001", "1\u0001 2", "1.5\u0001", "-1\u0001" }) {
            for (int mode : ALL_MODES) {
                try (JsonParser p = createParser(JSON_F, mode, doc)) {
                    _consume(p, mode);
                    fail("Should not pass for mode " + mode + ", doc: " + doc);
                } catch (StreamReadException e) {
                    verifyException(e, "CTRL-CHAR, code 1");
                }
            }
        }
    }

    @Test
    void ctrlCharWithNextName() throws Exception {
        for (String doc : new String[] { "\u0001{\"a\":1}", "{\"a\":1,\u0001\"b\":2}" }) {
            for (int mode : ALL_MODES) {
                try (JsonParser p = createParser(JSON_F, mode, doc)) {
                    _consumeWithNextName(p);
                    fail("Should not pass for mode " + mode + ", doc: " + doc);
                } catch (StreamReadException e) {
                    verifyException(e, "Illegal character ((CTRL-CHAR, code 1))");
                }
            }
        }
    }

    @Test
    void ctrlCharBeforeColon() throws Exception {
        _verifyRejected("{\"a\"\u0001:1}");
    }

    @Test
    void ctrlCharAfterColon() throws Exception {
        _verifyRejected("{\"a\":\u0001 1}");
    }

    @Test
    void ctrlCharAfterComment() throws Exception {
        _verifyRejected("[1,/* c */\u0001 2]");
        _verifyRejected("{\"a\"/* c */\u0001:1}");
    }

    @Test
    void validWhiteSpaceStillAccepted() throws Exception {
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(JSON_F, mode,
                    " \t\r\n{ \"a\" \t:\r\n 1 , \"b\"\n:\t2}\r\n ")) {
                _consume(p, mode);
            }
        }
    }

    // Like the other parsers, Record Separator is accepted where token skipping starts
    // (between root values, but also e.g. after '[' or before ',' and ']'); not after
    // comma or colon, nor directly after a root-level number or a comment
    @Test
    void recordSeparatorAllowedBetweenRootValues() throws Exception {
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(RS_F, mode, "[1]\u001E[2]\u001E")) {
                _consume(p, mode);
            }
        }
    }

    @Test
    void recordSeparatorAllowedWithNextName() throws Exception {
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(RS_F, mode, "{\"a\":1}\u001E{\"b\":2}\u001E")) {
                assertEquals(2, _consumeWithNextName(p));
            }
        }
    }

    // CR-LF must count as a single linefeed with all parsers
    @Test
    void crlfLineCounting() throws Exception {
        final String doc = "[\r\n1,\r2,\n3,\r\n/* a\r\n b */ 4,\r\n// c\r\n5,\r\n# d\r\n6,"
                + "{\"x\"\r\n:\r\n7}]\r\n8\r\n9 ";
        final int[] expLines = { 1, 2, 3, 4, 6, 8, 10, 10, 10, 12, 12, 12, 13, 14 };
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(COMMENTS_F, mode, doc)) {
                for (int i = 0; i < expLines.length; ++i) {
                    assertNotNull(p.nextToken());
                    assertEquals(expLines[i], p.currentTokenLocation().getLineNr(),
                            "Token #" + i + " (" + p.currentToken() + "), mode " + mode);
                }
                assertNull(p.nextToken());
            }
        }
    }

    @Test
    void recordSeparatorRejectedAfterRootNumberOrComment() throws Exception {
        final JsonFactory f = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_RS_CONTROL_CHAR)
                .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
                .build();
        for (String doc : new String[] { "1\u001E2", "[1]/* c */\u001E[2]" }) {
            for (int mode : ALL_MODES) {
                try (JsonParser p = createParser(f, mode, doc)) {
                    _consume(p, mode);
                    fail("Should not pass for mode " + mode + ", doc: " + doc);
                } catch (StreamReadException e) {
                    verifyException(e, "CTRL-CHAR, code 30");
                }
            }
        }
    }

    // Comment (and white space after it) at the end of content is valid
    @Test
    void commentAtEndOfInput() throws Exception {
        for (String doc : new String[] { "[1] // c", "[1] // c\r\n ", "[1] # c\n", "[1]\r",
                "[1] /* c */", "[1] /* c */ \n", "1 /* c */" }) {
            for (int mode : ALL_MODES) {
                try (JsonParser p = createParser(COMMENTS_F, mode, doc)) {
                    _consume(p, mode);
                }
            }
        }
    }

    @Test
    void eofInsideValueWhiteSpaceOrComment() throws Exception {
        for (String doc : new String[] { "[1] /* c", "[1] /", "[1, // c",
                "[1, // c\n", "[1, /* c */ ", "[1, # c\r", "{\"a\" // c\n", "{\"a\": /* c */\r\n",
                "[1,\r", "[1,", "[1, ", "{\"a\"", "{\"a\" ", "{\"a\":", "{\"a\": ", "{\"a\":\t\n" }) {
            for (int mode : ALL_MODES) {
                try (JsonParser p = createParser(COMMENTS_F, mode, doc)) {
                    _consume(p, mode);
                    fail("Should not pass for mode " + mode + ", doc: " + doc);
                } catch (StreamReadException e) {
                    verifyException(e, "Unexpected end-of-input");
                }
            }
        }
    }

    @Test
    void truncatedUtf8InCommentAtEnd() throws Exception {
        final byte[] doc = new byte[] { '[', '1', ']', ' ', '/', '/', ' ', (byte) 0xC3 };
        for (int mode : ALL_BINARY_MODES) {
            try (JsonParser p = createParser(COMMENTS_F, mode, doc)) {
                _consume(p, mode);
                fail("Should not pass for mode " + mode);
            } catch (StreamReadException e) {
                verifyException(e, "Unexpected end-of-input");
            }
        }
    }

    // Value following property name must report its own start row
    @Test
    void valueLocationAfterName() throws Exception {
        final JsonFactory f = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
                .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
                .build();
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(f, mode, "{\"a\":\n'x\ny',\"b\"\r\n:\r\n[1]}")) {
                assertToken(JsonToken.START_OBJECT, p.nextToken());
                assertEquals("a", p.nextName());
                assertEquals(1, p.currentTokenLocation().getLineNr(), "mode " + mode);
                assertToken(JsonToken.VALUE_STRING, p.nextToken());
                assertEquals(2, p.currentTokenLocation().getLineNr(), "mode " + mode);
                assertEquals("b", p.nextName());
                assertEquals(3, p.currentTokenLocation().getLineNr(), "mode " + mode);
                // via `nextStringValue()` which handles deferred value separately
                assertNull(p.nextStringValue());
                assertToken(JsonToken.START_ARRAY, p.currentToken());
                assertEquals(5, p.currentTokenLocation().getLineNr(), "mode " + mode);
            }
        }
    }

    @Test
    void recordSeparatorRejectedInsideValue() throws Exception {
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(RS_F, mode, "[1,\u001E2]")) {
                _consume(p, mode);
                fail("Should not pass for mode " + mode);
            } catch (StreamReadException e) {
                verifyException(e, "Illegal character ((CTRL-CHAR, code 30))");
            }
        }
    }

    private void _verifyRejected(String doc) throws Exception {
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(JSON_F, mode, doc)) {
                _consume(p, mode);
                fail("Should not pass for mode " + mode + ", doc: " + doc);
            } catch (StreamReadException e) {
                verifyException(e, "Illegal character ((CTRL-CHAR, code 1))");
            }
        }
    }

    private int _consumeWithNextName(JsonParser p) throws Exception {
        int names = 0;
        while (true) {
            if (p.nextName() != null) {
                ++names;
            } else if (p.currentToken() == null) {
                return names;
            }
        }
    }

    private void _consume(JsonParser p, int mode) throws Exception {
        int tokens = 0;
        while (p.nextToken() != null) {
            ++tokens;
        }
        assertTrue(tokens > 0, "No tokens for mode " + mode);
    }
}
