/* Jackson JSON-processor.
 *
 * Copyright (c) 2007- Tatu Saloranta, tatu.saloranta@iki.fi
 */

package com.fasterxml.jackson.core;

/**
 * Exception type for exceptions during JSON writing, such as trying
 * to output  content in wrong context (non-matching end-array or end-object,
 * for example).
 */
public class JsonGenerationException
    extends JsonProcessingException
{
    private final static long serialVersionUID = 123; // Stupid eclipse...

    // transient since 2.7.4
    protected transient JsonGenerator _processor;

    @Deprecated // since 2.7
    public JsonGenerationException(Throwable rootCause) {
        super(rootCause);
    }

    @Deprecated // since 2.7
    public JsonGenerationException(String msg) {
        super(msg, (JsonLocation)null);
    }

    @Deprecated // since 2.7
    public JsonGenerationException(String msg, Throwable rootCause) {
        super(msg, null, rootCause);
    }

    /**
     * @since 2.7
     */
    public JsonGenerationException(Throwable rootCause, JsonGenerator g) {
        super(rootCause);
        _processor = g;
    }

    /**
     * @since 2.7
     */
    public JsonGenerationException(String msg, JsonGenerator g) {
        super(msg, (JsonLocation) null);
        _processor = g;
    }
    
    /**
     * @since 2.7
     */
    public JsonGenerationException(String msg, Throwable rootCause, JsonGenerator g) {
        super(msg, null, rootCause);
        _processor = g;
    }

    /**
     * Fluent method that may be used to assign originating {@link JsonGenerator},
     * to be accessed using {@link #getProcessor()}.
     *
     * @since 2.7
     */
    public JsonGenerationException withGenerator(JsonGenerator g) {
        _processor = g;
        return this;
    }

    @Override
    public JsonGenerator getProcessor() { return _processor; }
}
