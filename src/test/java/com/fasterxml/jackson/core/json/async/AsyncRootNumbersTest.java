package com.fasterxml.jackson.core.json.async;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;

public class AsyncRootNumbersTest extends AsyncTestBase
{
    private final JsonFactory JSON_F = new JsonFactory();

    public void testRootInts() throws Exception {
        _testRootInts("10", 10);
        _testRootInts(" 10", 10);
        _testRootInts("10   ", 10);

        _testRootInts("0", 0);
        _testRootInts("    0", 0);
        _testRootInts("0 ", 0);

        _testRootInts("-1234", -1234);
        _testRootInts("  -1234", -1234);
        _testRootInts(" -1234  ", -1234);
    }

    private void _testRootInts(String doc, int value) throws Exception
    {
        byte[] input = _jsonDoc(doc);
        JsonFactory f = JSON_F;
        _testRootInts(value, f, input, 0, 90);
        _testRootInts(value, f, input, 0, 3);
        _testRootInts(value, f, input, 0, 2);
        _testRootInts(value, f, input, 0, 1);

        _testRootInts(value, f, input, 1, 90);
        _testRootInts(value, f, input, 1, 3);
        _testRootInts(value, f, input, 1, 1);
    }

    private void _testRootInts(int value, JsonFactory f,
            byte[] data, int offset, int readSize) throws IOException
    {
        AsyncReaderWrapper r = asyncForBytes(f, readSize, data, offset);
        assertNull(r.currentToken());

        assertToken(JsonToken.VALUE_NUMBER_INT, r.nextToken());
        assertEquals(value, r.getIntValue());
        assertNull(r.nextToken());
        assertTrue(r.isClosed());
    }

    public void testRootDoublesSimple() throws Exception {
        _testRootDoubles("10.0", 10.0);
        _testRootDoubles(" 10.0", 10.0);
        _testRootDoubles("10.0   ", 10.0);

        _testRootDoubles("-1234.25", -1234.25);
        _testRootDoubles("  -1234.25", -1234.25);
        _testRootDoubles(" -1234.25  ", -1234.25);

        _testRootDoubles("0.25", 0.25);
        _testRootDoubles(" 0.25", 0.25);
        _testRootDoubles("0.25   ", 0.25);
    }

    public void testRootDoublesScientific() throws Exception
    {
        _testRootDoubles("9e3", 9e3);
        _testRootDoubles("  9e3", 9e3);
        _testRootDoubles("9e3  ", 9e3);

        _testRootDoubles("9e-2", 9e-2);
        _testRootDoubles("  9e-2", 9e-2);
        _testRootDoubles("9e-2  ", 9e-2);

        _testRootDoubles("-12.5e3", -12.5e3);
        _testRootDoubles("  -12.5e3", -12.5e3);
        _testRootDoubles(" -12.5e3  ", -12.5e3);

        _testRootDoubles("-12.5E3", -12.5e3);
        _testRootDoubles("  -12.5E3", -12.5e3);
        _testRootDoubles("-12.5E3  ", -12.5e3);

        _testRootDoubles("-12.5E-2", -12.5e-2);
        _testRootDoubles("  -12.5E-2", -12.5e-2);
        _testRootDoubles(" -12.5E-2  ", -12.5e-2);

        _testRootDoubles("0e-05", 0e-5);
        _testRootDoubles("0e-5  ", 0e-5);
        _testRootDoubles("  0e-5", 0e-5);

        _testRootDoubles("0e1", 0e1);
        _testRootDoubles("0e1  ", 0e1);
        _testRootDoubles("  0e1", 0e1);
    }

    private void _testRootDoubles(String doc, double value) throws Exception
    {
        byte[] input = _jsonDoc(doc);
        JsonFactory f = JSON_F;
        _testRootDoubles(value, f, input, 0, 90);
        _testRootDoubles(value, f, input, 0, 3);
        _testRootDoubles(value, f, input, 0, 2);
        _testRootDoubles(value, f, input, 0, 1);

        _testRootDoubles(value, f, input, 1, 90);
        _testRootDoubles(value, f, input, 1, 3);
        _testRootDoubles(value, f, input, 1, 1);
    }

    private void _testRootDoubles(double value, JsonFactory f,
            byte[] data, int offset, int readSize) throws IOException
    {
        AsyncReaderWrapper r = asyncForBytes(f, readSize, data, offset);
        assertNull(r.currentToken());

        assertToken(JsonToken.VALUE_NUMBER_FLOAT, r.nextToken());
        assertEquals(value, r.getDoubleValue());
        assertNull(r.nextToken());
        assertTrue(r.isClosed());
    }
}
