package com.fasterxml.jackson.core.async;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.json.JsonReadFeature;

import static org.junit.jupiter.api.Assertions.*;

// [jackson-core#1742]: feed split inside low surrogate escape of a property name
class AsyncSplitSurrogateInFieldName1742Test extends AsyncTestBase
{
    private final JsonFactory FACTORY = newStreamFactory();
    private final JsonFactory APOS_FACTORY = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .build();

    // U+1F600 GRINNING FACE
    private static final String SMILEY = new String(Character.toChars(0x1F600));
    // U+1D11E MUSICAL SYMBOL G CLEF
    private static final String G_CLEF = new String(Character.toChars(0x1D11E));

    @Test
    void splitInLowSurrogateEscape() throws Exception
    {
        _testSplitInLowSurrogateEscape(false);
        _testSplitInLowSurrogateEscape(true);
    }

    @Test
    void splitInLowSurrogateEscapeApos() throws Exception
    {
        _testSplitInLowSurrogateEscapeApos(false);
        _testSplitInLowSurrogateEscapeApos(true);
    }

    private void _testSplitInLowSurrogateEscape(boolean byteBuffer) throws Exception
    {
        _testAllSplits(byteBuffer, FACTORY, "{\"\\uD83D\\uDE00\":1}", SMILEY);
        _testAllSplits(byteBuffer, FACTORY, "{\"ab\\uD83D\\uDE00cd\":1}", "ab"+SMILEY+"cd");
        _testAllSplits(byteBuffer, FACTORY, "{\"x\":{\"\\uD83D\\uDE00\":1}}", "x", SMILEY);
        _testAllSplits(byteBuffer, FACTORY, "{\"\\uD83D\\uDE00\\uD834\\uDD1E\":1}", SMILEY+G_CLEF);
        _testAllSplits(byteBuffer, FACTORY, "{\"a\":1,\"\\ud834\\udd1e\":2}", "a", G_CLEF);
    }

    private void _testSplitInLowSurrogateEscapeApos(boolean byteBuffer) throws Exception
    {
        _testAllSplits(byteBuffer, APOS_FACTORY, "{'\\uD83D\\uDE00':1}", SMILEY);
        _testAllSplits(byteBuffer, APOS_FACTORY, "{'ab\\uD83D\\uDE00cd':1}", "ab"+SMILEY+"cd");
    }

    private void _testAllSplits(boolean byteBuffer, JsonFactory f, String json, String... expNames) throws Exception
    {
        byte[] doc = json.getBytes(StandardCharsets.UTF_8);
        for (int split = 1; split < doc.length; ++split) {
            _testSplit(byteBuffer, f, doc, split, String.join("|", expNames));
        }
    }

    private void _testSplit(boolean byteBuffer, JsonFactory f, byte[] doc, int split, String expNames) throws Exception
    {
        JsonParser p = byteBuffer ? f.createNonBlockingByteBufferParser()
                : f.createNonBlockingByteArrayParser();
        NonBlockingInputFeeder feeder = p.getNonBlockingInputFeeder();
        _feed(feeder, doc, 0, split);
        boolean fedAll = false;
        List<String> names = new ArrayList<>();
        JsonToken t;
        while (true) {
            t = p.nextToken();
            if (t == JsonToken.NOT_AVAILABLE) {
                assertTrue(feeder.needMoreInput(),
                        "NOT_AVAILABLE with unread input, split at "+split+", byteBuffer="+byteBuffer);
                if (fedAll) {
                    feeder.endOfInput();
                } else {
                    _feed(feeder, doc, split, doc.length);
                    fedAll = true;
                }
                continue;
            }
            if (t == null) {
                break;
            }
            if (t == JsonToken.FIELD_NAME) {
                names.add(p.currentName());
            }
        }
        assertEquals(expNames, String.join("|", names), "split at "+split+", byteBuffer="+byteBuffer);
        p.close();
    }

    private static void _feed(NonBlockingInputFeeder feeder, byte[] doc, int start, int end)
        throws Exception
    {
        if (feeder instanceof ByteBufferFeeder) {
            ((ByteBufferFeeder) feeder).feedInput(ByteBuffer.wrap(doc, start, end - start));
        } else {
            ((ByteArrayFeeder) feeder).feedInput(doc, start, end);
        }
    }
}
