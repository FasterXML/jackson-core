package com.fasterxml.jackson.core.util;

import java.nio.charset.Charset;

/**
 * Request payload wrapper class, which returns the payload object
 * The wrapper class can hold the request payload bytes or directly the string representation of the payload
 * The class converts the byte[] to String, based on the request source type i.e, bytes or String
 */
public class RequestPayloadWrapper {
	//default charset
	private static final String DEFAULT_CHARSET = "UTF-8";
	
	//request payload as byte[]
	private byte[] requestPayloadAsBytes;
	
	//request payload as String
	private String requestPayloadAsString;
	
	//Charset if the request payload is set in bytes
	private String charset;
	
	public RequestPayloadWrapper(byte[] requestPayloadAsBytes, String charset) {
		this.requestPayloadAsBytes = requestPayloadAsBytes;
		this.charset = charset;
	}
	
	public RequestPayloadWrapper(String requestPayloadAsString) {
        this.requestPayloadAsString = requestPayloadAsString;
    }
	
	/**
	 * Returns the raw request payload object i.e, either byte[] or String
	 * 
	 * @return Object which is a raw request payload i.e, either byte[] or String
	 */
	public Object getRawRequestPayload() {
	    if(this.requestPayloadAsBytes != null) {
	        return this.requestPayloadAsBytes;
	    }
	    
	    return this.requestPayloadAsString;
	}

	@Override
	public String toString() {
		//if request payload is null, return
		if(requestPayloadAsBytes == null && requestPayloadAsString == null){
			return null;
		}
		
		if(requestPayloadAsBytes != null) {
    		//check if charset is present, if not use the default charset
    		Charset charsetObj = null;
    		if(charset == null || "".equals(charset.trim())){
    			charsetObj = Charset.forName(DEFAULT_CHARSET);
    		}
    		else{
    			charsetObj = Charset.forName(charset);
    		}
    		
    		return (new String(requestPayloadAsBytes, charsetObj));
		}
		
		return requestPayloadAsString;
		
	}
	
	

}
