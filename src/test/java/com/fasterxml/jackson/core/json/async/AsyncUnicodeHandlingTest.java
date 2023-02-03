package com.fasterxml.jackson.core.json.async;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;

public class AsyncUnicodeHandlingTest extends AsyncTestBase
{
    private final JsonFactory JSON_F = new JsonFactory();

    public void testShortUnicodeWithSurrogates() throws IOException
    {
        JsonFactory f = JSON_F;

        // first, no buffer boundaries
        _testUnicodeWithSurrogates(f, 28, 99);
        _testUnicodeWithSurrogates(f, 53, 99);

        // then small chunks
        _testUnicodeWithSurrogates(f, 28, 3);
        _testUnicodeWithSurrogates(f, 53, 5);

        // and finally one-by-one
        _testUnicodeWithSurrogates(f, 28, 1);
        _testUnicodeWithSurrogates(f, 53, 1);
    }

    public void testLongUnicodeWithSurrogates() throws IOException
    {
        JsonFactory f = JSON_F;

        _testUnicodeWithSurrogates(f, 230, Integer.MAX_VALUE);
        _testUnicodeWithSurrogates(f, 700, Integer.MAX_VALUE);
        _testUnicodeWithSurrogates(f, 9600, Integer.MAX_VALUE);

        _testUnicodeWithSurrogates(f, 230, 3);
        _testUnicodeWithSurrogates(f, 700, 3);
        _testUnicodeWithSurrogates(f, 9600, 3);

        _testUnicodeWithSurrogates(f, 230, 1);
        _testUnicodeWithSurrogates(f, 700, 1);
        _testUnicodeWithSurrogates(f, 9600, 1);
    }

    private void _testUnicodeWithSurrogates(JsonFactory f,
            int length, int readSize) throws IOException
    {
        final String SURROGATE_CHARS = "\ud834\udd1e";
        StringBuilder sb = new StringBuilder(length+200);
        while (sb.length() < length) {
            sb.append(SURROGATE_CHARS);
            sb.append(sb.length());
            if ((sb.length() & 1) == 1) {
                sb.append("\u00A3");
            } else {
                sb.append("\u3800");
            }
        }
        final String TEXT = sb.toString();
        final String quoted = q(TEXT);
        byte[] data = _jsonDoc(quoted);
        AsyncReaderWrapper r = asyncForBytes(f, readSize, data, 0);

        assertToken(JsonToken.VALUE_STRING, r.nextToken());
        assertEquals(TEXT, r.currentText());
        assertNull(r.nextToken());
        r.close();

        // Then same but skipping
        r = asyncForBytes(f, readSize, data, 0);
        assertToken(JsonToken.VALUE_STRING, r.nextToken());
        assertNull(r.nextToken());
        r.close();

        // Also, verify that it works as field name
        data = _jsonDoc("{"+quoted+":true}");
        r = asyncForBytes(f, readSize, data, 0);
        assertToken(JsonToken.START_OBJECT, r.nextToken());
        assertToken(JsonToken.FIELD_NAME, r.nextToken());
        assertEquals(TEXT, r.currentName());
        assertToken(JsonToken.VALUE_TRUE, r.nextToken());
        assertToken(JsonToken.END_OBJECT, r.nextToken());
        assertNull(r.nextToken());
        r.close();

        // and skipping
        r = asyncForBytes(f, readSize, data, 0);
        assertToken(JsonToken.START_OBJECT, r.nextToken());
        assertToken(JsonToken.FIELD_NAME, r.nextToken());
        assertToken(JsonToken.VALUE_TRUE, r.nextToken());
        assertToken(JsonToken.END_OBJECT, r.nextToken());
        r.close();
    }
}
