package tools.jackson.core.unittest.read.loc;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.unittest.JacksonCoreTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DataInputLocation601Test extends JacksonCoreTestBase
{
    @Test
    void tokenOffsets() throws Exception {
        for (boolean constrained : new boolean[] { false, true }) {
            JsonFactory factory = constrained ? JsonFactory.builder()
                    .streamReadConstraints(StreamReadConstraints.builder()
                            .maxDocumentLength(10000).build()).build() : new JsonFactory();
            for (boolean bom : new boolean[] { false, true }) {
                for (boolean nextName : new boolean[] { false, true }) {
                    for (boolean readStrings : new boolean[] { false, true }) {
                        _verifyOffsets(factory, bom, nextName, readStrings);
                    }
                }
            }
        }
    }

    private void _verifyOffsets(JsonFactory factory, boolean bom,
            boolean nextName, boolean readStrings) throws Exception {
        byte[] doc = (" {\n\"\u00e9\": [12, -3.5, true, false, null, \"\u20ac\uD83D\uDE00\"],"
                + "\"nested\": {\"value\": 7}} ").getBytes(StandardCharsets.UTF_8);
        if (bom) {
            byte[] prefixed = new byte[doc.length + 3];
            prefixed[0] = (byte) 0xEF;
            prefixed[1] = (byte) 0xBB;
            prefixed[2] = (byte) 0xBF;
            System.arraycopy(doc, 0, prefixed, 3, doc.length);
            doc = prefixed;
        }
        try (JsonParser expected = factory.createParser(ObjectReadContext.empty(), doc);
                JsonParser actual = factory.createParser(ObjectReadContext.empty(),
                        (DataInput) new DataInputStream(new ByteArrayInputStream(doc)))) {
            assertEquals(expected.currentLocation().getByteOffset(),
                    actual.currentLocation().getByteOffset());
            while (true) {
                if (nextName) {
                    assertEquals(expected.nextName(), actual.nextName());
                } else {
                    assertEquals(expected.nextToken(), actual.nextToken());
                }
                JsonToken token = expected.currentToken();
                assertEquals(token, actual.currentToken());
                assertEquals(expected.currentLocation().getByteOffset(),
                        actual.currentLocation().getByteOffset(), "current offset for " + token);
                if (token == null) {
                    break;
                }
                assertEquals(expected.currentTokenLocation().getByteOffset(),
                        actual.currentTokenLocation().getByteOffset(), "token offset for " + token);
                assertEquals(-1L, actual.currentLocation().getCharOffset());
                if (readStrings && token == JsonToken.VALUE_STRING) {
                    assertEquals(expected.getString(), actual.getString());
                    assertEquals(expected.currentLocation().getByteOffset(),
                            actual.currentLocation().getByteOffset());
                }
            }
        }
    }
}
