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

    protected transient JsonGenerator _processor;

    public StreamWriteException(JsonGenerator g, Throwable rootCause) {
        super(rootCause);
        _processor = g;
    }

    public StreamWriteException(JsonGenerator g, String msg) {
        super(msg);
        _processor = g;
    }

    public StreamWriteException(JsonGenerator g, String msg, Throwable rootCause) {
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
