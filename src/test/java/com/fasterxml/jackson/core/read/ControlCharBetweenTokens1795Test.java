package com.fasterxml.jackson.core.read;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.json.JsonReadFeature;

// [core#1795]: control characters between tokens must be rejected by all parsers,
// including the `DataInput`-based one
class ControlCharBetweenTokens1795Test
    extends JUnit5TestBase
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
                    _consume(p);
                    fail("Should not pass for mode " + mode + ", doc: " + doc);
                } catch (StreamReadException e) {
                    verifyException(e, "CTRL-CHAR, code 1");
                }
            }
        }
    }

    @Test
    void ctrlCharWithNextFieldName() throws Exception {
        for (String doc : new String[] { "\u0001{\"a\":1}", "{\"a\":1,\u0001\"b\":2}" }) {
            for (int mode : ALL_MODES) {
                try (JsonParser p = createParser(JSON_F, mode, doc)) {
                    _consumeWithNextFieldName(p);
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
                _consume(p);
            }
        }
    }

    // Like the other parsers, only allow Record Separator between root values
    @Test
    void recordSeparatorAllowedBetweenRootValues() throws Exception {
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(RS_F, mode, "[1]\u001E[2]\u001E")) {
                _consume(p);
            }
        }
    }

    @Test
    void recordSeparatorAllowedWithNextFieldName() throws Exception {
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(RS_F, mode, "{\"a\":1}\u001E{\"b\":2}\u001E")) {
                assertEquals(2, _consumeWithNextFieldName(p));
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
    void recordSeparatorRejectedInsideValue() throws Exception {
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(RS_F, mode, "[1,\u001E2]")) {
                _consume(p);
                fail("Should not pass for mode " + mode);
            } catch (StreamReadException e) {
                verifyException(e, "Illegal character ((CTRL-CHAR, code 30))");
            }
        }
    }

    private void _verifyRejected(String doc) throws Exception {
        for (int mode : ALL_MODES) {
            try (JsonParser p = createParser(JSON_F, mode, doc)) {
                _consume(p);
                fail("Should not pass for mode " + mode + ", doc: " + doc);
            } catch (StreamReadException e) {
                verifyException(e, "Illegal character ((CTRL-CHAR, code 1))");
            }
        }
    }

    private int _consumeWithNextFieldName(JsonParser p) throws Exception {
        int names = 0;
        while (true) {
            if (p.nextFieldName() != null) {
                ++names;
            } else if (p.currentToken() == null) {
                return names;
            }
        }
    }

    private void _consume(JsonParser p) throws Exception {
        int tokens = 0;
        while (p.nextToken() != null) {
            ++tokens;
        }
        assertEquals(true, tokens > 0);
    }
}
