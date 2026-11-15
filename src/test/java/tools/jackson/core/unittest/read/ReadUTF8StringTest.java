package tools.jackson.core.unittest.read;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.exc.JacksonIOException;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.unittest.JacksonCoreTestBase;
import tools.jackson.core.util.JsonParserDelegate;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReadUTF8StringTest extends JacksonCoreTestBase
{
    private final JsonFactory factory = new JsonFactory();

    @Test
    void stringsAcrossBackends() throws Exception {
        String[][] cases = {
                {"\"\"", ""},
                {"\"ASCII\"", "ASCII"},
                {"\"é日😀\"", "é日😀"},
                {"\"a\\n\\t\\\"\\\\z\"", "a\n\t\"\\z"},
                {"\"\\u00e9\\u65e5\\ud83d\\ude00\"", "é日😀"},
                {"\"\\ud800x\\udc00\"", "?x?"},
                {"\"" + "é日😀".repeat(10000) + "\"", "é日😀".repeat(10000)}
        };
        for (int mode : ALL_MODES) {
            for (String[] entry : cases) {
                try (JsonParser parser = createParser(factory, mode, "[" + entry[0] + ",42]")) {
                    assertToken(JsonToken.START_ARRAY, parser.nextToken());
                    assertToken(JsonToken.VALUE_STRING, parser.nextToken());
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    byte[] expected = entry[1].getBytes(UTF_8);
                    assertEquals(expected.length, parser.readUTF8String(output));
                    assertArrayEquals(expected, output.toByteArray());
                    assertToken(JsonToken.VALUE_NUMBER_INT, parser.nextToken());
                    assertEquals(42, parser.getIntValue());
                    assertToken(JsonToken.END_ARRAY, parser.nextToken());
                }
            }
        }
    }

    @Test
    void cachedStringsAndDelegates() throws Exception {
        for (boolean cached : new boolean[] {false, true}) {
            try (JsonParser parser = new JsonParserDelegate(factory.createParser("\"é日😀\"".getBytes(UTF_8)))) {
                parser.nextToken();
                if (cached) {
                    assertEquals("é日😀", parser.getString());
                }
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                assertEquals(9, parser.readUTF8String(output));
                assertArrayEquals("é日😀".getBytes(UTF_8), output.toByteArray());
            }
        }
    }

    @Test
    void stringLimitsCountCharacters() throws Exception {
        JsonFactory limited = factory.rebuild().streamReadConstraints(
                StreamReadConstraints.builder().maxStringLength(4).build()).build();
        for (int mode : ALL_MODES) {
            try (JsonParser parser = createParser(limited, mode, "\"é日😀\"")) {
                parser.nextToken();
                assertEquals(9, parser.readUTF8String(new ByteArrayOutputStream()));
            }
            try (JsonParser parser = createParser(limited, mode, "\"é日😀x\"")) {
                parser.nextToken();
                assertThrows(StreamConstraintsException.class,
                        () -> parser.readUTF8String(new ByteArrayOutputStream()));
            }
        }
    }

    @Test
    void destinationFailureIsWrapped() throws Exception {
        IOException failure = new IOException("destination failed");
        OutputStream output = new OutputStream() {
            @Override
            public void write(int value) throws IOException {
                throw failure;
            }
        };
        for (int mode : ALL_MODES) {
            try (JsonParser parser = createParser(factory, mode, "\"value\"")) {
                parser.nextToken();
                JacksonIOException thrown = assertThrows(JacksonIOException.class,
                        () -> parser.readUTF8String(output));
                assertSame(failure, thrown.getCause());
            }
        }
    }

    @Test
    void nonBlockingParserRejectsOutput() throws Exception {
        try (JsonParser parser = factory.createNonBlockingByteArrayParser(ObjectReadContext.empty())) {
            assertThrows(UnsupportedOperationException.class,
                    () -> parser.readUTF8String(new ByteArrayOutputStream()));
        }
    }

    @Test
    void nonStringTokens() throws Exception {
        try (JsonParser parser = factory.createParser("[true,1.25,null]")) {
            assertEquals(0, parser.readUTF8String(new ByteArrayOutputStream()));
            parser.nextToken();
            for (String expected : new String[] {"true", "1.25", "null"}) {
                parser.nextToken();
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                assertEquals(expected.length(), parser.readUTF8String(output));
                assertEquals(expected, output.toString(UTF_8));
            }
        }
    }

    @Test
    void malformedBytesMatchExistingDecoder() throws Exception {
        Random random = new Random(98213);
        for (int i = 0; i < 5000; i++) {
            byte[] json = new byte[2 + random.nextInt(30)];
            random.nextBytes(json);
            json[0] = '"';
            json[json.length - 1] = '"';
            byte[] expected;
            try (JsonParser parser = factory.createParser(json)) {
                parser.nextToken();
                expected = parser.getString().getBytes(UTF_8);
            } catch (tools.jackson.core.JacksonException failure) {
                try (JsonParser parser = factory.createParser(json)) {
                    assertThrows(failure.getClass(), () -> {
                        parser.nextToken();
                        parser.readUTF8String(new ByteArrayOutputStream());
                    });
                }
                continue;
            }
            try (JsonParser parser = factory.createParser(json)) {
                parser.nextToken();
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                assertEquals(expected.length, parser.readUTF8String(output));
                assertArrayEquals(expected, output.toByteArray());
            }
        }
    }
}
