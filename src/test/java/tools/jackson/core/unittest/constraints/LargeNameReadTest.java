package tools.jackson.core.unittest.constraints;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.async.ByteArrayFeeder;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.unittest.*;

import static org.junit.jupiter.api.Assertions.fail;

// [core#1047]: Add max-name-length constraints
class LargeNameReadTest extends JacksonCoreTestBase
{
    private final JsonFactory JSON_F_DEFAULT = newStreamFactory();

    private final JsonFactory JSON_F_NAME_100 = JsonFactory.builder()
            .streamReadConstraints(StreamReadConstraints.builder().maxNameLength(100).build())
            .build();

    // Test name that is below default max name
    @Test
    void largeNameBytes() throws Exception {
        final String doc = generateJSON(StreamReadConstraints.defaults().getMaxNameLength() - 100);
        try (JsonParser p = createParserUsingStream(JSON_F_DEFAULT, doc, "UTF-8")) {
            consumeTokens(p);
        }
    }

    @Test
    void largeNameChars() throws Exception {
        final String doc = generateJSON(StreamReadConstraints.defaults().getMaxNameLength() - 100);
        try (JsonParser p = createParserUsingReader(JSON_F_DEFAULT, doc)) {
            consumeTokens(p);
        }
    }

    @Test
    void largeNameWithSmallLimitBytes() throws Exception {
        _testLargeNameWithSmallLimitBytes(JSON_F_NAME_100);
    }

    private void _testLargeNameWithSmallLimitBytes(JsonFactory jf) throws Exception
    {
        final String doc = generateJSON(1000);
        try (JsonParser p = createParserUsingStream(jf, doc, "UTF-8")) {
            consumeTokens(p);
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            verifyException(e, "Name length");
        }
    }

    @Test
    void largeNameWithSmallLimitChars() throws Exception {
        _testLargeNameWithSmallLimitChars(JSON_F_NAME_100);
    }

    private void _testLargeNameWithSmallLimitChars(JsonFactory jf) throws Exception
    {
        final String doc = generateJSON(1000);
        try (JsonParser p = createParserUsingReader(jf, doc)) {
            consumeTokens(p);
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            verifyException(e, "Name length");
        }
    }

    @Test
    void largeNameWithSmallLimitAsync() throws Exception
    {
        final byte[] doc = utf8Bytes(generateJSON(1000));
        try (JsonParser p = JSON_F_NAME_100.createNonBlockingByteArrayParser(ObjectReadContext.empty())) {
            ((ByteArrayFeeder) p).feedInput(doc, 0, doc.length);
            consumeTokens(p);
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            verifyException(e, "Name length");
        }
    }

    private void consumeTokens(JsonParser p) throws IOException {
        while (p.nextToken() != null) {
            ;
        }
    }

    private String generateJSON(final int nameLen) {
        final StringBuilder sb = new StringBuilder();
        sb.append("{\"");
        for (int i = 0; i < nameLen; i++) {
            sb.append("a");
        }
        sb.append("\":\"value\"}");
        return sb.toString();
    }
}
