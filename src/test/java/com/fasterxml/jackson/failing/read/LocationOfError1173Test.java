package com.fasterxml.jackson.failing.read;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import com.fasterxml.jackson.core.exc.StreamReadException;

import static com.fasterxml.jackson.core.BaseTest.a2q;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests that the {@link JsonLocation} attached to a thrown {@link JsonProcessingException} due to invalid json points
 * to the correct character.
 */
public class LocationOfError1173Test
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
        DATA_INPUT(
            (String input) -> JSON_F.createParser((DataInput) new DataInputStream(new ByteArrayInputStream(
                input.getBytes(StandardCharsets.UTF_8)
            ))),
            false,
            false,
            false
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
            "Object property missing colon",
            a2q("{'invalid' 'json'}"),
            11, // byte offset
            11, // char offset
            1,  // line number
            12  // column number
        ),
        new InvalidJson(
            "Comma after key in object property",
            a2q("{'invalid', 'json'}"),
            10,
            10,
            1,
            11
        ),
        new InvalidJson(
            "Missing comma between object properties",
            a2q("{'key1':'value1' 'key2':'value2'}"),
            17,
            17,
            1,
            18
        ),
        new InvalidJson(
            "Number as a property key",
            "{1234: 5678}",
            1,
            1,
            1,
            2
        ),
        new InvalidJson(
            "false literal as property key",
            "{false: true}",
            1,
            1,
            1,
            2
        ),
        new InvalidJson(
            "true literal as property key",
            "{true: false}",
            1,
            1,
            1,
            2
        ),
        new InvalidJson(
            "null literal as property key",
            "{null: \"content\"}",
            1,
            1,
            1,
            2
        ),
        new InvalidJson(
            "Missing comma between list elements",
            "[\"no\" \"commas\"]",
            6,
            6,
            1,
            7
        ),
        new InvalidJson(
            "Property key/value delimiter in list",
            "[\"still\":\"invalid\"]",
            8,
            8,
            1,
            9
        ),
        new InvalidJson(
            "Unexpected EOF",
            "{",
            1,
            1,
            1,
            2
        ),
        new InvalidJson(
            "Close marker without matching open marker",
            "}",
            0,
            0,
            1,
            1
        ),
        new InvalidJson(
            "Mismatched open/close tokens",
            "{\"open\":\"close\"]",
            15,
            15,
            1,
            16
        ),
        new InvalidJson(
            "Bare strings in JSON",
            "{missing: quotes}",
            1,
            1,
            1,
            2
        ),
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
        new InvalidJson(
            "Invalid JSON with raw unicode character",
            // javac will parse the unicode control sequence, it will be passed to the parser as a raw unicode character
            "{\"validJson\":\"\u274c\",\"right\", \"here\"}",
            26,
            24,
            1,
            25
        ),
        new InvalidJson(
            "Error in middle of line for multiline input",
            // missing comma delimiter between properties two and three
            "{\n  \"one\": 1,\n  \"two\": 2\n  \"three\": 3\n}",
            27,
            27,
            4,
            3
        ),
        new InvalidJson(
            "Error at end of line for multiline input",
            // double commas between keys
            "{\n\"key1\":\"value1\",,\n\"key2\":\"value2\"\n}",
            18,
            18,
            2,
            17
        )
    );

    @ParameterizedTest
    @MethodSource("_generateTestData")
    public void testParserBackendWithInvalidJson(ParserVariant variant, InvalidJson invalidJson)
        throws Exception
    {
        try (JsonParser parser = variant.createParser(invalidJson.input))
        {
            StreamReadException e = Assertions.assertThrows(
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
                assertEquals("Incorrect byte offset (for '"+msg+"')",
                        invalidJson.byteOffset, location.getByteOffset());
            }
            if (variant.supportsCharOffset)
            {
                assertEquals("Incorrect char offset (for '"+msg+"')",
                        invalidJson.charOffset, location.getCharOffset());
            }
            if (variant.supportsColumnNr)
            {
                assertEquals("Incorrect column (for '"+msg+"')",
                        invalidJson.columnNr, location.getColumnNr());
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
