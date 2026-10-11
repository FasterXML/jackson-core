package tools.jackson.core.unittest.json;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import tools.jackson.core.*;
import tools.jackson.core.exc.JacksonIOException;
import tools.jackson.core.exc.UnexpectedEndOfInputException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.unittest.JacksonCoreTestBase;

import static org.junit.jupiter.api.Assertions.*;

class DataInputEOF1761Test extends JacksonCoreTestBase
{
    private final JsonFactory FACTORY = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS).build();

    static Stream<Arguments> documents() {
        return Arrays.stream(ALL_MODES).boxed().flatMap(mode ->
                Stream.of("\"abc", "{\"abc", "{\"a\":", "[ 12", "+")
                        .map(doc -> Arguments.of(mode, doc)));
    }

    @ParameterizedTest
    @MethodSource("documents")
    void truncatedDocument(int mode, String doc) {
        try (JsonParser p = createParser(FACTORY, mode, doc)) {
            UnexpectedEndOfInputException e = assertThrows(UnexpectedEndOfInputException.class,
                    () -> consume(p));
            assertTrue(e.getMessage().contains("Unexpected end-of-input"));
        }
    }

    static Stream<Arguments> stringAccessors() {
        return Arrays.stream(ALL_MODES).boxed().flatMap(mode ->
                IntStream.range(0, 9).boxed().flatMap(accessor ->
                        Stream.of("\"abc", "\"a\\u12", "\"a\\")
                                .map(input -> Arguments.of(mode, accessor, input))));
    }

    @ParameterizedTest
    @MethodSource("stringAccessors")
    void truncatedLazyString(int mode, int accessor, String input) {
        try (JsonParser p = createParser(FACTORY, mode, input)) {
            assertEquals(JsonToken.VALUE_STRING, p.nextToken());
            assertThrows(UnexpectedEndOfInputException.class, () -> accessString(p, accessor));
        }
    }

    @ParameterizedTest
    @MethodSource("modes")
    void truncatedPropertyNameWithNextName(int mode) {
        try (JsonParser p = createParser(FACTORY, mode, "{\"abc")) {
            assertEquals(JsonToken.START_OBJECT, p.nextToken());
            assertThrows(UnexpectedEndOfInputException.class, p::nextName);
        }
    }

    static IntStream modes() { return Arrays.stream(ALL_MODES); }

    static Stream<Arguments> splitUtf8() {
        byte[][] inputs = { { '"', (byte) 0xC3 }, { '"', (byte) 0xE2, (byte) 0x82 },
                { '"', (byte) 0xF0, (byte) 0x9F, (byte) 0x92 } };
        return Arrays.stream(ALL_BINARY_MODES).boxed().flatMap(mode ->
                Arrays.stream(inputs).flatMap(input -> Stream.of(false, true)
                        .map(streaming -> Arguments.of(mode, input, streaming))));
    }

    @ParameterizedTest
    @MethodSource("splitUtf8")
    void truncatedUtf8String(int mode, byte[] input, boolean streaming) {
        try (JsonParser p = createParser(FACTORY, mode, input)) {
            assertEquals(JsonToken.VALUE_STRING, p.nextToken());
            assertThrows(UnexpectedEndOfInputException.class, () -> {
                if (streaming) p.readString(new StringWriter());
                else p.getString();
            });
        }
    }

    static Stream<Arguments> binaryStrings() {
        return Arrays.stream(ALL_BINARY_MODES).boxed().flatMap(mode ->
                Stream.of("\"", "\"YWJj", "\"YQ==").flatMap(input -> Stream.of(false, true)
                        .map(streaming -> Arguments.of(mode, input, streaming))));
    }

    @ParameterizedTest
    @MethodSource("binaryStrings")
    void truncatedBinaryString(int mode, String input, boolean streaming) {
        try (JsonParser p = createParser(FACTORY, mode, input)) {
            assertEquals(JsonToken.VALUE_STRING, p.nextToken());
            assertThrows(UnexpectedEndOfInputException.class, () -> {
                if (streaming) p.readBinaryValue(Base64Variants.getDefaultVariant(), new ByteArrayOutputStream());
                else p.getBinaryValue(Base64Variants.getDefaultVariant());
            });
        }
    }

    static Stream<Arguments> validDocuments() {
        return Arrays.stream(ALL_MODES).boxed().flatMap(mode ->
                Stream.of("0", "-0", "1", "12", "-12", "+12", "0.1", "123.5", "1e2", "-1e-2", "+1e+2",
                        "true", "false", "null", "\"abc\"", "{\"a\":\"b\"}", "[12]", "{}", "[]")
                        .map(input -> Arguments.of(mode, input)));
    }

    @ParameterizedTest
    @MethodSource("validDocuments")
    void normalEndOfDocument(int mode, String input) {
        try (JsonParser p = createParser(FACTORY, mode, input)) {
            assertDoesNotThrow(() -> consume(p));
            assertNull(p.nextToken());
        }
    }

    static Stream<Arguments> completeNumbers() {
        return Arrays.stream(ALL_MODES).boxed().flatMap(mode ->
                Stream.of("0", "-0", "1", "12", "-12", "+12", "0.1", "123.5", "1e2", "-1e-2", "+1e+2")
                        .map(input -> Arguments.of(mode, input)));
    }

    @ParameterizedTest
    @MethodSource("completeNumbers")
    void completeNumberKeepsItsValue(int mode, String input) {
        try (JsonParser p = createParser(FACTORY, mode, input)) {
            assertTrue(p.nextToken().isNumeric());
            assertEquals(input, p.getString());
            assertEquals(new BigDecimal(input), p.getDecimalValue());
            assertNull(p.nextToken());
        }
    }

    static Stream<Arguments> incompleteValues() {
        return Arrays.stream(ALL_MODES).boxed().flatMap(mode ->
                Stream.of("-", "+", "1.", "1e", "1e+", "1e-", "tru", "fals", "nul", "[12", "{\"a\":12")
                        .map(input -> Arguments.of(mode, input)));
    }

    @ParameterizedTest
    @MethodSource("incompleteValues")
    void incompleteValueIsNotAcceptedAsRootEOF(int mode, String input) {
        try (JsonParser p = createParser(FACTORY, mode, input)) {
            assertThrows(JacksonException.class, () -> consume(p));
        }
    }

    static Stream<Arguments> permissiveNumbers() {
        return Arrays.stream(ALL_MODES).boxed().flatMap(mode ->
                Stream.of(".5", "-.5", "+.5", "1.", "+1.", "-1.", "00", "0001", "00.5", "01e2")
                        .map(input -> Arguments.of(mode, input)));
    }

    @ParameterizedTest
    @MethodSource("permissiveNumbers")
    void allowedNumberEndsNormallyAtRoot(int mode, String input) {
        JsonFactory permissive = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS)
                .enable(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS)
                .enable(JsonReadFeature.ALLOW_TRAILING_DECIMAL_POINT_FOR_NUMBERS)
                .enable(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS).build();
        try (JsonParser p = createParser(permissive, mode, input)) {
            assertTrue(p.nextToken().isNumeric());
            assertEquals(new BigDecimal(input), p.getDecimalValue());
            assertNull(p.nextToken());
        }
    }

    static Stream<Arguments> writerFailures() {
        return Arrays.stream(ALL_MODES).boxed().flatMap(mode -> Stream.of(false, true).flatMap(eof ->
                Stream.of(false, true).map(streaming -> Arguments.of(mode, eof, streaming))));
    }

    @ParameterizedTest
    @MethodSource("writerFailures")
    void outputFailureRemainsIO(int mode, boolean eof, boolean streaming) {
        IOException failure = eof ? new EOFException("output") : new IOException("output");
        Writer writer = new Writer() {
            @Override public void write(char[] data, int offset, int length) throws IOException { throw failure; }
            @Override public void flush() { }
            @Override public void close() { }
        };
        try (JsonParser p = createParser(FACTORY, mode, "\"" + "é".repeat(7000) + "\"")) {
            assertEquals(JsonToken.VALUE_STRING, p.nextToken());
            JacksonIOException e = assertThrows(JacksonIOException.class, () -> {
                if (streaming) p.readString(writer);
                else p.getString(writer);
            });
            assertSame(failure, e.getCause());
        }
    }

    static Stream<Arguments> outputFailures() {
        return Arrays.stream(ALL_BINARY_MODES).boxed().flatMap(mode -> Stream.of(false, true)
                .map(eof -> Arguments.of(mode, eof)));
    }

    @ParameterizedTest
    @MethodSource("outputFailures")
    void binaryOutputFailureRemainsIO(int mode, boolean eof) {
        IOException failure = eof ? new EOFException("output") : new IOException("output");
        OutputStream out = new OutputStream() {
            @Override public void write(int value) throws IOException { throw failure; }
        };
        try (JsonParser p = createParser(FACTORY, mode, "\"YWJj\"")) {
            assertEquals(JsonToken.VALUE_STRING, p.nextToken());
            JacksonIOException e = assertThrows(JacksonIOException.class,
                    () -> p.readBinaryValue(Base64Variants.getDefaultVariant(), out));
            assertSame(failure, e.getCause());
        }
    }

    @Test
    void ordinaryInputFailureRemainsIO() {
        IOException failure = new IOException("input");
        InputStream input = new InputStream() {
            private int index;
            private final byte[] prefix = "\"a".getBytes(StandardCharsets.UTF_8);
            @Override public int read() throws IOException {
                if (index == prefix.length) throw failure;
                return prefix[index++];
            }
        };
        try (JsonParser p = createParserForDataInput(FACTORY, new DataInputStream(input))) {
            assertEquals(JsonToken.VALUE_STRING, p.nextToken());
            JacksonIOException e = assertThrows(JacksonIOException.class, p::getString);
            assertSame(failure, e.getCause());
        }
    }

    @Test
    void realDataInputStreamTruncation() {
        try (JsonParser p = createParserForDataInput(FACTORY,
                new DataInputStream(new ByteArrayInputStream("\"abc".getBytes(StandardCharsets.UTF_8))))) {
            assertEquals(JsonToken.VALUE_STRING, p.nextToken());
            assertThrows(UnexpectedEndOfInputException.class, p::getString);
        }
    }

    private void consume(JsonParser p) {
        while (p.nextToken() != null) {
            if (p.currentToken() == JsonToken.VALUE_STRING) p.getString();
        }
    }

    private void accessString(JsonParser p, int accessor) {
        switch (accessor) {
        case 0: p.getString(); break;
        case 1: p.getString(new StringWriter()); break;
        case 2: p.readString(new StringWriter()); break;
        case 3: p.getStringCharacters(); break;
        case 4: p.getStringLength(); break;
        case 5: p.getValueAsString(); break;
        case 6: p.getValueAsString("fallback"); break;
        case 7: p.finishToken(); break;
        default: p.nextToken(); break;
        }
    }
}
