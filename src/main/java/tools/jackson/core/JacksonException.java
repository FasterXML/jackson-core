package tools.jackson.core;

import java.io.Closeable;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Base class for all Jackson-produced checked exceptions.
 *<p>
 * Note that in Jackson 2.x this exception extended {@link java.io.IOException}
 * but in 3.x {@link RuntimeException}
 */
public class JacksonException
    extends RuntimeException
{
    private final static long serialVersionUID = 3L; // eclipse complains otherwise

    /**
     * Let's limit length of reference chain, to limit damage in cases
     * of infinite recursion.
     */
    private final static int MAX_REFS_TO_LIST = 1000;

    /**
     * Simple bean class used to contain references. References
     * can be added to indicate execution/reference path that
     * lead to the problem that caused this exception to be
     * thrown.
     *
     * @since 3.0 (in 2.x was part of databind-level exceptions only)
     */
    public static class Reference implements Serializable
    {
        private static final long serialVersionUID = 3L;

        protected transient Object _from;

        /**
         * Name of property (for POJO) or key (for Maps) that is part
         * of the reference. May be null for Collection types (which
         * generally have {@link #_index} defined), or when resolving
         * Map classes without (yet) having an instance to operate on.
         */
        protected String _propertyName;

        /**
         * Index within a {@link Collection} instance that contained
         * the reference; used if index is relevant and available.
         * If either not applicable, or not available, -1 is used to
         * denote "not known" (or not relevant).
         */
        protected int _index = -1;

        /**
         * Lazily-constructed description of this instance; needed mostly to
         * allow JDK serialization to work in case where {@link #_from} is
         * non-serializable (and has to be dropped) but we still want to pass
         * actual description along.
         */
        protected String _desc;

        /**
         * Default constructor for deserialization purposes
         */
        protected Reference() { }

        public Reference(Object from) { _from = from; }

        public Reference(Object from, String propertyName) {
            _from = from;
            _propertyName = Objects.requireNonNull(propertyName, "Cannot pass null 'propertyName'");
        }

        public Reference(Object from, int index) {
            _from = from;
            _index = index;
        }

        // Setters to let Jackson deserialize instances, but not to be called from outside
        void setPropertyName(String n) { _propertyName = n; }
        void setIndex(int ix) { _index = ix; }
        void setDescription(String d) { _desc = d; }

        /**
         * Object through which reference was resolved. Can be either
         * actual instance (usually the case for serialization), or
         * Class (usually the case for deserialization).
         *<p>
         * Note that this the accessor is not a getter on purpose as we cannot
         * (in general) serialize/deserialize this reference
         */
        public Object from() { return _from; }

        public String getPropertyName() { return _propertyName; }
        public int getIndex() { return _index; }

        public String getDescription()
        {
            if (_desc == null) {
                StringBuilder sb = new StringBuilder();

                if (_from == null) { // can this ever occur?
                    sb.append("UNKNOWN");
                } else {
                    Class<?> cls = (_from instanceof Class<?>) ? (Class<?>)_from : _from.getClass();
                    // Hmmh. Although Class.getName() is mostly ok, it does look
                    // butt-ugly for arrays.
                    // 06-Oct-2016, tatu: as per [databind#1403], `getSimpleName()` not so good
                    //   as it drops enclosing class. So let's try bit different approach
                    int arrays = 0;
                    while (cls.isArray()) {
                        cls = cls.getComponentType();
                        ++arrays;
                    }
                    sb.append(cls.getName());
                    while (--arrays >= 0) {
                        sb.append("[]");
                    }
                }
                sb.append('[');
                if (_propertyName != null) {
                    sb.append('"');
                    sb.append(_propertyName);
                    sb.append('"');
                } else if (_index >= 0) {
                    sb.append(_index);
                } else {
                    sb.append('?');
                }
                sb.append(']');
                _desc = sb.toString();
            }
            return _desc;
        }

        @Override
        public String toString() {
            return getDescription();
        }

        /**
         * May need some cleaning here, given that `from` may or may not be serializable.
         */
        Object writeReplace() {
            // as per [databind#1195], need to ensure description is not null, since
            // `_from` is transient
            getDescription();
            return this;
        }
    }

    /*
    /**********************************************************************
    /* State/configuration
    /**********************************************************************
     */

    protected JsonLocation _location;

    /**
     * Path through which problem that triggering throwing of
     * this exception was reached.
     */
    protected LinkedList<Reference> _path;

    /**
     * Underlying processor ({@link JsonParser} or {@link JsonGenerator}),
     * if known.
     *<p>
     * NOTE: typically not serializable hence <code>transient</code>
     */
    protected transient Closeable _processor;
    
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
        this(msg, null, rootCause);
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
    /* Life-cycle: more advanced factory-like methods
    /**********************************************************************
     */

    /**
     * Method that can be called to either create a new DatabindException
     * (if underlying exception is not a DatabindException), or augment
     * given exception with given path/reference information.
     *
     * This version of method is called when the reference is through a
     * non-indexed object, such as a Map or POJO/bean.
     */
    public static JacksonException wrapWithPath(Throwable src, Object refFrom,
            String refPropertyName) {
        return wrapWithPath(src, new Reference(refFrom, refPropertyName));
    }

    /**
     * Method that can be called to either create a new DatabindException
     * (if underlying exception is not a DatabindException), or augment
     * given exception with given path/reference information.
     *
     * This version of method is called when the reference is through an
     * index, which happens with arrays and Collections.
     */
    public static JacksonException wrapWithPath(Throwable src, Object refFrom, int index) {
        return wrapWithPath(src, new Reference(refFrom, index));
    }

    /**
     * Method that can be called to either create a new DatabindException
     * (if underlying exception is not a DatabindException), or augment
     * given exception with given path/reference information.
     */
    @SuppressWarnings("resource")
    public static JacksonException wrapWithPath(Throwable src, Reference ref)
    {
        JacksonException jme;
        if (src instanceof JacksonException) {
            jme = (JacksonException) src;
        } else {
            // [databind#2128]: try to avoid duplication
            String msg = _exceptionMessage(src);
            // Let's use a more meaningful placeholder if all we have is null
            if (msg == null || msg.isEmpty()) {
                msg = "(was "+src.getClass().getName()+")";
            }
            // 17-Aug-2015, tatu: Let's also pass the processor (parser/generator) along
            Closeable proc = null;
            if (src instanceof JacksonException) {
                Object proc0 = ((JacksonException) src).processor();
                if (proc0 instanceof Closeable) {
                    proc = (Closeable) proc0;
                }
            }
            jme = new JacksonException(msg, src);
            jme._processor = proc;
        }
        jme.prependPath(ref);
        return jme;
    }

    private static String _exceptionMessage(Throwable t) {
        if (t instanceof JacksonException) {
            return ((JacksonException) t).getOriginalMessage();
        }
        if (t instanceof InvocationTargetException && t.getCause() != null) {
            return t.getCause().getMessage();
        }
        return t.getMessage();
    }

    /*
    /**********************************************************************
    /* Life-cycle: information augmentation (cannot use factory style, alas)
    /**********************************************************************
     */

    /**
     * Method called to prepend a reference information in front of
     * current path
     */
    public JacksonException prependPath(Object referrer, String propertyName) {
        return prependPath(new Reference(referrer, propertyName));
    }

    /**
     * Method called to prepend a reference information in front of
     * current path
     */
    public JacksonException prependPath(Object referrer, int index) {
        return prependPath(new Reference(referrer, index));
    }

    public JacksonException prependPath(Reference r)
    {
        if (_path == null) {
            _path = new LinkedList<Reference>();
        }
        // Also: let's not increase without bounds. Could choose either
        // head or tail; tail is easier (no need to ever remove), as
        // well as potentially more useful so let's use it:
        if (_path.size() < MAX_REFS_TO_LIST) {
            _path.addFirst(r);
        }
        return this;
    }

    /*
    /**********************************************************************
    /* Accessors
    /**********************************************************************
     */

    /**
     * Method for accessing full structural path within type hierarchy
     * down to problematic property.
     */
    public List<Reference> getPath()
    {
        if (_path == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(_path);
    }

    /**
     * Method for accessing description of path that lead to the
     * problem that triggered this exception
     */
    public String getPathReference()
    {
        return getPathReference(new StringBuilder()).toString();
    }

    public StringBuilder getPathReference(StringBuilder sb)
    {
        _appendPathDesc(sb);
        return sb;
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
    public Object processor() {
        return _processor;
    }

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

    /*
    /**********************************************************************
    /* Internal methods
    /**********************************************************************
     */

    protected void _appendPathDesc(StringBuilder sb)
    {
        if (_path == null) {
            return;
        }
        Iterator<Reference> it = _path.iterator();
        while (it.hasNext()) {
            sb.append(it.next().toString());
            if (it.hasNext()) {
                sb.append("->");
            }
        }
    }

}
