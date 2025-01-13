package tools.jackson.core.unittest.json;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import tools.jackson.core.JsonParser;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.io.SerializedString;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.testutil.MockDataInput;

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
        ObjectReadContext ctxt = ObjectReadContext.empty();
        return closeParsers(
                JSON_F.createParser(ctxt, new InputStreamReader(inputStream)),
                JSON_F.createParser(ctxt, inputStream),
                JSON_F.createParser(ctxt, new MockDataInput("{}"))
        );
    }

    public void initJsonParserClosedCaseTest(String parserName, JsonParser parser) {
        this.parser = parser;
    }

    @MethodSource("parsers")
    @ParameterizedTest(name = "{0}")
    void nullReturnedOnClosedParserOnNextFieldName(String parserName, JsonParser parser) throws Exception {
        initJsonParserClosedCaseTest(parserName, parser);
        assertNull(parser.nextName());
    }

    @MethodSource("parsers")
    @ParameterizedTest(name = "{0}")
    void falseReturnedOnClosedParserOnNextFieldNameSerializedString(String parserName, JsonParser parser) throws Exception {
        initJsonParserClosedCaseTest(parserName, parser);
        assertFalse(parser.nextName(new SerializedString("")));
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
