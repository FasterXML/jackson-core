/* Jackson JSON-processor.
 *
 * Copyright (c) 2007- Tatu Saloranta, tatu.saloranta@iki.fi
 */

package com.fasterxml.jackson.core;

import com.fasterxml.jackson.core.exc.StreamWriteException;

/**
 * Exception type for exceptions during JSON writing, such as trying
 * to output  content in wrong context (non-matching end-array or end-object,
 * for example).
 */
public class JsonGenerationException
    extends StreamWriteException
{
    private final static long serialVersionUID = 3L;

    public JsonGenerationException(Throwable rootCause, JsonGenerator g) {
        super(rootCause, g);
    }

    public JsonGenerationException(String msg, JsonGenerator g) {
        super(msg, g);
        _processor = g;
    }

    public JsonGenerationException(String msg, Throwable rootCause, JsonGenerator g) {
        super(msg, rootCause, g);
        _processor = g;
    }
}
