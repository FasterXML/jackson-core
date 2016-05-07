/* Jackson JSON-processor.
 *
 * Copyright (c) 2007- Tatu Saloranta, tatu.saloranta@iki.fi
 */

package com.fasterxml.jackson.core;

import com.fasterxml.jackson.core.util.RequestPayload;

/**
 * Exception type for parsing problems, used when non-well-formed content
 * (content that does not conform to JSON syntax as per specification)
 * is encountered.
 */
public class JsonParseException extends JsonProcessingException {
    private static final long serialVersionUID = 2L; // 2.7

    // transient since 2.7.4
    protected transient JsonParser _processor;

    /**
     * Optional payload that can be assigned to pass along for error reporting
     * or handling purposes. Core streaming parser implementations DO NOT
     * initialize this; it is up to using applications and frameworks to
     * populate it.
     *
     * @since 2.8
     */
    protected RequestPayload _requestPayload;

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

    /**
     * Fluent method that may be used to assign originating {@link JsonParser},
     * to be accessed using {@link #getProcessor()}.
     *<p>
     * NOTE: `this` instance is modified and no new instance is constructed.
     *
     * @since 2.7
     */
    public JsonParseException withParser(JsonParser p) {
        _processor = p;
        return this;
    }

    /**
     * Fluent method that may be used to assign payload to this exception,
     * to let recipient access it for diagnostics purposes.
     *<p>
     * NOTE: `this` instance is modified and no new instance is constructed.
     *
     * @since 2.8
     */
    public JsonParseException withRequestPayload(RequestPayload p) {
        _requestPayload = p;
        return this;
    }
    
    @Override
    public JsonParser getProcessor() {
        return _processor;
    }

    /**
     * Method that may be called to find payload that was being parsed, if
     * one was specified for parser that threw this Exception.
     *
     * @return request body, if payload was specified; `null` otherwise
     *
     * @since 2.8
     */
    public RequestPayload getRequestPayload() {
        return _requestPayload;
    }

    /**
     * The method returns the String representation of the request payload if
     * one was specified for parser that threw this Exception.
     * 
     * @return request body as String, if payload was specified; `null` otherwise
     * 
     * @since 2.8
     */
    public String getRequestPayloadAsString() {
        return (_requestPayload != null) ? _requestPayload.toString() : null;
    }
    
    /**
     * Overriding the getMessage() to include the request body
     */
    @Override 
    public String getMessage() {
        String msg = super.getMessage();
        if (_requestPayload != null) {
            msg +=  "\nRequest payload : " + _requestPayload.toString();
        }
        return msg;
    }
}
