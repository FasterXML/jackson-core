package tools.jackson.core.unittest.constraints;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.async.ByteArrayFeeder;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.unittest.*;

import static org.junit.jupiter.api.Assertions.fail;

// [core#1047]: Add max-name-length constraints
class LargeNameReadTest extends JacksonCoreTestBase
{
    private final JsonFactory JSON_F_DEFAULT = newStreamFactory();

    private final JsonFactory JSON_F_NAME_100 = JsonFactory.builder()
            .streamReadConstraints(StreamReadConstraints.builder().maxNameLength(100).build())
            .build();

    // Factory that also allows non-standard name flavors ("apostrophe" and unquoted)
    private final JsonFactory JSON_F_NAME_100_ODD = JsonFactory.builder()
            .streamReadConstraints(StreamReadConstraints.builder().maxNameLength(100).build())
            .configure(JsonReadFeature.ALLOW_SINGLE_QUOTES, true)
            .configure(JsonReadFeature.ALLOW_UNQUOTED_PROPERTY_NAMES, true)
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

    // [core#1643]: Reader-backed parser must reject an over-limit name promptly, the
    // same way byte-based input already does -- not only once the entire (possibly
    // huge) name has already been buffered.
    // (note: `String` / `char[]` input is not affected the same way, since the whole
    // document is already in memory and gets scanned in-place, without buffering)
    @Test
    void largeNameWithSmallLimitCharsFailsFast() throws Exception {
        // Name much larger than the configured limit: without incremental checking
        // the whole name gets buffered (bounded only by much bigger `maxStringLength`)
        // before failing, whereas the fix must reject within a segment fill or two.
        _testLargeNameFailsFast(JSON_F_NAME_100, "\"");
    }

    // [core#1643]: ... and same goes for the non-standard name flavors, which are
    // decoded by different code paths ("apostrophe" and unquoted names)
    @Test
    void largeOddNameWithSmallLimitCharsFailsFast() throws Exception {
        _testLargeNameFailsFast(JSON_F_NAME_100_ODD, "'");
        _testLargeNameFailsFast(JSON_F_NAME_100_ODD, "");
    }

    private void _testLargeNameFailsFast(JsonFactory jf, String nameQuote) throws Exception
    {
        final int nameLen = 1_000_000;
        final String doc = generateJSON(nameLen, nameQuote);
        try (JsonParser p = createParserUsingReader(jf, doc)) {
            consumeTokens(p);
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            verifyException(e, "Name length");
            // Length the exception reports tells us how much had been accumulated
            // before the check fired: needs to be small fraction of the whole name
            final int reportedLen = _reportedNameLength(e);
            final int maxExpected = nameLen >> 4;
            if (reportedLen > maxExpected) {
                fail("Should have failed before buffering "+maxExpected
                        +" chars (limit is 100), but reported length was: "+reportedLen);
            }
        }
    }

    private int _reportedNameLength(StreamConstraintsException e) {
        Matcher m = Pattern.compile("Name length \\((\\d+)\\)").matcher(e.getMessage());
        if (!m.find()) {
            fail("Could not find reported name length from message: "+e.getMessage());
        }
        return Integer.parseInt(m.group(1));
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

    // [core#1602]: DataInput-backed parser must also enforce maxNameLength
    @Test
    void largeNameWithSmallLimitDataInput() throws Exception {
        _testLargeNameWithSmallLimitDataInput(JSON_F_NAME_100);
    }

    private void _testLargeNameWithSmallLimitDataInput(JsonFactory jf) throws Exception
    {
        final String doc = generateJSON(1000);
        try (JsonParser p = createParser(jf, MODE_DATA_INPUT, doc)) {
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
        return generateJSON(nameLen, "\"");
    }

    // @param nameQuote Quote character to use around name; empty String for unquoted name
    private String generateJSON(final int nameLen, final String nameQuote) {
        final StringBuilder sb = new StringBuilder();
        sb.append("{").append(nameQuote);
        for (int i = 0; i < nameLen; i++) {
            sb.append("a");
        }
        sb.append(nameQuote).append(":\"value\"}");
        return sb.toString();
    }
}
