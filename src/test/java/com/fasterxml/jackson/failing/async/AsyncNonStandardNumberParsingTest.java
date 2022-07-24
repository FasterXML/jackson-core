package com.fasterxml.jackson.failing.async;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;

import java.io.IOException;

public class AsyncNonStandardNumberParsingTest extends AsyncTestBase
{
    public void testLeadingPlusSignNoLeadingZeroEnabled() throws Exception {
        final String JSON = "[ +.123 ]";

        JsonFactory jsonFactory = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS)
                .enable(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS)
                .build();
        AsyncReaderWrapper p = createParser(jsonFactory, JSON, 1);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            assertEquals(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertEquals(0.123, p.getDoubleValue());
            assertEquals("0.123", p.getDecimalValue().toString());
            assertEquals("+.123", p.currentText());
        } finally {
            p.close();
        }
    }

    public void testLeadingDotInNegativeDecimalEnabled() throws Exception {
        final String JSON = "[ -.123 ]";
        final JsonFactory factory = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS)
                .build();

        AsyncReaderWrapper p = createParser(factory, JSON, 1);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            assertEquals(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertEquals(-0.123, p.getDoubleValue());
            assertEquals("-0.123", p.getDecimalValue().toString());
            assertEquals("-.123", p.currentText());
        } finally {
            p.close();
        }
    }

    private AsyncReaderWrapper createParser(JsonFactory f, String doc, int readBytes) throws IOException
    {
        return asyncForBytes(f, readBytes, _jsonDoc(doc), 1);
    }
}
