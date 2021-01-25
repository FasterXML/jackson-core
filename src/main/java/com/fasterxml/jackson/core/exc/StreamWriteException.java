package com.fasterxml.jackson.core.exc;

import com.fasterxml.jackson.core.*;

/**
 * Intermediate base class for all write-side streaming processing problems,
 * mostly content generation issues.
 */
public class StreamWriteException
    extends JacksonException
{
    private final static long serialVersionUID = 3L;

    protected transient JsonGenerator _processor;

    public StreamWriteException(Throwable rootCause, JsonGenerator g) {
        super(rootCause);
        _processor = g;
    }

    public StreamWriteException(String msg, JsonGenerator g) {
        super(msg);
        _processor = g;
    }

    public StreamWriteException(String msg, Throwable rootCause, JsonGenerator g) {
        super(msg, null, rootCause);
        _processor = g;
    }

    /**
     * Fluent method that may be used to assign originating {@link JsonGenerator},
     * to be accessed using {@link #processor()}.
     *
     * @param g Generator to assign
     *
     * @return This exception instance (to allow call chaining)
     */
    public StreamWriteException withGenerator(JsonGenerator g) {
        _processor = g;
        return this;
    }

    @Override
    public JsonGenerator processor() { return _processor; }
}
