package tools.jackson.core.json;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestUnicode extends tools.jackson.core.JacksonCoreTestBase
{
    @Test
    void surrogates() throws Exception
    {
        JsonFactory f = new JsonFactory();
        _testSurrogates(f, true);
        _testSurrogates(f, false);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private void _testSurrogates(JsonFactory f, boolean checkText) throws IOException
    {
        byte[] json = "{\"text\":\"\uD83D\uDE03\"}".getBytes("UTF-8");
        // first
        JsonParser jp = f.createParser(ObjectReadContext.empty(), json);
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertToken(JsonToken.PROPERTY_NAME, jp.nextToken());
        if (checkText) {
            assertEquals("text", jp.getString());
        }
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        if (checkText) {
            assertEquals("\uD83D\uDE03", jp.getString());
        }
        assertToken(JsonToken.END_OBJECT, jp.nextToken());
        jp.close();
    }
}
