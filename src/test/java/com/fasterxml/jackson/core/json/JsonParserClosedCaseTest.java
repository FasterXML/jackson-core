package com.fasterxml.jackson.core.json;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.core.testsupport.MockDataInput;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests asserts that using closed `JsonParser` doesn't cause ArrayIndexOutOfBoundsException
 * with `nextXxx()` methods but returns `null` as expected.
 */
public class JsonParserClosedCaseTest {
    private static final JsonFactory JSON_F = new JsonFactory();

    JsonParser parser;

    /**
     * Creates a list of parsers to tests.
     *
     * @return List of Object[2]. Object[0] is is the name of the class, Object[1] is instance itself.
     * @throws IOException when closing stream fails.
     */
    public static Collection<Object[]> parsers() throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[] { '{', '}' });

        return closeParsers(
                (ReaderBasedJsonParser) JSON_F.createParser(new InputStreamReader(inputStream)),
                (UTF8StreamJsonParser) JSON_F.createParser(inputStream),
                (UTF8DataInputJsonParser) JSON_F.createParser(new MockDataInput("{}"))
        );
    }

    public void initJsonParserClosedCaseTest(String parserName, JsonParser parser) {
        this.parser = parser;
    }

    @MethodSource("parsers")
    @ParameterizedTest(name = "{0}")
    void nullReturnedOnClosedParserOnNextFieldName(String parserName, JsonParser parser) throws Exception {
        initJsonParserClosedCaseTest(parserName, parser);
        assertNull(parser.nextFieldName());
    }

    @MethodSource("parsers")
    @ParameterizedTest(name = "{0}")
    void falseReturnedOnClosedParserOnNextFieldNameSerializedString(String parserName, JsonParser parser) throws Exception {
        initJsonParserClosedCaseTest(parserName, parser);
        assertFalse(parser.nextFieldName(new SerializedString("")));
    }

    @MethodSource("parsers")
    @ParameterizedTest(name = "{0}")
    void nullReturnedOnClosedParserOnNextToken(String parserName, JsonParser parser) throws Exception {
        initJsonParserClosedCaseTest(parserName, parser);
        assertNull(parser.nextToken());
    }

    @MethodSource("parsers")
    @ParameterizedTest(name = "{0}")
    void nullReturnedOnClosedParserOnNextValue(String parserName, JsonParser parser) throws Exception {
        initJsonParserClosedCaseTest(parserName, parser);
        assertNull(parser.nextValue());
    }

    private static Collection<Object[]> closeParsers(JsonParser... parsersToClose) throws IOException {
        List<Object[]> list = new ArrayList<>();
        for (JsonParser p : parsersToClose) {
            p.close();
            list.add(new Object[] { p.getClass().getSimpleName(), p });
        }
        return list;
    }
}
