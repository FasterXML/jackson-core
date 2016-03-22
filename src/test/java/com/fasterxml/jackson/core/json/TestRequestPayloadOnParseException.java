package com.fasterxml.jackson.core.json;

import java.io.IOException;

import com.fasterxml.jackson.core.BaseTest;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public class TestRequestPayloadOnParseException extends BaseTest {
	
	/**
	 * Tests for Request body data on parsing error
	 * @throws Exception
	 */
	public void testRequestBodyOnParseException() throws Exception{
		testRequestBodyOnParseExceptionInternal(true,"nul");
		testRequestBodyOnParseExceptionInternal(false,"nul");
	}
	
	/**
	 * Tests for no Request body data on parsing error
	 * @throws Exception
	 */
	public void testNoRequestBodyOnParseException() throws Exception{
		testNoRequestBodyOnParseExceptionInternal(true,"nul");
		testNoRequestBodyOnParseExceptionInternal(false,"nul");
	}
	
	/*
	 * *******************Private Methods*************************
	 */
	private void testRequestBodyOnParseExceptionInternal(boolean isStream, String value) throws Exception{
		final String doc = "{ \"key1\" : "+value+" }";
	     JsonParser jp = isStream ? createParserUsingStream(doc, "UTF-8")
	                : createParserUsingReader(doc);
	     jp.setRequestPayloadOnError(doc.getBytes(), "UTF-8");
	     assertToken(JsonToken.START_OBJECT, jp.nextToken());
	     try{
	    	 jp.nextToken();
	    	 fail("Expecting parsing exception");
	     }
	     catch(JsonParseException ex){
	    	 assertEquals("Request payload data should match", doc, ex.getRequestPayload());
	    	 assertTrue("Message contains request body", ex.getMessage().contains("Request Payload : "+doc));
	     }
	}

	private void testNoRequestBodyOnParseExceptionInternal(boolean isStream, String value) throws Exception{
		final String doc = "{ \"key1\" : "+value+" }";
	     JsonParser jp = isStream ? createParserUsingStream(doc, "UTF-8")
	                : createParserUsingReader(doc);
	     assertToken(JsonToken.START_OBJECT, jp.nextToken());
	     try{
	    	 jp.nextToken();
	    	 fail("Expecting parsing exception");
	     }
	     catch(JsonParseException ex){
	    	 assertEquals("Request payload data should be null", null, ex.getRequestPayload());
	     }
	}
}
