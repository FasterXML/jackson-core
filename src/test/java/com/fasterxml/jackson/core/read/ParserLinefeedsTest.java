package com.fasterxml.jackson.core.read;


import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Set of basic unit tests for verifying that the basic parser
 * functionality works as expected.
 */
class ParserLinefeedsTest
        extends JUnit5TestBase
{
    @Test
    void cr() throws Exception
    {
        _testLinefeeds("\r", true);
        _testLinefeeds("\r", false);
    }

    @Test
    void lf() throws Exception
    {
        _testLinefeeds("\n", true);
        _testLinefeeds("\n", false);
    }

    @Test
    void crlf() throws Exception
    {
        _testLinefeeds("\r\n", true);
        _testLinefeeds("\r\n", false);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private void _testLinefeeds(String lf, boolean useStream)
        throws IOException
    {
        String DOC = "[1,@2,@-478@]";
        DOC = DOC.replaceAll("@", lf);

        JsonParser jp = useStream ?
            createParserUsingStream(DOC, "UTF-8")
            : createParserUsingReader(DOC);

        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertEquals(1, jp.currentLocation().getLineNr());

        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(1, jp.getIntValue());
        assertEquals(1, jp.currentLocation().getLineNr());

        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(2, jp.getIntValue());
        assertEquals(2, jp.currentLocation().getLineNr());

        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(-478, jp.getIntValue());
        assertEquals(3, jp.currentLocation().getLineNr());

        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        assertEquals(4, jp.currentLocation().getLineNr());

        jp.close();
    }
}
