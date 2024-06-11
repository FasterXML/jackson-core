package tools.jackson.core.exc;

import tools.jackson.core.*;

/**
 * Intermediate base class for all write-side streaming processing problems,
 * mostly content generation issues.
 */
public class StreamWriteException
    extends JacksonException
{
    private final static long serialVersionUID = 3L;

    public StreamWriteException(JsonGenerator g, Throwable rootCause) {
        super(g, rootCause);
    }

    public StreamWriteException(JsonGenerator g, String msg) {
        super(g, msg);
    }

    public StreamWriteException(JsonGenerator g, String msg, Throwable rootCause) {
        super(g, msg, rootCause);
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

    // Overridden for co-variance
    @Override
    public JsonGenerator processor() {
        return (JsonGenerator) _processor;
    }
}
