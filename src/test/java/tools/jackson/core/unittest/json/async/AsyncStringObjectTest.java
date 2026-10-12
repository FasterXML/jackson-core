package tools.jackson.core.unittest.json.async;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonToken;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.unittest.async.AsyncTestBase;
import tools.jackson.core.unittest.testutil.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.*;

class AsyncStringObjectTest extends AsyncTestBase
{
    private final static String STR0_9 = "0123456789";
    private final static String ASCII_SHORT_NAME = "a"+STR0_9+"z";
    private final static String UNICODE_SHORT_NAME = "Unicode"+UNICODE_3BYTES+"RlzOk";
    private final static String UNICODE_LONG_NAME = String.format(
            "Unicode-"+UNICODE_3BYTES+"-%s-%s-%s-"+UNICODE_2BYTES+"-%s-%s-%s-"+UNICODE_3BYTES+"-%s-%s-%s",
            STR0_9, STR0_9, STR0_9, STR0_9, STR0_9, STR0_9, STR0_9, STR0_9, STR0_9);

    private final JsonFactory JSON_F = new JsonFactory();

    @Test
    void basicFieldsNames() throws IOException
    {
        final String json = String.format("{\"%s\":\"%s\",\"%s\":\"%s\",\"%s\":\"%s\"}",
            UNICODE_SHORT_NAME, UNICODE_LONG_NAME,
            UNICODE_LONG_NAME, UNICODE_SHORT_NAME,
            ASCII_SHORT_NAME, ASCII_SHORT_NAME);

        final JsonFactory f = JSON_F;

        byte[] data = _jsonDoc(json);
        _testBasicFieldsNames(f, data, 0, 100);
        _testBasicFieldsNames(f, data, 0, 3);
        _testBasicFieldsNames(f, data, 0, 1);

        _testBasicFieldsNames(f, data, 1, 100);
        _testBasicFieldsNames(f, data, 1, 3);
        _testBasicFieldsNames(f, data, 1, 1);
    }

    private void _testBasicFieldsNames(JsonFactory f,
            byte[] data, int offset, int readSize) throws IOException
    {
        _testBasicFieldsNames2(f, data, offset, readSize, true);
        _testBasicFieldsNames2(f, data, offset, readSize, false);
    }

    private void _testBasicFieldsNames2(JsonFactory f,
            byte[] data, int offset, int readSize, boolean verifyContents) throws IOException
    {
        try (AsyncReaderWrapper r = asyncForBytes(f, readSize, data, offset)) {

            // start with "no token"
            assertNull(r.currentToken());
            assertToken(JsonToken.START_OBJECT, r.nextToken());

            assertToken(JsonToken.PROPERTY_NAME, r.nextToken());
            if (verifyContents) {
                assertEquals(UNICODE_SHORT_NAME, r.currentName());
                assertEquals(UNICODE_SHORT_NAME, r.currentText());
            }
            assertToken(JsonToken.VALUE_STRING, r.nextToken());
            // also, should always be accessible this way:
            if (verifyContents) {
                assertTrue(r.parser().hasStringCharacters());
                assertEquals(UNICODE_LONG_NAME, r.currentText());
            }

            assertToken(JsonToken.PROPERTY_NAME, r.nextToken());
            if (verifyContents) {
                assertEquals(UNICODE_LONG_NAME, r.currentName());
                assertEquals(UNICODE_LONG_NAME, r.currentText());
            }
            assertToken(JsonToken.VALUE_STRING, r.nextToken());
            if (verifyContents) {
                assertEquals(UNICODE_SHORT_NAME, r.currentText());
            }

            // and ASCII entry
            assertToken(JsonToken.PROPERTY_NAME, r.nextToken());
            if (verifyContents) {
                assertEquals(ASCII_SHORT_NAME, r.currentName());
                assertEquals(ASCII_SHORT_NAME, r.currentText());
            }
            assertToken(JsonToken.VALUE_STRING, r.nextToken());
            if (verifyContents) {
                assertEquals(ASCII_SHORT_NAME, r.currentText());
            }

            assertToken(JsonToken.END_OBJECT, r.nextToken());
            assertNull(r.nextToken());
        }
            // Second round, try with alternate read method
        if (verifyContents) {
            try (AsyncReaderWrapper r = asyncForBytes(f, readSize, data, offset)) {
                assertToken(JsonToken.START_OBJECT, r.nextToken());
                assertToken(JsonToken.PROPERTY_NAME, r.nextToken());
                assertEquals(UNICODE_SHORT_NAME, r.currentTextViaWriter());
                assertToken(JsonToken.VALUE_STRING, r.nextToken());
                assertEquals(UNICODE_LONG_NAME, r.currentTextViaWriter());

                assertToken(JsonToken.PROPERTY_NAME, r.nextToken());
                assertEquals(UNICODE_LONG_NAME, r.currentTextViaWriter());
                assertToken(JsonToken.VALUE_STRING, r.nextToken());
                assertEquals(UNICODE_SHORT_NAME, r.currentTextViaWriter());

                assertToken(JsonToken.PROPERTY_NAME, r.nextToken());
                assertEquals(ASCII_SHORT_NAME, r.currentTextViaWriter());
                assertToken(JsonToken.VALUE_STRING, r.nextToken());
                assertEquals(ASCII_SHORT_NAME, r.currentTextViaWriter());

                assertToken(JsonToken.END_OBJECT, r.nextToken());
            }
        }
    }

