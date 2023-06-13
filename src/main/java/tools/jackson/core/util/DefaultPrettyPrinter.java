package tools.jackson.core.util;

import tools.jackson.core.*;
import tools.jackson.core.io.SerializedString;

/**
 * Default {@link PrettyPrinter} implementation that uses 2-space
 * indentation with platform-default linefeeds.
 * Usually this class is not instantiated directly, but instead
 * instantiated by {@code JsonFactory} or databind level mapper.
 *<p>
 * If you override this class, take note of {@link Instantiatable},
 * as subclasses will still create an instance of DefaultPrettyPrinter.
 *<p>
 * This class is designed for the JSON data format. It works on other formats
 * with same logical model (such as binary {@code CBOR} and {@code Smile} formats),
 * but may not work as-is for other data formats, most notably {@code XML}.
 * It may be necessary to use format-specific {@link PrettyPrinter}
 * implementation specific to that format.
 */
@SuppressWarnings("serial")
public class DefaultPrettyPrinter
    implements PrettyPrinter, Instantiatable<DefaultPrettyPrinter>,
        java.io.Serializable
{
    private static final long serialVersionUID = 1;

    /**
     * Interface that defines objects that can produce indentation used
     * to separate object entries and array values. Indentation in this
     * context just means insertion of white space, independent of whether
     * linefeeds are output.
     */
    public interface Indenter
    {
        void writeIndentation(JsonGenerator g, int level) throws JacksonException;

        /**
         * @return True if indenter is considered inline (does not add linefeeds),
         *   false otherwise
         */
        boolean isInline();
    }

    // // // Config, indentation

    /**
     * By default, let's use only spaces to separate array values.
     */
    protected Indenter _arrayIndenter = FixedSpaceIndenter.instance();

    /**
     * By default, let's use linefeed-adding indenter for separate
     * object entries. We'll further configure indenter to use
     * system-specific linefeeds, and 2 spaces per level (as opposed to,
     * say, single tabs)
     */
    protected Indenter _objectIndenter = DefaultIndenter.SYSTEM_LINEFEED_INSTANCE;

    /**
     * String printed between root-level values, if any.
     */
    protected final SerializableString _rootValueSeparator;

    // // // Config, other white space configuration

    // // // State:

    /**
     * Number of open levels of nesting. Used to determine amount of
     * indentation to use.
     */
    protected transient int _nesting;

    protected Separators _separators;

    /**
     * Separator between Object property names and values,
     * including possible before/after spaces.
     */
    protected String _objectNameValueSeparator;

    /**
     * Separator between Object property entries, 
     * including possible before/after spaces.
     */
    protected String _objectEntrySeparator;

    /**
     * Separator between Array elements, 
     * including possible before/after spaces.
     */
    protected String _arrayElementSeparator;

    /*
    /**********************************************************************
    /* Life-cycle (construct, configure)
    /**********************************************************************
     */

    public DefaultPrettyPrinter() {
        this(DEFAULT_SEPARATORS);
    }

    /**
     * @since 2.16
     */
    public DefaultPrettyPrinter(Separators separators)
    {
        _separators = separators;

        _rootValueSeparator = separators.getRootSeparator() == null ? null : new SerializedString(separators.getRootSeparator());
        _objectNameValueSeparator = separators.getObjectNameValueSpacing().apply(
                separators.getObjectNameValueSeparator());
        _objectEntrySeparator = separators.getObjectEntrySpacing().apply(separators.getObjectEntrySeparator());
        _arrayElementSeparator = separators.getArrayElementSpacing().apply(separators.getArrayElementSeparator());
    }
    
    /**
     * Copy constructor
     * 
     * @since 2.16
     */
    public DefaultPrettyPrinter(DefaultPrettyPrinter base) {
        _arrayIndenter = base._arrayIndenter;
        _objectIndenter = base._objectIndenter;
        _nesting = base._nesting;

        _separators = base._separators;
        _rootValueSeparator = base._rootValueSeparator;
        _objectNameValueSeparator = base._objectNameValueSeparator;
        _objectEntrySeparator = base._objectEntrySeparator;
        _arrayElementSeparator = base._arrayElementSeparator;
    }

    public DefaultPrettyPrinter(DefaultPrettyPrinter base, Separators separators) {
        _arrayIndenter = base._arrayIndenter;
        _objectIndenter = base._objectIndenter;
        _nesting = base._nesting;

        _separators = separators;
        _rootValueSeparator = separators.getRootSeparator() == null ? null : new SerializedString(separators.getRootSeparator());
        _objectNameValueSeparator = separators.getObjectNameValueSpacing().apply(
                separators.getObjectNameValueSeparator());
        _objectEntrySeparator = separators.getObjectEntrySpacing().apply(separators.getObjectEntrySeparator());
        _arrayElementSeparator = separators.getArrayElementSpacing().apply(separators.getArrayElementSeparator());
    
    }
    
    public void indentArraysWith(Indenter i) {
        _arrayIndenter = (i == null) ? NopIndenter.instance() : i;
    }

    public void indentObjectsWith(Indenter i) {
        _objectIndenter = (i == null) ? NopIndenter.instance() : i;
    }

    public DefaultPrettyPrinter withArrayIndenter(Indenter i) {
        if (i == null) {
            i = NopIndenter.instance();
        }
        if (_arrayIndenter == i) {
            return this;
        }
        DefaultPrettyPrinter pp = new DefaultPrettyPrinter(this);
        pp._arrayIndenter = i;
        return pp;
    }

    public DefaultPrettyPrinter withObjectIndenter(Indenter i) {
        if (i == null) {
            i = NopIndenter.instance();
        }
        if (_objectIndenter == i) {
            return this;
        }
        DefaultPrettyPrinter pp = new DefaultPrettyPrinter(this);
        pp._objectIndenter = i;
        return pp;
    }

    protected DefaultPrettyPrinter _withSpaces(boolean state)
    {
        Separators copy = _separators.withObjectNameValueSpacing(state ? Separators.Spacing.BOTH : Separators.Spacing.NONE);
        DefaultPrettyPrinter pp = withSeparators(copy);
        return pp;
    }

    /**
     * Method for configuring separators for this pretty-printer to use
     *
     * @param separators Separator definitions to use
     *
     * @return This pretty-printer instance (for call chaining)
     */
    public DefaultPrettyPrinter withSeparators(Separators separators) {
        return new DefaultPrettyPrinter(this, separators);
    }

    /*
    /**********************************************************************
    /* Instantiatable impl
    /**********************************************************************
     */

    @Override
    public DefaultPrettyPrinter createInstance() {
        if (getClass() != DefaultPrettyPrinter.class) {
            throw new IllegalStateException("Failed `createInstance()`: "+getClass().getName()
                    +" does not override method; it has to");
        }
        return new DefaultPrettyPrinter(this);
    }

    /*
    /**********************************************************************
    /* PrettyPrinter impl
    /**********************************************************************
     */

    @Override
    public void writeRootValueSeparator(JsonGenerator g) throws JacksonException
    {
        if (_rootValueSeparator != null) {
            g.writeRaw(_rootValueSeparator);
        }
    }

    @Override
    public void writeStartObject(JsonGenerator g) throws JacksonException
    {
        g.writeRaw('{');
        if (!_objectIndenter.isInline()) {
            ++_nesting;
        }
    }

    @Override
    public void beforeObjectEntries(JsonGenerator g) throws JacksonException
    {
        _objectIndenter.writeIndentation(g, _nesting);
    }

    /**
     * Method called after the object property name has been output, but
     * before the value is output.
     *<p>
     * Default handling (without pretty-printing) will output a single
     * colon to separate the two. Pretty-printer is
     * to output a colon as well, but can surround that with other
     * (white-space) decoration.
     */
    @Override
    public void writeObjectNameValueSeparator(JsonGenerator g) throws JacksonException
    {
        g.writeRaw(_objectNameValueSeparator);
    }

    /**
     * Method called after an object entry (field:value) has been completely
     * output, and before another value is to be output.
     *<p>
     * Default handling (without pretty-printing) will output a single
     * comma to separate the two. Pretty-printer is
     * to output a comma as well, but can surround that with other
     * (white-space) decoration.
     */
    @Override
    public void writeObjectEntrySeparator(JsonGenerator g) throws JacksonException
    {
        g.writeRaw(_objectEntrySeparator);
        _objectIndenter.writeIndentation(g, _nesting);
    }

    @Override
    public void writeEndObject(JsonGenerator g, int nrOfEntries) throws JacksonException
    {
        if (!_objectIndenter.isInline()) {
            --_nesting;
        }
        if (nrOfEntries > 0) {
            _objectIndenter.writeIndentation(g, _nesting);
        } else {
            g.writeRaw(' ');
        }
        g.writeRaw('}');
    }

    @Override
    public void writeStartArray(JsonGenerator g) throws JacksonException
    {
        if (!_arrayIndenter.isInline()) {
            ++_nesting;
        }
        g.writeRaw('[');
    }

    @Override
    public void beforeArrayValues(JsonGenerator g) throws JacksonException {
        _arrayIndenter.writeIndentation(g, _nesting);
    }

    /**
     * Method called after an array value has been completely
     * output, and before another value is to be output.
     *<p>
     * Default handling (without pretty-printing) will output a single
     * comma to separate the two. Pretty-printer is
     * to output a comma as well, but can surround that with other
     * (white-space) decoration.
     */
    @Override
    public void writeArrayValueSeparator(JsonGenerator g) throws JacksonException
    {
        g.writeRaw(_arrayElementSeparator);
        _arrayIndenter.writeIndentation(g, _nesting);
    }

    @Override
    public void writeEndArray(JsonGenerator g, int nrOfValues) throws JacksonException
    {
        if (!_arrayIndenter.isInline()) {
            --_nesting;
        }
        if (nrOfValues > 0) {
            _arrayIndenter.writeIndentation(g, _nesting);
        } else {
            g.writeRaw(' ');
        }
        g.writeRaw(']');
    }

    /*
    /**********************************************************************
    /* Helper classes
    /**********************************************************************
     */

    /**
     * Dummy implementation that adds no indentation whatsoever
     */
    public static class NopIndenter
        implements Indenter, java.io.Serializable
    {
        private static final NopIndenter _nopInstance = new NopIndenter();

        public static NopIndenter instance() { return _nopInstance; }

        @Override
        public void writeIndentation(JsonGenerator g, int level) { }

        @Override
        public boolean isInline() { return true; }
    }

    /**
     * This is a very simple indenter that only adds a
     * single space for indentation. It is used as the default
     * indenter for array values.
     */
    public static class FixedSpaceIndenter extends NopIndenter
    {
        private static final FixedSpaceIndenter _fixedInstance = new FixedSpaceIndenter();

        public static FixedSpaceIndenter instance() { return _fixedInstance; }

        @Override
        public void writeIndentation(JsonGenerator g, int level) throws JacksonException
        {
            g.writeRaw(' ');
        }

        @Override
        public boolean isInline() { return true; }
    }
}
