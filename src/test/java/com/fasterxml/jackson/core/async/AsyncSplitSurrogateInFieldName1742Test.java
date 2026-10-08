package com.fasterxml.jackson.core.async;

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

    @Test
    void splitInLowSurrogateEscape() throws Exception
    {
        _testAllSplits(FACTORY, "{\"\\uD83D\\uDE00\":1}", SMILEY);
        _testAllSplits(FACTORY, "{\"ab\\uD83D\\uDE00cd\":1}", "ab"+SMILEY+"cd");
        _testAllSplits(FACTORY, "{\"x\":{\"\\uD83D\\uDE00\":1}}", "x", SMILEY);
    }

    @Test
    void splitInLowSurrogateEscapeApos() throws Exception
    {
        _testAllSplits(APOS_FACTORY, "{'\\uD83D\\uDE00':1}", SMILEY);
        _testAllSplits(APOS_FACTORY, "{'ab\\uD83D\\uDE00cd':1}", "ab"+SMILEY+"cd");
    }

    private void _testAllSplits(JsonFactory f, String json, String... expNames) throws Exception
    {
        byte[] doc = json.getBytes(StandardCharsets.UTF_8);
        for (int split = 1; split < doc.length; ++split) {
            _testSplit(f, doc, split, String.join("|", expNames));
        }
    }

    private void _testSplit(JsonFactory f, byte[] doc, int split, String expNames) throws Exception
    {
        JsonParser p = f.createNonBlockingByteArrayParser();
        ByteArrayFeeder feeder = (ByteArrayFeeder) p.getNonBlockingInputFeeder();
        feeder.feedInput(doc, 0, split);
        boolean fedAll = false;
        List<String> names = new ArrayList<>();
        JsonToken t;
        while (true) {
            t = p.nextToken();
            if (t == JsonToken.NOT_AVAILABLE) {
                assertTrue(feeder.needMoreInput(),
                        "NOT_AVAILABLE with unread input, split at "+split);
                if (fedAll) {
                    feeder.endOfInput();
                } else {
                    feeder.feedInput(doc, split, doc.length);
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
        assertEquals(expNames, String.join("|", names), "split at "+split);
        p.close();
    }
}
