package com.fasterxml.jackson.core.json;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;

import static org.junit.jupiter.api.Assertions.*;

public class RequestPayloadOnExceptionTest extends JUnit5TestBase
{
    /**
     * Tests for Request payload data (bytes) on parsing error
     */
    @Test
    public void testRequestPayloadAsBytesOnParseException() throws Exception {
        testRequestPayloadAsBytesOnParseExceptionInternal(true, "nul");
        testRequestPayloadAsBytesOnParseExceptionInternal(false, "nul");
    }

    /**
     * Tests for Request payload data (String) on parsing error
     */
    @Test
    public void testRequestPayloadAsStringOnParseException() throws Exception {
        testRequestPayloadAsStringOnParseExceptionInternal(true, "nul");
        testRequestPayloadAsStringOnParseExceptionInternal(false, "nul");
    }

    /**
     * Tests for Raw Request payload data on parsing error
     */
    @Test
    public void testRawRequestPayloadOnParseException() throws Exception {
        testRawRequestPayloadOnParseExceptionInternal(true, "nul");
        testRawRequestPayloadOnParseExceptionInternal(false, "nul");
    }

    /**
     * Tests for no Request payload data on parsing error
     */
    @Test
    public void testNoRequestPayloadOnParseException() throws Exception {
        testNoRequestPayloadOnParseExceptionInternal(true, "nul");
        testNoRequestPayloadOnParseExceptionInternal(false, "nul");
    }

    /**
     * Tests for Request payload data which is null
     */
    @Test
    public void testNullRequestPayloadOnParseException() throws Exception {
        testNullRequestPayloadOnParseExceptionInternal(true, "nul");
        testNullRequestPayloadOnParseExceptionInternal(false, "nul");
    }

    /**
     * Tests for null Charset in Request payload data
     */
    @Test
    public void testNullCharsetOnParseException() throws Exception {
        testNullCharsetOnParseExceptionInternal(true, "nul");
        testNullCharsetOnParseExceptionInternal(false, "nul");
    }

    /*
     * *******************Private Methods*************************
     */
    @Test
    private void testRequestPayloadAsBytesOnParseExceptionInternal(boolean isStream, String value) throws Exception {
        final String doc = "{ \"key1\" : " + value + " }";
        JsonParser jp = isStream ? createParserUsingStream(doc, "UTF-8") : createParserUsingReader(doc);
        jp.setRequestPayloadOnError(doc.getBytes(), "UTF-8");
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        try {
            jp.nextToken();
            fail("Expecting parsing exception");
        } catch (JsonParseException ex) {
            assertEquals(doc, ex.getRequestPayloadAsString(), "Request payload data should match");
            assertTrue(ex.getMessage().contains("Request payload : " + doc), "Message contains request body");
        }
        jp.close();
    }

    @Test
    private void testRequestPayloadAsStringOnParseExceptionInternal(boolean isStream, String value) throws Exception {
        final String doc = "{ \"key1\" : " + value + " }";
        JsonParser jp = isStream ? createParserUsingStream(doc, "UTF-8") : createParserUsingReader(doc);
        jp.setRequestPayloadOnError(doc);
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        try {
            jp.nextToken();
            fail("Expecting parsing exception");
        } catch (JsonParseException ex) {
            assertEquals(doc, ex.getRequestPayloadAsString(), "Request payload data should match");
            assertTrue(ex.getMessage().contains("Request payload : " + doc), "Message contains request body");
        }
        jp.close();
    }

    @Test
    private void testRawRequestPayloadOnParseExceptionInternal(boolean isStream, String value) throws Exception {
        final String doc = "{ \"key1\" : " + value + " }";
        JsonParser jp = isStream ? createParserUsingStream(doc, "UTF-8") : createParserUsingReader(doc);
        jp.setRequestPayloadOnError(doc.getBytes(), "UTF-8");
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        try {
            jp.nextToken();
            fail("Expecting parsing exception");
        } catch (JsonParseException ex) {
            assertTrue(((byte[]) ex.getRequestPayload().getRawPayload()).length > 0);
            assertTrue(ex.getMessage().contains("Request payload : " + doc), "Message contains request body");
        }
        jp.close();
    }

    @Test
    private void testNoRequestPayloadOnParseExceptionInternal(boolean isStream, String value) throws Exception {
        final String doc = "{ \"key1\" : " + value + " }";
        JsonParser jp = isStream ? createParserUsingStream(doc, "UTF-8") : createParserUsingReader(doc);
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        try {
            jp.nextToken();
            fail("Expecting parsing exception");
        } catch (JsonParseException ex) {
            assertEquals(null, ex.getRequestPayload(), "Request payload data should be null");
        }
        jp.close();
    }

    @Test
    private void testNullRequestPayloadOnParseExceptionInternal(boolean isStream, String value) throws Exception {
        final String doc = "{ \"key1\" : " + value + " }";
        JsonParser jp = isStream ? createParserUsingStream(doc, "UTF-8") : createParserUsingReader(doc);
        jp.setRequestPayloadOnError(null, "UTF-8");
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        try {
            jp.nextToken();
            fail("Expecting parsing exception");
        } catch (JsonParseException ex) {
            assertEquals(null, ex.getRequestPayload(), "Request payload data should be null");
        }
        jp.close();
    }

    @Test
    private void testNullCharsetOnParseExceptionInternal(boolean isStream, String value) throws Exception {
        final String doc = "{ \"key1\" : " + value + " }";
        JsonParser jp = isStream ? createParserUsingStream(doc, "UTF-8") : createParserUsingReader(doc);
        jp.setRequestPayloadOnError(doc.getBytes(), "UTF-8");
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        try {
            jp.nextToken();
            fail("Expecting parsing exception");
        } catch (JsonParseException ex) {
            assertEquals(doc, ex.getRequestPayloadAsString(), "Request payload data should match");
            assertTrue(ex.getMessage().contains("Request payload : " + doc), "Message contains request body");
        }
        jp.close();
    }
}