    // [core#1288]: readString(Writer) should not be supported for non-blocking parsers
    @Test
    void readStringNotSupported() throws IOException
    {
        final String json = """
                {"name":"value"}""";
        byte[] data = _jsonDoc(json);

        _testReadStringNotSupported(data, 0, 100);
        _testReadStringNotSupported(data, 0, 3);
        _testReadStringNotSupported(data, 0, 1);
    }

    private void _testReadStringNotSupported(byte[] data, int offset, int readSize)
        throws IOException
    {
        try (AsyncReaderWrapper r = asyncForBytes(JSON_F, readSize, data, offset)) {
            assertToken(JsonToken.START_OBJECT, r.nextToken());
            assertToken(JsonToken.PROPERTY_NAME, r.nextToken());
            assertEquals("name", r.currentName());
            assertToken(JsonToken.VALUE_STRING, r.nextToken());
            assertEquals("value", r.currentText());

            // readString(Writer) should throw UnsupportedOperationException
            Writer writer = new StringWriter();
            UnsupportedOperationException ex = assertThrows(
                UnsupportedOperationException.class,
                () -> r.parser().readString(writer),
                "Should throw UnsupportedOperationException for readString() on non-blocking parser"
            );

            assertTrue(ex.getMessage().contains("not supported for non-blocking"),
                "Exception message should mention non-blocking parsers");
            assertTrue(ex.getMessage().contains("readString"),
                "Exception message should mention readString method");

            assertToken(JsonToken.END_OBJECT, r.nextToken());
        }
    }

    @Test
    void readStringNotSupportedForVariousTokens() throws IOException
    {
        // Test with different token types to ensure consistent behavior
        final String json = """
                {"prop":"text","num":42,"flag":true,"nul":null}""";
        byte[] data = _jsonDoc(json);

        try (AsyncReaderWrapper r = asyncForBytes(JSON_F, 100, data, 0)) {
            assertToken(JsonToken.START_OBJECT, r.nextToken());

            // Property name
            assertToken(JsonToken.PROPERTY_NAME, r.nextToken());
            Writer writer = new StringWriter();
            assertThrows(UnsupportedOperationException.class,
                () -> r.parser().readString(writer));

            // String value
            assertToken(JsonToken.VALUE_STRING, r.nextToken());
            assertThrows(UnsupportedOperationException.class,
                () -> r.parser().readString(writer));

            // Number
            assertToken(JsonToken.PROPERTY_NAME, r.nextToken());
            assertToken(JsonToken.VALUE_NUMBER_INT, r.nextToken());
            assertThrows(UnsupportedOperationException.class,
                () -> r.parser().readString(writer));

            // Boolean
            assertToken(JsonToken.PROPERTY_NAME, r.nextToken());
            assertToken(JsonToken.VALUE_TRUE, r.nextToken());
            assertThrows(UnsupportedOperationException.class,
                () -> r.parser().readString(writer));

            // Null
            assertToken(JsonToken.PROPERTY_NAME, r.nextToken());
            assertToken(JsonToken.VALUE_NULL, r.nextToken());
            assertThrows(UnsupportedOperationException.class,
                () -> r.parser().readString(writer));

            assertToken(JsonToken.END_OBJECT, r.nextToken());
        }
    }

    @Test
    void readStringNotSupportedWithLongString() throws IOException
    {
        // Test with a longer string to ensure it's not a length-related issue
        final String longValue = "x".repeat(1000);
        final String json = """
            {"data":"%s"}""".formatted(longValue);
        byte[] data = _jsonDoc(json);

        try (AsyncReaderWrapper r = asyncForBytes(JSON_F, 100, data, 0)) {
            assertToken(JsonToken.START_OBJECT, r.nextToken());
            assertToken(JsonToken.PROPERTY_NAME, r.nextToken());
            assertToken(JsonToken.VALUE_STRING, r.nextToken());

            // Verify we can still read via getString()
            assertEquals(longValue, r.currentText());

            // But readString(Writer) should throw
            Writer writer = new StringWriter();
            UnsupportedOperationException ex = assertThrows(
                UnsupportedOperationException.class,
                () -> r.parser().readString(writer)
            );

            assertTrue(ex.getMessage().contains("not supported"));
            assertToken(JsonToken.END_OBJECT, r.nextToken());
        }
    }
}
