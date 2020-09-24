/* Jackson JSON-processor.
 *
 * Copyright (c) 2007- Tatu Saloranta, tatu.saloranta@iki.fi
 */

package com.fasterxml.jackson.core;

/**
 * Intermediate base class for all problems encountered when
 * processing (parsing, generating) JSON content
 * that are not pure I/O problems.
 * Regular {@link java.io.IOException}s will be passed through as is.
 * Sub-class of {@link java.io.IOException} for convenience.
 */
public class JsonProcessingException extends JacksonException
{
    private final static long serialVersionUID = 123; // eclipse complains otherwise

    protected JsonProcessingException(String msg, JsonLocation loc, Throwable rootCause) {
        super(msg, loc, rootCause);
    }

    protected JsonProcessingException(String msg) {
        super(msg);
    }

    protected JsonProcessingException(String msg, JsonLocation loc) {
        this(msg, loc, null);
    }

    protected JsonProcessingException(String msg, Throwable rootCause) {
        this(msg, null, rootCause);
    }

    protected JsonProcessingException(Throwable rootCause) {
        this(null, null, rootCause);
    }
}
