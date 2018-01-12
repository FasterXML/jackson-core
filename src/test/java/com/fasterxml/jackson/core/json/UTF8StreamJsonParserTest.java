package com.fasterxml.jackson.core.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.io.SerializedString;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class UTF8StreamJsonParserTest {
    private final JsonFactory JSON_F = new JsonFactory();

    @Test(expected = JsonParseException.class)
    public void testExceptionOnClosedParserOnNextFieldName() throws Exception {
        JsonParser parser = _prepareClosedParser();
        parser.nextFieldName();
    }

    @Test(expected = JsonParseException.class)
    public void testExceptionOnClosedParserOnNextFieldNameSerializedString() throws Exception {
        JsonParser parser = _prepareClosedParser();
        parser.nextFieldName(new SerializedString(""));
    }

    @Test(expected = JsonParseException.class)
    public void testExceptionOnClosedParserOnNextToken() throws Exception {
        JsonParser parser = _prepareClosedParser();
        parser.nextToken();
    }

    @Test(expected = JsonParseException.class)
    public void testExceptionOnClosedParserOnNextValue() throws Exception {
        JsonParser parser = _prepareClosedParser();
        parser.nextValue();
    }

    private JsonParser _prepareClosedParser() throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream("{}".getBytes());
        JsonParser parser = JSON_F.createParser(inputStream);
        parser.close();
        return parser;
    }

}