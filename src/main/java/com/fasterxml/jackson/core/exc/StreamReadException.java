package com.fasterxml.jackson.core.exc;

import com.fasterxml.jackson.core.*;

/**
 * Intermediate base class for all read-side streaming processing problems, including
 * parsing and input value coercion problems.
 */
public class StreamReadException
    extends JacksonException
{
    private final static long serialVersionUID = 3L;

    protected transient JsonParser _processor;

    public StreamReadException(JsonParser p, String msg) {
        super(msg, (p == null) ? null : p.currentLocation(), null);
        _processor = p;
    }

    public StreamReadException(JsonParser p, String msg, Throwable root) {
        super(msg, (p == null) ? null : p.currentLocation(), root);
        _processor = p;
    }

    public StreamReadException(JsonParser p, String msg, JsonLocation loc) {
        super(msg, loc, null);
        _processor = p;
    }

    public StreamReadException(String msg, JsonLocation loc, Throwable rootCause) {
        super(msg);
        if (rootCause != null) {
            initCause(rootCause);
        }
        _location = loc;
    }

    /**
     * Fluent method that may be used to assign originating {@link JsonParser},
     * to be accessed using {@link #processor()}.
     *<p>
     * NOTE: `this` instance is modified and no new instance is constructed.
     *
     * @param p Parser instance to assign to this exception
     *
     * @return This exception instance to allow call chaining
     */
    public StreamReadException withParser(JsonParser p) {
        _processor = p;
        return this;
    }

    @Override
    public JsonParser processor() {
        return _processor;
    }
}
