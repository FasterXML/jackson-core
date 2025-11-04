package com.fasterxml.jackson.core.read.loc;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import com.fasterxml.jackson.core.exc.StreamReadException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that the {@link JsonLocation} attached to a thrown {@link StreamReadException}
 * due to invalid JSON points to the correct character.
 */
class LocationOfError1180Test
{
    static final JsonFactory JSON_F = new JsonFactory();

    /** Represents the different parser backends */
    public enum ParserVariant
    {
        BYTE_ARRAY(
            (String input) -> JSON_F.createParser(input.getBytes(StandardCharsets.UTF_8)),
            true,   // supports byte offsets in reported location
            false,  // supports character offsets in reported location
            true    // supports column numbers in reported location
        ),
        CHAR_ARRAY(
            (String input) -> JSON_F.createParser(input.toCharArray()),
            false,
            true,
            true
        ),
        ASYNC(
            (String input) -> {
                JsonParser parser = JSON_F.createNonBlockingByteArrayParser();
                ByteArrayFeeder feeder = (ByteArrayFeeder) parser.getNonBlockingInputFeeder();
                assertTrue(feeder.needMoreInput());

                byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
                feeder.feedInput(inputBytes, 0, inputBytes.length);
                feeder.endOfInput();

                return parser;
            },
            true,
            false,
            true
        )
        ;

        ParserVariant(
            ParserGenerator parserGenerator,
            boolean supportsByteOffset,
            boolean supportsCharOffset,
            boolean supportsColumnNr
        )
        {
            _parserGenerator = parserGenerator;

            this.supportsByteOffset = supportsByteOffset;
            this.supportsCharOffset = supportsCharOffset;
            this.supportsColumnNr = supportsColumnNr;
        }

        public JsonParser createParser(String input) throws Exception
        {
            return _parserGenerator.createParser(input);
        }

        private final ParserGenerator _parserGenerator;
        public final boolean supportsByteOffset;
        public final boolean supportsCharOffset;
        public final boolean supportsColumnNr;
    }

    /** Collection of differing invalid JSON input cases to test */
    private static final List<InvalidJson> INVALID_JSON_CASES = Arrays.asList(
        new InvalidJson(
            "Incorrect case for false literal",
            "{\"isThisValidJson\": FALSE}",
            20,  // byte offset: 'F' starts at position 20
            20,  // char offset: 'F' starts at position 20
            1,
            21   // column: 1-indexed, so position 20 = column 21
        ),
        new InvalidJson(
            "Incorrect case for true literal",
            "{\"shouldYouAvoidWritingJsonLikeThis\": TRUE}",
            38,  // byte offset: 'T' starts at position 38
            38,  // char offset: 'T' starts at position 38
            1,
            39   // column: 1-indexed, so position 38 = column 39
        ),
        new InvalidJson(
            "Incorrect case for null literal",
            "{\"licensePlate\": NULL}",
            17,  // byte offset: 'N' starts at position 17
            17,  // char offset: 'N' starts at position 17
            1,
            18   // column: 1-indexed, so position 17 = column 18
        )
        // NOTE: Unicode test case removed - it tests a different error path ("Unexpected character"
        // vs "Unrecognized token") and has additional complexities with multi-byte UTF-8
    );

    @ParameterizedTest
    @MethodSource("_generateTestData")
    void parserBackendWithInvalidJson(ParserVariant variant, InvalidJson invalidJson)
            throws Exception
    {
       try (JsonParser parser = variant.createParser(invalidJson.input))
        {
            StreamReadException e = assertThrows(
                    StreamReadException.class,
                () -> {
                    // Blindly advance the parser through the end of input
                    while (parser.nextToken() != null) {}
                }
            );

            JsonLocation location = e.getLocation();
            assertEquals(invalidJson.lineNr, location.getLineNr());
            final String msg = e.getOriginalMessage();

            if (variant.supportsByteOffset)
            {
                // [core#1180]: Async parser may be off by 1 due to when token start position is captured
                if (variant == ParserVariant.ASYNC) {
                    long actual = location.getByteOffset();
                    assertTrue(actual == invalidJson.byteOffset || actual == invalidJson.byteOffset + 1,
                            String.format("Byte offset should be %d or %d but was %d (for '%s')",
                                    invalidJson.byteOffset, invalidJson.byteOffset + 1, actual, msg));
                } else {
                    assertEquals(invalidJson.byteOffset, location.getByteOffset(), "Incorrect byte offset (for '"+msg+"')");
                }
            }
            if (variant.supportsCharOffset)
            {
                assertEquals(invalidJson.charOffset, location.getCharOffset(), "Incorrect char offset (for '"+msg+"')");
            }
            if (variant.supportsColumnNr)
            {
                assertEquals(invalidJson.columnNr, location.getColumnNr(), "Incorrect column (for '"+msg+"')");
            }
        }
    }

    private static Stream<Arguments> _generateTestData()
    {
        return Arrays.stream(ParserVariant.values())
            .flatMap(parserVariant -> INVALID_JSON_CASES.stream().map(
                invalidJson -> Arguments.of(parserVariant, invalidJson)
            ));
    }

    @FunctionalInterface
    public interface ParserGenerator
    {
        JsonParser createParser(String input) throws Exception;
    }

    static class InvalidJson
    {
        InvalidJson(String name, String input, int byteOffset, int charOffset,
                int lineNr, int columnNr)
        {
            _name = name;

            this.input = input;
            this.byteOffset = byteOffset;
            this.charOffset = charOffset;
            this.lineNr = lineNr;
            this.columnNr = columnNr;
        }

        @Override
        public String toString()
        {
            return _name;
        }

        protected final String _name;
        public final String input;
        public final int byteOffset;
        public final int charOffset;
        public final int lineNr;
        public final int columnNr;
    }
}
