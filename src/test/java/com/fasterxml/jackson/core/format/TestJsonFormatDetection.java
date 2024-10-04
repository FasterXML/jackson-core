package com.fasterxml.jackson.core.format;

import java.io.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;

import static org.junit.jupiter.api.Assertions.*;

class TestJsonFormatDetection extends com.fasterxml.jackson.core.JUnit5TestBase
{
    @Test
    void simpleValidArray() throws Exception
    {
        JsonFactory jsonF = new JsonFactory();
        DataFormatDetector detector = new DataFormatDetector(jsonF);
        final String ARRAY_JSON = "[ 1, 2 ]";
        DataFormatMatcher matcher = detector.findFormat(new ByteArrayInputStream(ARRAY_JSON.getBytes("UTF-8")));
        // should have match
        assertTrue(matcher.hasMatch());
        assertEquals("JSON", matcher.getMatchedFormatName());
        assertSame(jsonF, matcher.getMatch());
        // no "certain" match with JSON, but solid:
        assertEquals(MatchStrength.SOLID_MATCH, matcher.getMatchStrength());
        // and thus:
        JsonParser jp = matcher.createParserWithMatch();
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        assertNull(jp.nextToken());
        jp.close();
    }

    @Test
    void simpleValidObject() throws Exception
    {
        JsonFactory jsonF = new JsonFactory();
        DataFormatDetector detector = new DataFormatDetector(jsonF);
        final String JSON = "{  \"field\" : true }";
        DataFormatMatcher matcher = detector.findFormat(new ByteArrayInputStream(JSON.getBytes("UTF-8")));
        // should have match
        assertTrue(matcher.hasMatch());
        assertEquals("JSON", matcher.getMatchedFormatName());
        assertSame(jsonF, matcher.getMatch());
        // no "certain" match with JSON, but solid:
        assertEquals(MatchStrength.SOLID_MATCH, matcher.getMatchStrength());
        // and thus:
        JsonParser p = matcher.createParserWithMatch();
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("field", p.currentName());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertNull(p.nextToken());
        p.close();
    }

    /**
     * While JSON String is not a strong match alone, it should
     * be detected unless some better match is available
     */
    @Test
    void simpleValidString() throws Exception
    {
        JsonFactory jsonF = new JsonFactory();
        DataFormatDetector detector = new DataFormatDetector(jsonF);
        final String JSON = "\"JSON!\"";
        final byte[] bytes = JSON.getBytes("UTF-8");

        _testSimpleValidString(jsonF, detector.findFormat(bytes));
        _testSimpleValidString(jsonF, detector.findFormat(new ByteArrayInputStream(bytes)));
    }

    private void _testSimpleValidString(JsonFactory jsonF, DataFormatMatcher matcher) throws Exception
    {
        // should have match
        assertTrue(matcher.hasMatch());
        assertEquals("JSON", matcher.getMatchedFormatName());
        assertSame(jsonF, matcher.getMatch());
        assertEquals(MatchStrength.WEAK_MATCH, matcher.getMatchStrength());
        JsonParser jp = matcher.createParserWithMatch();
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals("JSON!", jp.getText());
        assertNull(jp.nextToken());
        jp.close();
    }

    @Test
    void simpleInvalid() throws Exception
    {
        DataFormatDetector detector = new DataFormatDetector(new JsonFactory());
        final String NON_JSON = "<root />";
        DataFormatMatcher matcher = detector.findFormat(new ByteArrayInputStream(NON_JSON.getBytes("UTF-8")));
        // should not have match
        assertFalse(matcher.hasMatch());
        assertNull(matcher.getMatchedFormatName());
        // and thus:
        assertEquals(MatchStrength.INCONCLUSIVE, matcher.getMatchStrength());
        // also:
        assertNull(matcher.createParserWithMatch());
    }

}
