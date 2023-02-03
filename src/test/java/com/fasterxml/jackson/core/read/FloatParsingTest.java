package com.fasterxml.jackson.core.read;

import com.fasterxml.jackson.core.BaseTest;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.TokenStreamFactory;

// for [core#755]
public class FloatParsingTest extends BaseTest
{
    private final byte[] FLOATS_DOC = readResource("/data/floats-755.json");

    public void testFloatArrayViaInputStream() throws Exception
    {
        _testFloatArray(MODE_INPUT_STREAM, false);
        _testFloatArray(MODE_INPUT_STREAM_THROTTLED, false);
    }

    public void testFloatArrayViaInputStreamWithFastParser() throws Exception
    {
        _testFloatArray(MODE_INPUT_STREAM, true);
        _testFloatArray(MODE_INPUT_STREAM_THROTTLED, true);
    }

    public void testFloatArrayViaReader() throws Exception {
        _testFloatArray(MODE_READER, false);
    }

    public void testFloatArrayViaReaderWithFastParser() throws Exception {
        _testFloatArray(MODE_READER, true);
    }

    public void testFloatArrayViaDataInput() throws Exception {
       _testFloatArray(MODE_DATA_INPUT, false);
    }

    public void testFloatArrayViaDataInputWithFasrtParser() throws Exception {
        _testFloatArray(MODE_DATA_INPUT, true);
    }

    private void _testFloatArray(int mode, boolean useFastParser) throws Exception
    {
        // construct new instance to reduce buffer recycling etc:
        TokenStreamFactory jsonF = newStreamFactory();

        JsonParser p = createParser(jsonF, mode, FLOATS_DOC);
        if (useFastParser) {
            p.enable(JsonParser.Feature.USE_FAST_DOUBLE_PARSER);
        }

        assertToken(JsonToken.START_ARRAY, p.nextToken());

        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(7.038531e-26f, p.getFloatValue());

        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(1.199999988079071f, p.getFloatValue());

        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(3.4028235677973366e38f, p.getFloatValue());

        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(7.006492321624086e-46f, p.getFloatValue());

        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());

        p.close();
    }
}
