package com.fasterxml.jackson.core.read;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private void _consume(JsonParser p) throws Exception {
        int tokens = 0;
        while (p.nextToken() != null) {
            ++tokens;
        }
        assertEquals(true, tokens > 0);
    }
}
