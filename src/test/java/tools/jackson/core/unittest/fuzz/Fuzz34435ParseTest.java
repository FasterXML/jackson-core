package tools.jackson.core.unittest.fuzz;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.unittest.*;

import static org.junit.jupiter.api.Assertions.fail;

// Trying to repro: https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=34435
class Fuzz34435ParseTest extends JacksonCoreTestBase
{
    private final byte[] DOC = readResource("/data/fuzz-json-34435.json");

    @Test
    void fuzz34435ViaParser() throws Exception
    {
        final JsonFactory f = JsonFactory.builder()
                // NOTE: test set up enables a few non-standard features
                .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
                .enable(JsonReadFeature.ALLOW_YAML_COMMENTS)
                .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
                .enable(JsonReadFeature.ALLOW_UNQUOTED_PROPERTY_NAMES)
                .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(Integer.MAX_VALUE).build())
                .build();

        JsonParser p = f.createParser(ObjectReadContext.empty(), DOC);

        // Long doc so only check a few initial entries first:
        for (int i = 0; i < 10; ++i) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        }

        // Beyond that, simply read through to catch expected issue
        try {
            while (p.nextToken() != null) {
                // but force decoding of Strings
                switch (p.currentToken()) {
                case PROPERTY_NAME:
                    p.currentName();
                    break;
                case VALUE_STRING:
                    p.getString();
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
