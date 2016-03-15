package com.fasterxml.jackson.core.json;

import java.io.IOException;

import com.fasterxml.jackson.core.BaseTest;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public class TestRequestBodyOnParseException extends BaseTest {
	
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
	     jp.addRequestBodyOnError(true);
	     assertToken(JsonToken.START_OBJECT, jp.nextToken());
	     try{
	    	 jp.nextToken();
	    	 fail("Expecting parsing exception");
	     }
	     catch(JsonParseException ex){
	    	 assertEquals("Request body data should match", doc, ex.getRequestBody());
	     }
	}

	private void testNoRequestBodyOnParseExceptionInternal(boolean isStream, String value) throws Exception{
		final String doc = "{ \"key1\" : "+value+" }";
	     JsonParser jp = isStream ? createParserUsingStream(doc, "UTF-8")
	                : createParserUsingReader(doc);
	     jp.addRequestBodyOnError(false);
	     assertToken(JsonToken.START_OBJECT, jp.nextToken());
	     try{
	    	 jp.nextToken();
	    	 fail("Expecting parsing exception");
	     }
	     catch(JsonParseException ex){
	    	 assertEquals("Request body data should be null", null, ex.getRequestBody());
	     }
	}
}
