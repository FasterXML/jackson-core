package com.fasterxml.jackson.core.util;

import java.nio.charset.Charset;

/**
 * Request payload wrapper class, which returns the payload object as string
 * The class converts the byte[] to String based on the charset defined for the payload object
 */
public class RequestPayloadWrapper {
	
	private static final String DEFAULT_CHARSET = "UTF-8";
	private byte[] requestPayload;
	private String charset;
	
	public RequestPayloadWrapper(byte[] requestPayload, String charset){
		this.requestPayload = requestPayload;
		this.charset = charset;
	}

	@Override
	public String toString() {
		//if request payload is null, return
		if(requestPayload == null){
			return null;
		}
		
		//check if charset is present, if not use the default charset
		Charset charsetObj = null;
		if(charset == null || "".equals(charset.trim())){
			charsetObj = Charset.forName(DEFAULT_CHARSET);
		}
		else{
			charsetObj = Charset.forName(charset);
		}
		
		return (new String(requestPayload, charsetObj));
	}
	
	

}
