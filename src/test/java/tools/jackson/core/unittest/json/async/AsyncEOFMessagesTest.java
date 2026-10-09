package tools.jackson.core.unittest.json.async;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.json.async.NonBlockingByteArrayJsonParser;
import tools.jackson.core.unittest.async.AsyncTestBase;
import tools.jackson.core.unittest.testutil.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

// 09-Oct-2026, tatu: end-of-input within token should give specific error message
//   (not "internal state"); and BOM-only content is same as empty content
class AsyncEOFMessagesTest extends AsyncTestBase
{
    private final JsonFactory F = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .enable(JsonReadFeature.ALLOW_UNQUOTED_PROPERTY_NAMES)
            .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
            .enable(JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS)
            .build();

    private final static byte[] BOM = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };

    @Test
    void eofInFieldName() throws Exception
    {
        _testEOF(utf8Bytes("{\"ab"), "Unexpected end-of-input in property name");
        _testEOF(utf8Bytes("{'ab"), "Unexpected end-of-input in property name");
        _testEOF(utf8Bytes("{ab"), "Unexpected end-of-input in property name");
    }

    @Test
    void eofBetweenTokens() throws Exception
    {
        for (String doc : new String[] {
                "{ ", "{\"a\":1,", "{\"a\":1, ", "{\"a\"", "{\"a\" ", "{\"a\":1 "
        }) {
            _testEOF(utf8Bytes(doc), "expected close marker for Object");
        }
        for (String doc : new String[] { "[ ", "[1 ", "[1,", "[1, " }) {
            _testEOF(utf8Bytes(doc), "expected close marker for Array");
        }
    }

    @Test
    void eofInNumberSign() throws Exception
    {
        _testEOF(utf8Bytes("+"), "Unexpected end-of-input in a Number value");
        _testEOF(utf8Bytes("[-"), "Unexpected end-of-input in a Number value");
    }

    // Number with no digits after decimal point or exponent sign is invalid
    @Test
    void eofInNumberWithoutDigits() throws Exception
    {
        _testEOF(utf8Bytes("1."), "Decimal point not followed by a digit");
        _testEOF(utf8Bytes("-1."), "Decimal point not followed by a digit");
        _testEOF(utf8Bytes("[1."), "Decimal point not followed by a digit");
        _testEOF(utf8Bytes("1e+"), "was expecting digits after exponent marker");
        _testEOF(utf8Bytes("1.5e-"), "was expecting digits after exponent marker");
    }

    @Test
    void eofAfterTrailingDecimalPointAllowed() throws Exception
    {
        final JsonFactory f = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_TRAILING_DECIMAL_POINT_FOR_NUMBERS)
                .build();
        final byte[] doc = utf8Bytes("1.");
        for (int readSize = 1; readSize <= doc.length; ++readSize) {
            try (AsyncReaderWrapper p = asyncForBytes(f, readSize, doc, 0)) {
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals(1.0, p.getDoubleValue());
                assertNull(p.nextToken());
            }
        }
    }

    @Test
    void eofInCommentStart() throws Exception
    {
        _testEOF(utf8Bytes("/"), "Unexpected end-of-input in a comment");
        _testEOF(utf8Bytes("[/"), "Unexpected end-of-input in a comment");
    }

    @Test
    void eofInEscape() throws Exception
    {
        _testEOF(utf8Bytes("{\"a\\u00"), "Unexpected end-of-input in character escape sequence");
        _testEOF(utf8Bytes("\"a\\u00"), "Unexpected end-of-input in character escape sequence");
        _testEOF(utf8Bytes("[\"a\\"), "Unexpected end-of-input in character escape sequence");
    }

    @Test
    void eofInStringValue() throws Exception
    {
        _testEOF(utf8Bytes("\"ab"), "Unexpected end-of-input in a String value");
        _testEOF(utf8Bytes("['ab"), "Unexpected end-of-input in a String value");
        // and with partial multi-byte UTF-8 characters
        _testEOF(new byte[] { '"', 'a', (byte) 0xC3 }, "Unexpected end-of-input in a String value");
        _testEOF(new byte[] { '"', 'a', (byte) 0xE2, (byte) 0x82 },
                "Unexpected end-of-input in a String value");
        _testEOF(new byte[] { '"', 'a', (byte) 0xF0, (byte) 0x9F, (byte) 0x98 },
                "Unexpected end-of-input in a String value");
    }

    @Test
    void eofInBOM() throws Exception
    {
        _testEOF(new byte[] { (byte) 0xEF }, "Unexpected end-of-input in UTF-8 BOM");
        _testEOF(new byte[] { (byte) 0xEF, (byte) 0xBB }, "Unexpected end-of-input in UTF-8 BOM");
    }

    @Test
    void bomOnlyContent() throws Exception
    {
        for (int readSize = 1; readSize <= BOM.length; ++readSize) {
            for (boolean byteBuffer : new boolean[] { false, true }) {
                try (AsyncReaderWrapper p = _async(byteBuffer, readSize, BOM)) {
                    assertNull(p.nextToken());
                }
            }
        }
        // and with all content (and end-of-input) available right away
        try (NonBlockingByteArrayJsonParser p = (NonBlockingByteArrayJsonParser)
                F.createNonBlockingByteArrayParser(ObjectReadContext.empty())) {
            p.feedInput(BOM, 0, BOM.length);
            p.endOfInput();
            assertNull(p.nextToken());
        }
    }

    private void _testEOF(byte[] doc, String expMsg) throws Exception
    {
        for (int readSize = 1; readSize <= doc.length; ++readSize) {
            for (boolean byteBuffer : new boolean[] { false, true }) {
                try (AsyncReaderWrapper p = _async(byteBuffer, readSize, doc)) {
                    while (p.nextToken() != null) { }
                    fail("Should not pass for readSize "+readSize+", byteBuffer="+byteBuffer);
                } catch (StreamReadException e) {
                    verifyException(e, expMsg);
                }
            }
        }
    }

    private AsyncReaderWrapper _async(boolean byteBuffer, int readSize, byte[] doc)
        throws Exception
    {
        return byteBuffer ? asyncForByteBuffer(F, readSize, doc, 0)
                : asyncForBytes(F, readSize, doc, 0);
    }
}
