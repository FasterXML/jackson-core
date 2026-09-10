package tools.jackson.core.unittest.json.async;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonToken;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.unittest.async.AsyncTestBase;
import tools.jackson.core.unittest.testutil.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.*;

// Tests for [core#1640]: non-blocking (async) parser should reject numeric values
// not followed by a valid separator in non-root context, matching blocking parsers (#1615).
class AsyncNumberSeparator1640Test extends AsyncTestBase
{
    private final JsonFactory JSON_F = new JsonFactory();

    @Test
    void mangledIntInArray() throws Exception {
        for (int readSize : new int[]{90, 3, 1}) {
            _testMangledNumber("[123true]", readSize);
            _testMangledNumber("[123false]", readSize);
            _testMangledNumber("[123null]", readSize);
            _testMangledNumber("[-99z]", readSize);
        }
    }

    @Test
    void mangledFloatFractionInArray() throws Exception {
        for (int readSize : new int[]{90, 3, 1}) {
            _testMangledNumber("[1.5true]", readSize);
            _testMangledNumber("[0.25x]", readSize);
        }
    }

    @Test
    void mangledFloatExponentInArray() throws Exception {
        for (int readSize : new int[]{90, 3, 1}) {
            _testMangledNumber("[1.5e2x]", readSize);
            _testMangledNumber("[9e3k]", readSize);
            _testMangledNumber("[1e10z]", readSize);
        }
    }

    // Valid JSON should still parse without error
    @Test
    void validSeparatorsInArray() throws Exception {
        for (int readSize : new int[]{90, 3, 1}) {
            _testValidInt("[123]", 123, readSize);
            _testValidInt("[123,456]", 123, readSize);
            _testValidInt("[ 123 ]", 123, readSize);
            _testValidDouble("[1.5]", 1.5, readSize);
            _testValidDouble("[1.5,2.5]", 1.5, readSize);
            _testValidDouble("[1.5e2]", 150.0, readSize);
            _testValidDouble("[ 1.5e2 ]", 150.0, readSize);
        }
    }

    private void _testMangledNumber(String doc, int readSize) throws Exception {
        _testMangledNumber(doc, readSize, "Expected space");
    }

    private void _testMangledNumber(String doc, int readSize, String expectedMsg) throws Exception
    {
        byte[] input = _jsonDoc(doc);
        try (AsyncReaderWrapper p = asyncForBytes(JSON_F, readSize, input, 0)) {
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            try {
                JsonToken t = p.nextToken();
                fail("Should have failed on '" + doc + "' (readSize=" + readSize + "); got: " + t);
            } catch (StreamReadException e) {
                verifyException(e, expectedMsg);
            }
        }
    }

    private void _testValidInt(String doc, int expected, int readSize) throws Exception {
        byte[] input = _jsonDoc(doc);
        try (AsyncReaderWrapper p = asyncForBytes(JSON_F, readSize, input, 0)) {
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(expected, p.getIntValue(),
                    "Int value mismatch for '" + doc + "' (readSize=" + readSize + ")");
        }
    }

    private void _testValidDouble(String doc, double expected, int readSize) throws Exception {
        byte[] input = _jsonDoc(doc);
        try (AsyncReaderWrapper p = asyncForBytes(JSON_F, readSize, input, 0)) {
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertEquals(expected, p.getDoubleValue(), 0.001,
                    "Double value mismatch for '" + doc + "' (readSize=" + readSize + ")");
        }
    }
}
