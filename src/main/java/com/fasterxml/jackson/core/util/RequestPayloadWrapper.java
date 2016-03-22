package com.fasterxml.jackson.core.util;

import java.nio.charset.Charset;

/**
 * Request payload wrapper class, which returns the payload object as string
 * The class converts the byte[] to String based on the charset defined for the payload object
 */
public class RequestPayloadWrapper {
	
	private byte[] requestPayload;
	private String charset;
	
	public RequestPayloadWrapper(byte[] requestPayload, String charset){
		this.requestPayload = requestPayload;
		this.charset = charset;
	}

	@Override
	public String toString() {
		String requestPayloadStr = "";
		if(requestPayload != null){
			requestPayloadStr = new String(requestPayload, Charset.forName(charset));
		}
		return requestPayloadStr;
	}
	
	

}
