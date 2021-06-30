package com.fasterxml.jackson.core.fuzz;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.json.JsonReadFeature;

// Trying to repro: https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=34435
public class Fuzz34435ParseTest extends BaseTest
{
    private final byte[] DOC = readResource("/data/fuzz-json-34435.json");

    public void testFuzz34435ViaParser() throws Exception
    {
        final JsonFactory f = JsonFactory.builder()
                // NOTE: test set up enables a few non-standard features
                .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
                .enable(JsonReadFeature.ALLOW_YAML_COMMENTS)
                .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
                .enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
                .build();

        JsonParser p = f.createParser(/*ObjectReadContext.empty(), */ DOC);

        // Long doc so only check a few initial entries first:
        for (int i = 0; i < 10; ++i) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
        }

        // Beyond that, simply read through to catch expected issue
        try {
            while (p.nextToken() != null) {
                // but force decoding of Strings
                switch (p.currentToken()) {
                case FIELD_NAME:
                    p.currentName();
                    break;
                case VALUE_STRING:
                    p.getText();
                    break;
                default:
                }
            }
            fail("Should not pass");
        } catch (StreamReadException e) {
            verifyException(e, "Unexpected character");
            verifyException(e, "colon to separate");
        }
        p.close();
    }
}
