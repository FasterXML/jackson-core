/* Jackson JSON-processor.
 *
 * Copyright (c) 2007- Tatu Saloranta, tatu.saloranta@iki.fi
 */

package com.fasterxml.jackson.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

import com.fasterxml.jackson.core.util.RequestPayloadWrapper;

/**
 * Exception type for parsing problems, used when non-well-formed content
 * (content that does not conform to JSON syntax as per specification)
 * is encountered.
 */
public class JsonParseException extends JsonProcessingException {
    private static final long serialVersionUID = 2L; // 2.7

    protected JsonParser _processor;
    protected RequestPayloadWrapper requestPayload;

    @Deprecated // since 2.7
    public JsonParseException(String msg, JsonLocation loc) {
        super(msg, loc);
    }

    @Deprecated // since 2.7
    public JsonParseException(String msg, JsonLocation loc, Throwable root) {
        super(msg, loc, root);
    }

    /**
     * Constructor that uses current parsing location as location, and
     * sets processor (accessible via {@link #getProcessor()}) to
     * specified parser.
     *
     * @since 2.7
     */
    public JsonParseException(JsonParser p, String msg) {
        super(msg, (p == null) ? null : p.getCurrentLocation());
        _processor = p;
    }

    /**
     * @since 2.7
     */
    public JsonParseException(JsonParser p, String msg, Throwable root) {
        super(msg, (p == null) ? null : p.getCurrentLocation(), root);
        _processor = p;
    }
    
    /**
     * @since 2.7
     */
    public JsonParseException(JsonParser p, String msg, JsonLocation loc) {
        super(msg, loc);
        _processor = p;
    }

    /**
     * @since 2.7
     */
    public JsonParseException(JsonParser p, String msg, JsonLocation loc, Throwable root) {
        super(msg, loc, root);
        _processor = p;
    }
    
    /*
     *******************************************************************
     Extended Constructors for setting the Request Body in the exception
     *******************************************************************
     */
    public JsonParseException(JsonParser p, String msg, RequestPayloadWrapper requestPayload) {
        this(p, msg);
        this.requestPayload = requestPayload;
    }

    public JsonParseException(JsonParser p, String msg, Throwable root, RequestPayloadWrapper requestPayload) {
        this(p, msg, root);
        this.requestPayload = requestPayload;
    }

    public JsonParseException(JsonParser p, String msg, JsonLocation loc, RequestPayloadWrapper requestPayload) {
        this(p, msg, loc);
        this.requestPayload = requestPayload;
    }
    
    public JsonParseException(JsonParser p, String msg, JsonLocation loc, Throwable root, RequestPayloadWrapper requestPayload) {
        this(p, msg, loc, root);
        this.requestPayload = requestPayload;
    }

    /**
     * Fluent method that may be used to assign originating {@link JsonParser},
     * to be accessed using {@link #getProcessor()}.
     *
     * @since 2.7
     */
    public JsonParseException withParser(JsonParser p) {
        _processor = p;
        return this;
    }

    @Override
    public JsonParser getProcessor() {
        return _processor;
    }

    /**
     * Method to get the request payload as string if present
     * @return request body
     */
    public String getRequestPayload(){
    	return requestPayload != null ? requestPayload.toString() : null;
    }
    
    /**
     * Overriding the getMessage() to include the request body
     */
    @Override 
    public String getMessage() {
    	String msg = super.getMessage();
    	return requestPayload != null ? (msg + "\nRequest Payload : " + getRequestPayload()) : msg;
    }

    
}
