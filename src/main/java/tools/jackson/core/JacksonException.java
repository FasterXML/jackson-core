package tools.jackson.core;

/**
 * Base class for all Jackson-produced checked exceptions.
 *<p>
 * Note that in Jackson 2.x this exception extended {@link java.io.IOException}
 * but in 3.x {@link RuntimeException}
 */
public abstract class JacksonException
    extends RuntimeException
{
    private final static long serialVersionUID = 3L; // eclipse complains otherwise

    protected JsonLocation _location;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    protected JacksonException(String msg) {
        super(msg);
    }

    protected JacksonException(Throwable rootCause) {
        super(rootCause);
    }

    protected JacksonException(String msg, Throwable rootCause) {
        super(msg, rootCause);
    }

    protected JacksonException(String msg, JsonLocation loc, Throwable rootCause) {
        super(msg, rootCause);
        _location = (loc == null) ? JsonLocation.NA : loc;
    }

    // @since 3.0
    public JacksonException withCause(Throwable cause) {
        initCause(cause);
        return this;
    }

    /**
     * Method that allows to remove context information from this exception's message.
     * Useful when you are parsing security-sensitive data and don't want original data excerpts
     * to be present in Jackson parser error messages.
     *
     * @return This exception instance to allow call chaining
     */
    public JacksonException clearLocation() {
        _location = null;
        return this;
    }

    /*
    /**********************************************************************
    /* Extended API
    /**********************************************************************
     */

    /**
     * Accessor for location information related to position within input
     * or output (depending on operation), if available; if not available
     * may return {@link JsonLocation#NA} (but never {@code null}).
     *<p>
     * Accuracy of location information depends on backend (format) as well
     * as (in some cases) operation being performed.
     *
     * @return Location in input or output that triggered the problem reported, if
     *    available; {@code null} otherwise.
     */
    public JsonLocation getLocation() { return _location; }

    /**
     * Method that allows accessing the original "message" argument,
     * without additional decorations (like location information)
     * that overridden {@link #getMessage} adds.
     *
     * @return Original, unmodified {@code message} argument used to construct
     *    this exception instance
     */
    public String getOriginalMessage() { return super.getMessage(); }

    /**
     * Method that allows accessing underlying processor that triggered
     * this exception; typically either {@link JsonParser} or {@link JsonGenerator}
     * for exceptions that originate from streaming API, but may be other types
     * when thrown by databinding.
     *<p>
     * Note that it is possible that {@code null} may be returned if code throwing
     * exception either has no access to the processor; or has not been retrofitted
     * to set it; this means that caller needs to take care to check for nulls.
     * Subtypes override this method with co-variant return type, for more
     * type-safe access.
     *<p>
     * NOTE: In Jackson 2.x, accessor was {@code getProcessor()}: in 3.0 changed to
     * non-getter to avoid having to annotate for serialization.
     *
     * @return Originating processor, if available; {@code null} if not.
     */
    public abstract Object processor();

    /*
    /**********************************************************************
    /* Methods for sub-classes to use, override
    /**********************************************************************
     */

    /**
     * Accessor that sub-classes can override to append additional
     * information right after the main message, but before
     * source location information.
     *<p>
     * NOTE: In Jackson 2.x, accessor was {@code getMessageSuffix()}: in 3.0 changed to
     * non-getter to indicate it is not a "regular" getter (although it would not
     * be serialized anyway due to lower visibility).
     *
     * @return Message suffix configured to be used, if any; {@code null} if none
     */
    protected String messageSuffix() { return null; }

    /*
    /**********************************************************************
    /* Overrides of standard methods
    /**********************************************************************
     */

    /**
     * Default method overridden so that we can add location information
     *
     * @return Message constructed based on possible optional prefix; explicit
     *   {@code message} passed to constructor as well trailing location description
     *   (separate from message by linefeed)
     */
    @Override public String getMessage() {
        String msg = super.getMessage();
        if (msg == null) {
            msg = "N/A";
        }
        JsonLocation loc = getLocation();
        String suffix = messageSuffix();
        // mild optimization, if nothing extra is needed:
        if ((loc != null) || suffix != null) {
            StringBuilder sb = new StringBuilder(100);
            sb.append(msg);
            if (suffix != null) {
                sb.append(suffix);
            }
            if (loc != null) {
                sb.append('\n');
                sb.append(" at ");
                sb = loc.toString(sb);
            }
            msg = sb.toString();
        }
        return msg;
    }

    @Override public String toString() { return getClass().getName()+": "+getMessage(); }
}
