package com.fasterxml.jackson.failing;

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

import static com.fasterxml.jackson.core.TestBase.a2q;

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
            24,
            24,
            1,
            25
        ),
        new InvalidJson(
            "Incorrect case for true literal",
            "{\"shouldYouAvoidWritingJsonLikeThis\": TRUE}",
            41,
            41,
            1,
            42
        ),
        new InvalidJson(
            "Incorrect case for null literal",
            "{\"licensePlate\": NULL}",
            20,
            20,
            1,
            21
        ),
        // NOTE: to be removed, eventually
        new InvalidJson(
            "Invalid JSON with raw unicode character",
            // javac will parse the 3-byte unicode control sequence, it will be passed to the parser as a raw unicode character
            a2q("{'validJson':'\u274c','right', 'here'}"),
            26,
            24,
            1,
            25
        )
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
                assertEquals(invalidJson.byteOffset, location.getByteOffset(), "Incorrect byte offset (for '"+msg+"')");
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

        private final String _name;
        public final String input;
        public final int byteOffset;
        public final int charOffset;
        public final int lineNr;
        public final int columnNr;
    }
}
