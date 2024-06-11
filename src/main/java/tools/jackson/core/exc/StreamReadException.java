package tools.jackson.core.exc;

import tools.jackson.core.*;

/**
 * Intermediate base class for all read-side streaming processing problems, including
 * parsing and input value coercion problems.
 */
public class StreamReadException
    extends JacksonException
{
    private final static long serialVersionUID = 3L;

    public StreamReadException(String msg) {
        super(msg);
    }

    public StreamReadException(JsonParser p, String msg) {
        this(p, msg, _loc(p));
    }

    public StreamReadException(JsonParser p, String msg, Throwable rootCause) {
        this(p, msg, _loc(p), rootCause);
    }

    public StreamReadException(JsonParser p, String msg, JsonLocation loc) {
        super(p, msg, loc);
    }

    public StreamReadException(JsonParser p, String msg, JsonLocation loc,
            Throwable rootCause) {
        super(p, msg, loc, rootCause);
    }

    private static JsonLocation _loc(JsonParser p) {
        return(p == null) ? null : p.currentLocation();    
    }

    /**
     * Fluent method that may be used to assign originating {@link JsonParser},
     * to be accessed using {@link #processor()}.
     *<p>
     * NOTE: {@code this} instance is modified and no new instance is constructed.
     *
     * @param p Parser instance to assign to this exception
     *
     * @return This exception instance to allow call chaining
     */
    public StreamReadException withParser(JsonParser p) {
        _processor = p;
        return this;
    }

    // Overridden for co-variance
    @Override
    public JsonParser processor() {
        return (JsonParser) _processor;
    }
}
