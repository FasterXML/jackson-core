package com.fasterxml.jackson.core.read;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.json.async.NonBlockingJsonParser;

// for [core#633]: optionally allow Record-Separator ctrl char
class NonStandardAllowRSTest
    extends JUnit5TestBase
{
    @Test
    void recordSeparatorEnabled() throws Exception {
        doRecordSeparationTest(true);
    }

    @Test
    void recordSeparatorDisabled() throws Exception {
        doRecordSeparationTest(false);
    }

    // Testing record separation for all parser implementations
    private void doRecordSeparationTest(boolean recordSeparation) throws Exception {
        String contents = "{\"key\":true}\u001E";
        JsonFactory factory = JsonFactory.builder()
                .configure(JsonReadFeature.ALLOW_RS_CONTROL_CHAR, recordSeparation)
                .build();
        try (JsonParser parser = factory.createParser(contents)) {
            verifyRecordSeparation(parser, recordSeparation);
        }
        try (JsonParser parser = factory.createParser(new StringReader(contents))) {
            verifyRecordSeparation(parser, recordSeparation);
        }
        try (JsonParser parser = factory.createParser(contents.getBytes(StandardCharsets.UTF_8))) {
            verifyRecordSeparation(parser, recordSeparation);
        }
        try (NonBlockingJsonParser parser = (NonBlockingJsonParser) factory.createNonBlockingByteArrayParser()) {
            byte[] data = contents.getBytes(StandardCharsets.UTF_8);
            parser.feedInput(data, 0, data.length);
            parser.endOfInput();
            verifyRecordSeparation(parser, recordSeparation);
        }
    }

    private void verifyRecordSeparation(JsonParser parser, boolean recordSeparation) throws Exception {
        try {
            assertToken(JsonToken.START_OBJECT, parser.nextToken());
            String field1 = parser.nextFieldName();
            assertEquals("key", field1);
            assertToken(JsonToken.VALUE_TRUE, parser.nextToken());
            assertToken(JsonToken.END_OBJECT, parser.nextToken());
            parser.nextToken(); // RS token
            if (!recordSeparation) {
                fail("Should have thrown an exception");
            }
        } catch (StreamReadException e) {
            if (!recordSeparation) {
                verifyException(e, "Illegal character ((CTRL-CHAR");
                verifyException(e, "consider enabling `JsonReadFeature.ALLOW_RS_CONTROL_CHAR`");
            } else {
                throw e;
            }
        }
    }    
}
