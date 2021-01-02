/* Jackson JSON-processor.
 *
 * Copyright (c) 2007- Tatu Saloranta, tatu.saloranta@iki.fi
 */

package com.fasterxml.jackson.core;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.util.RequestPayload;

/**
 * Exception type for parsing problems, used when non-well-formed content
 * (content that does not conform to JSON syntax as per specification)
 * is encountered.
 */
public class JsonParseException extends StreamReadException
{
    private static final long serialVersionUID = 3L;

    /**
     * Constructor that uses current parsing location as location, and
     * sets processor (accessible via {@link #getProcessor()}) to
     * specified parser.
     *
     * @param p Parser in use when encountering issue reported
     * @param msg Base exception message to use
     */
    public JsonParseException(JsonParser p, String msg) {
        super(p, msg);
    }

    public JsonParseException(JsonParser p, String msg, Throwable root) {
        super(p, msg, root);
    }

    public JsonParseException(JsonParser p, String msg, JsonLocation loc) {
        super(p, msg, loc);
    }

    public JsonParseException(JsonParser p, String msg, JsonLocation loc, Throwable root) {
        super(msg, loc, root);
    }

    /**
     * Fluent method that may be used to assign originating {@link JsonParser},
     * to be accessed using {@link #getProcessor()}.
     *<p>
     * NOTE: `this` instance is modified and no new instance is constructed.
     *
     * @param p Parser instance to assign to this exception
     *
     * @return This exception instance to allow call chaining
     */
    @Override
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
     * @param payload Payload to assign to this exception
     *
     * @return This exception instance to allow call chaining
     */
    @Override
    public JsonParseException withRequestPayload(RequestPayload payload) {
        _requestPayload = payload;
        return this;
    }
}
