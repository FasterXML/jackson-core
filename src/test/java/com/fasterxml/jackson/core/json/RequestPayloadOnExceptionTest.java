package com.fasterxml.jackson.core.json;

import com.fasterxml.jackson.core.*;

public class RequestPayloadOnExceptionTest extends BaseTest
{
    /**
     * Tests for Request payload data (bytes) on parsing error
     */
    public void testRequestPayloadAsBytesOnParseException() throws Exception {
        testRequestPayloadAsBytesOnParseExceptionInternal(true, "nul");
        testRequestPayloadAsBytesOnParseExceptionInternal(false, "nul");
    }

    /**
     * Tests for Request payload data (String) on parsing error
     */
    public void testRequestPayloadAsStringOnParseException() throws Exception {
        testRequestPayloadAsStringOnParseExceptionInternal(true, "nul");
        testRequestPayloadAsStringOnParseExceptionInternal(false, "nul");
    }

    /**
     * Tests for Raw Request payload data on parsing error
     */
    public void testRawRequestPayloadOnParseException() throws Exception {
        testRawRequestPayloadOnParseExceptionInternal(true, "nul");
        testRawRequestPayloadOnParseExceptionInternal(false, "nul");
    }

    /**
     * Tests for no Request payload data on parsing error
     */
    public void testNoRequestPayloadOnParseException() throws Exception {
        testNoRequestPayloadOnParseExceptionInternal(true, "nul");
        testNoRequestPayloadOnParseExceptionInternal(false, "nul");
    }

    /**
     * Tests for Request payload data which is null
     */
    public void testNullRequestPayloadOnParseException() throws Exception {
        testNullRequestPayloadOnParseExceptionInternal(true, "nul");
        testNullRequestPayloadOnParseExceptionInternal(false, "nul");
    }

    /**
     * Tests for null Charset in Request payload data
     */
    public void testNullCharsetOnParseException() throws Exception {
        testNullCharsetOnParseExceptionInternal(true, "nul");
        testNullCharsetOnParseExceptionInternal(false, "nul");
    }

    /*
     * *******************Private Methods*************************
     */
    private void testRequestPayloadAsBytesOnParseExceptionInternal(boolean isStream, String value) throws Exception {
        final String doc = "{ \"key1\" : " + value + " }";
        JsonParser jp = isStream ? createParserUsingStream(doc, "UTF-8") : createParserUsingReader(doc);
        jp.setRequestPayloadOnError(doc.getBytes(), "UTF-8");
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        try {
            jp.nextToken();
            fail("Expecting parsing exception");
        } catch (JsonParseException ex) {
            assertEquals("Request payload data should match", doc, ex.getRequestPayloadAsString());
            assertTrue("Message contains request body", ex.getMessage().contains("Request payload : " + doc));
        }
        jp.close();
    }

    private void testRequestPayloadAsStringOnParseExceptionInternal(boolean isStream, String value) throws Exception {
        final String doc = "{ \"key1\" : " + value + " }";
        JsonParser jp = isStream ? createParserUsingStream(doc, "UTF-8") : createParserUsingReader(doc);
        jp.setRequestPayloadOnError(doc);
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        try {
            jp.nextToken();
            fail("Expecting parsing exception");
        } catch (JsonParseException ex) {
            assertEquals("Request payload data should match", doc, ex.getRequestPayloadAsString());
            assertTrue("Message contains request body", ex.getMessage().contains("Request payload : " + doc));
        }
        jp.close();
    }

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
            assertTrue("Message contains request body", ex.getMessage().contains("Request payload : " + doc));
        }
        jp.close();
    }

    private void testNoRequestPayloadOnParseExceptionInternal(boolean isStream, String value) throws Exception {
        final String doc = "{ \"key1\" : " + value + " }";
        JsonParser jp = isStream ? createParserUsingStream(doc, "UTF-8") : createParserUsingReader(doc);
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        try {
            jp.nextToken();
            fail("Expecting parsing exception");
        } catch (JsonParseException ex) {
            assertEquals("Request payload data should be null", null, ex.getRequestPayload());
        }
        jp.close();
    }

    private void testNullRequestPayloadOnParseExceptionInternal(boolean isStream, String value) throws Exception {
        final String doc = "{ \"key1\" : " + value + " }";
        JsonParser jp = isStream ? createParserUsingStream(doc, "UTF-8") : createParserUsingReader(doc);
        jp.setRequestPayloadOnError(null, "UTF-8");
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        try {
            jp.nextToken();
            fail("Expecting parsing exception");
        } catch (JsonParseException ex) {
            assertEquals("Request payload data should be null", null, ex.getRequestPayload());
        }
        jp.close();
    }

    private void testNullCharsetOnParseExceptionInternal(boolean isStream, String value) throws Exception {
        final String doc = "{ \"key1\" : " + value + " }";
        JsonParser jp = isStream ? createParserUsingStream(doc, "UTF-8") : createParserUsingReader(doc);
        jp.setRequestPayloadOnError(doc.getBytes(), "UTF-8");
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        try {
            jp.nextToken();
            fail("Expecting parsing exception");
        } catch (JsonParseException ex) {
            assertEquals("Request payload data should match", doc, ex.getRequestPayloadAsString());
            assertTrue("Message contains request body", ex.getMessage().contains("Request payload : " + doc));
        }
        jp.close();
    }
}
