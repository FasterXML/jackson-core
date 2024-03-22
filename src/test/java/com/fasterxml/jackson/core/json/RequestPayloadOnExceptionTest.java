package com.fasterxml.jackson.core.json;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;

import static org.junit.jupiter.api.Assertions.*;

class RequestPayloadOnExceptionTest extends JUnit5TestBase
{
    /**
     * Tests for Request payload data (bytes) on parsing error
     */
    @Test
    void requestPayloadAsBytesOnParseException() throws Exception {
        requestPayloadAsBytesOnParseExceptionInternal(true, "nul");
        requestPayloadAsBytesOnParseExceptionInternal(false, "nul");
    }

    /**
     * Tests for Request payload data (String) on parsing error
     */
    @Test
    void requestPayloadAsStringOnParseException() throws Exception {
        requestPayloadAsStringOnParseExceptionInternal(true, "nul");
        requestPayloadAsStringOnParseExceptionInternal(false, "nul");
    }

    /**
     * Tests for Raw Request payload data on parsing error
     */
    @Test
    void rawRequestPayloadOnParseException() throws Exception {
        rawRequestPayloadOnParseExceptionInternal(true, "nul");
        rawRequestPayloadOnParseExceptionInternal(false, "nul");
    }

    /**
     * Tests for no Request payload data on parsing error
     */
    @Test
    void noRequestPayloadOnParseException() throws Exception {
        noRequestPayloadOnParseExceptionInternal(true, "nul");
        noRequestPayloadOnParseExceptionInternal(false, "nul");
    }

    /**
     * Tests for Request payload data which is null
     */
    @Test
    void nullRequestPayloadOnParseException() throws Exception {
        nullRequestPayloadOnParseExceptionInternal(true, "nul");
        nullRequestPayloadOnParseExceptionInternal(false, "nul");
    }

    /**
     * Tests for null Charset in Request payload data
     */
    @Test
    void nullCharsetOnParseException() throws Exception {
        nullCharsetOnParseExceptionInternal(true, "nul");
        nullCharsetOnParseExceptionInternal(false, "nul");
    }

    /*
     * *******************Private Methods*************************
     */
    private void requestPayloadAsBytesOnParseExceptionInternal(boolean isStream, String value) throws Exception {
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

    private void requestPayloadAsStringOnParseExceptionInternal(boolean isStream, String value) throws Exception {
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

    private void rawRequestPayloadOnParseExceptionInternal(boolean isStream, String value) throws Exception {
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

    private void noRequestPayloadOnParseExceptionInternal(boolean isStream, String value) throws Exception {
        final String doc = "{ \"key1\" : " + value + " }";
        JsonParser jp = isStream ? createParserUsingStream(doc, "UTF-8") : createParserUsingReader(doc);
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        try {
            jp.nextToken();
            fail("Expecting parsing exception");
        } catch (JsonParseException ex) {
            assertNull(ex.getRequestPayload(), "Request payload data should be null");
        }
        jp.close();
    }

    private void nullRequestPayloadOnParseExceptionInternal(boolean isStream, String value) throws Exception {
        final String doc = "{ \"key1\" : " + value + " }";
        JsonParser jp = isStream ? createParserUsingStream(doc, "UTF-8") : createParserUsingReader(doc);
        jp.setRequestPayloadOnError(null, "UTF-8");
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        try {
            jp.nextToken();
            fail("Expecting parsing exception");
        } catch (JsonParseException ex) {
            assertNull(ex.getRequestPayload(), "Request payload data should be null");
        }
        jp.close();
    }

    private void nullCharsetOnParseExceptionInternal(boolean isStream, String value) throws Exception {
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
