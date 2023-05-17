package tools.jackson.core.json;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import tools.jackson.core.JsonParser;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.io.SerializedString;
import tools.jackson.core.testsupport.MockDataInput;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests asserts that using closed `JsonParser` doesn't cause ArrayIndexOutOfBoundsException
 * with `nextXxx()` methods but returns `null` as expected.
 */
@RunWith(Parameterized.class)
public class JsonParserClosedCaseTest {
    private static final JsonFactory JSON_F = new JsonFactory();

    private JsonParser parser;

    /**
     * Creates a list of parsers to tests.
     *
     * @return List of Object[2]. Object[0] is is the name of the class, Object[1] is instance itself.
     * @throws IOException when closing stream fails.
     */
    @Parameters(name = "{0}")
    public static Collection<Object[]> parsers() throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[] { '{', '}' });
        ObjectReadContext ctxt = ObjectReadContext.empty();
        return closeParsers(
                JSON_F.createParser(ctxt, new InputStreamReader(inputStream)),
                JSON_F.createParser(ctxt, inputStream),
                JSON_F.createParser(ctxt, new MockDataInput("{}"))
        );
    }

    public JsonParserClosedCaseTest(String parserName, JsonParser parser) {
        this.parser = parser;
    }

    @Test
    public void testNullReturnedOnClosedParserOnNextFieldName() throws Exception {
        Assert.assertNull(parser.nextName());
    }

    @Test
    public void testFalseReturnedOnClosedParserOnNextFieldNameSerializedString() throws Exception {
        Assert.assertFalse(parser.nextName(new SerializedString("")));
    }

    @Test
    public void testNullReturnedOnClosedParserOnNextToken() throws Exception {
        Assert.assertNull(parser.nextToken());
    }

    @Test
    public void testNullReturnedOnClosedParserOnNextValue() throws Exception {
        Assert.assertNull(parser.nextValue());
    }

    private static Collection<Object[]> closeParsers(JsonParser... parsersToClose) throws IOException {
        List<Object[]> list = new ArrayList<Object[]>();
        for (JsonParser p : parsersToClose) {
            p.close();
            list.add(new Object[] { p.getClass().getSimpleName(), p });
        }
        return list;
    }
}
