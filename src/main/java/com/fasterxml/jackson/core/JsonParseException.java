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

    @Override
    public JsonParseException withParser(JsonParser p) {
        _processor = p;
        return this;
    }

    @Override
    public JsonParseException withRequestPayload(RequestPayload p) {
        _requestPayload = p;
        return this;
    }
}
