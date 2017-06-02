package com.fasterxml.jackson.core.json.async;

import java.io.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;

public class AsyncFieldNamesTest extends AsyncTestBase
{
    private final JsonFactory JSON_F = new JsonFactory();

    public void testEscapedFieldNames() throws IOException
    {
        _testEscapedFieldNames("\\'foo\\'", "'foo'");
        _testEscapedFieldNames("\\'foobar\\'", "'foobar'");
        _testEscapedFieldNames("\\'foo \\u0026 bar\\'", "'foo & bar'");
        _testEscapedFieldNames("Something \\'longer\\'?", "Something 'longer'?");
        _testEscapedFieldNames("\\u00A7", "\u00A7");
        _testEscapedFieldNames("\\u4567", "\u4567");
        _testEscapedFieldNames("Unicode: \\u00A7 and \\u4567?", "Unicode: \u00A7 and \u4567?");
    }

    private void _testEscapedFieldNames(String nameEncoded, String nameExp) throws IOException
    {
        nameEncoded = aposToQuotes(nameEncoded);
        nameExp = aposToQuotes(nameExp);
        StringWriter w = new StringWriter();
        w.append("{\"");
        w.append(nameEncoded);
        w.append("\":true}");
        byte[] doc = w.toString().getBytes("UTF-8");

        _testEscapedFieldNames(doc, nameExp, 0, 99);
        _testEscapedFieldNames(doc, nameExp, 0, 5);
        _testEscapedFieldNames(doc, nameExp, 0, 3);
        _testEscapedFieldNames(doc, nameExp, 0, 2);
        _testEscapedFieldNames(doc, nameExp, 0, 1);

        _testEscapedFieldNames(doc, nameExp, 1, 99);
        _testEscapedFieldNames(doc, nameExp, 1, 3);
        _testEscapedFieldNames(doc, nameExp, 1, 1);
    }

    private void _testEscapedFieldNames(byte[] doc, String expName,
            int offset, int readSize) throws IOException
    {
        AsyncReaderWrapper r = asyncForBytes(JSON_F, readSize, doc, offset);
        assertNull(r.currentToken());
        assertToken(JsonToken.START_OBJECT, r.nextToken());
        assertToken(JsonToken.FIELD_NAME, r.nextToken());
        assertEquals(expName, r.currentName());
        assertToken(JsonToken.VALUE_TRUE, r.nextToken());
        
        r.close();
        assertNull(r.nextToken());
    }
}
