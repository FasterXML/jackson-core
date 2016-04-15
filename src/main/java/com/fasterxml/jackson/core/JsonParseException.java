/* Jackson JSON-processor.
 *
 * Copyright (c) 2007- Tatu Saloranta, tatu.saloranta@iki.fi
 */

package com.fasterxml.jackson.core;

/**
 * Exception type for parsing problems, used when non-well-formed content
 * (content that does not conform to JSON syntax as per specification)
 * is encountered.
 */
public class JsonParseException extends JsonProcessingException {
    private static final long serialVersionUID = 2L; // 2.7

    // since 2.7.4
    protected transient JsonParser _processor;

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
}
