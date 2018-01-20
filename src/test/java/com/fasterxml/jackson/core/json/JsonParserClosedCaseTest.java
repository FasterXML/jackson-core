package com.fasterxml.jackson.core.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.core.testsupport.MockDataInput;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * These tests asserts that using closed JsonParser doesn't cause ArrayIndexOutOfBoundsException.
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
        ByteArrayInputStream inputStream = new ByteArrayInputStream("{}".getBytes());

        return closeParsers(
                (ReaderBasedJsonParser) JSON_F.createParser(new InputStreamReader(inputStream)),
                (UTF8StreamJsonParser) JSON_F.createParser(inputStream),
                (UTF8DataInputJsonParser) JSON_F.createParser(new MockDataInput("{}"))
        );
    }

    public JsonParserClosedCaseTest(String parserName, JsonParser parser) {
        this.parser = parser;
    }

    @Test
    public void testNullReturnedOnClosedParserOnNextFieldName() throws Exception {
        Assert.assertNull(parser.nextFieldName());
    }

    @Test
    public void testFalseReturnedOnClosedParserOnNextFieldNameSerializedString() throws Exception {
        Assert.assertFalse(parser.nextFieldName(new SerializedString("")));
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
