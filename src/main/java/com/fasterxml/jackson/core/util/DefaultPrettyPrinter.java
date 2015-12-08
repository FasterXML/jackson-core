package com.fasterxml.jackson.core.util;

import java.io.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.SerializedString;

/**
 * Default {@link PrettyPrinter} implementation that uses 2-space
 * indentation with platform-default linefeeds.
 * Usually this class is not instantiated directly, but instead
 * method {@link JsonGenerator#useDefaultPrettyPrinter} is
 * used, which will use an instance of this class for operation.
 */
@SuppressWarnings("serial")
public class DefaultPrettyPrinter
    implements PrettyPrinter, Instantiatable<DefaultPrettyPrinter>,
        java.io.Serializable
{
    private static final long serialVersionUID = 1;

    /**
     * Constant that specifies default "root-level" separator to use between
     * root values: a single space character.
     * 
     * @since 2.1
     */
    public final static SerializedString DEFAULT_ROOT_VALUE_SEPARATOR = new SerializedString(" ");
    
    /**
     * Interface that defines objects that can produce indentation used
     * to separate object entries and array values. Indentation in this
     * context just means insertion of white space, independent of whether
     * linefeeds are output.
     */
    public interface Indenter
    {
        void writeIndentation(JsonGenerator jg, int level) throws IOException;

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
    protected Indenter _arrayIndenter = FixedSpaceIndenter.instance;

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
    protected final SerializableString _rootSeparator;
    
    // // // Config, other white space configuration

    /**
     * By default we will add spaces around colons used to
     * separate object fields and values.
     * If disabled, will not use spaces around colon.
     */
    protected boolean _spacesInObjectEntries = true;

    // // // State:

    /**
     * Number of open levels of nesting. Used to determine amount of
     * indentation to use.
     */
    protected transient int _nesting;

    /*
    /**********************************************************
    /* Life-cycle (construct, configure)
    /**********************************************************
    */

    public DefaultPrettyPrinter() {
        this(DEFAULT_ROOT_VALUE_SEPARATOR);
    }

    /**
     * Constructor that specifies separator String to use between root values;
     * if null, no separator is printed.
     *<p>
     * Note: simply constructs a {@link SerializedString} out of parameter,
     * calls {@link #DefaultPrettyPrinter(SerializableString)}
     * 
     * @param rootSeparator
     * 
     * @since 2.1
     */
    public DefaultPrettyPrinter(String rootSeparator) {
        this((rootSeparator == null) ? null : new SerializedString(rootSeparator));
    }

    /**
     * Constructor that specifies separator String to use between root values;
     * if null, no separator is printed.
     * 
     * @param rootSeparator
     * 
     * @since 2.1
     */
    public DefaultPrettyPrinter(SerializableString rootSeparator) {
        _rootSeparator = rootSeparator;
    }
    
    public DefaultPrettyPrinter(DefaultPrettyPrinter base) {
        this(base, base._rootSeparator);
    }

    public DefaultPrettyPrinter(DefaultPrettyPrinter base,
            SerializableString rootSeparator)
    {
        _arrayIndenter = base._arrayIndenter;
        _objectIndenter = base._objectIndenter;
        _spacesInObjectEntries = base._spacesInObjectEntries;
        _nesting = base._nesting;

        _rootSeparator = rootSeparator;
    }

    public DefaultPrettyPrinter withRootSeparator(SerializableString rootSeparator)
    {
        if (_rootSeparator == rootSeparator ||
                (rootSeparator != null && rootSeparator.equals(_rootSeparator))) {
            return this;
        }
        return new DefaultPrettyPrinter(this, rootSeparator);
    }

    /**
     * @since 2.6.0
     */
    public DefaultPrettyPrinter withRootSeparator(String rootSeparator) {
        return withRootSeparator((rootSeparator == null) ? null : new SerializedString(rootSeparator));
    }
    
    public void indentArraysWith(Indenter i) {
        _arrayIndenter = (i == null) ? NopIndenter.instance : i;
    }

    public void indentObjectsWith(Indenter i) {
        _objectIndenter = (i == null) ? NopIndenter.instance : i;
    }

    /**
     * @deprecated Since 2.3 use {@link #withSpacesInObjectEntries} and {@link #withoutSpacesInObjectEntries()}
     */
    @Deprecated
    public void spacesInObjectEntries(boolean b) { _spacesInObjectEntries = b; }

    /**
     * @since 2.3
     */
    public DefaultPrettyPrinter withArrayIndenter(Indenter i) {
        if (i == null) {
            i = NopIndenter.instance;
        }
        if (_arrayIndenter == i) {
            return this;
        }
        DefaultPrettyPrinter pp = new DefaultPrettyPrinter(this);
        pp._arrayIndenter = i;
        return pp;
    }

    /**
     * @since 2.3
     */
    public DefaultPrettyPrinter withObjectIndenter(Indenter i) {
        if (i == null) {
            i = NopIndenter.instance;
        }
        if (_objectIndenter == i) {
            return this;
        }
        DefaultPrettyPrinter pp = new DefaultPrettyPrinter(this);
        pp._objectIndenter = i;
        return pp;
    }

    /**
     * "Mutant factory" method that will return a pretty printer instance
     * that does use spaces inside object entries; if 'this' instance already
     * does this, it is returned; if not, a new instance will be constructed
     * and returned.
     *
     * @since 2.3
     */
    public DefaultPrettyPrinter withSpacesInObjectEntries() {
        return _withSpaces(true);
    }

    /**
     * "Mutant factory" method that will return a pretty printer instance
     * that does not use spaces inside object entries; if 'this' instance already
     * does this, it is returned; if not, a new instance will be constructed
     * and returned.
     * 
     * @since 2.3
     */
    public DefaultPrettyPrinter withoutSpacesInObjectEntries() {
        return _withSpaces(false);
    }

    protected DefaultPrettyPrinter _withSpaces(boolean state)
    {
        if (_spacesInObjectEntries == state) {
            return this;
        }
        DefaultPrettyPrinter pp = new DefaultPrettyPrinter(this);
        pp._spacesInObjectEntries = state;
        return pp;
    }
    
    /*
    /**********************************************************
    /* Instantiatable impl
    /**********************************************************
     */
    
    @Override
    public DefaultPrettyPrinter createInstance() {
        return new DefaultPrettyPrinter(this);
    }
    
    /*
    /**********************************************************
    /* PrettyPrinter impl
    /**********************************************************
     */

    @Override
    public void writeRootValueSeparator(JsonGenerator jg) throws IOException
    {
        if (_rootSeparator != null) {
            jg.writeRaw(_rootSeparator);
        }
    }

    @Override
    public void writeStartObject(JsonGenerator jg) throws IOException
    {
        jg.writeRaw('{');
        if (!_objectIndenter.isInline()) {
            ++_nesting;
        }
    }

    @Override
    public void beforeObjectEntries(JsonGenerator jg) throws IOException
    {
        _objectIndenter.writeIndentation(jg, _nesting);
    }

    /**
     * Method called after an object field has been output, but
     * before the value is output.
     *<p>
     * Default handling (without pretty-printing) will output a single
     * colon to separate the two. Pretty-printer is
     * to output a colon as well, but can surround that with other
     * (white-space) decoration.
     */
    @Override
    public void writeObjectFieldValueSeparator(JsonGenerator jg) throws IOException
    {
        if (_spacesInObjectEntries) {
            jg.writeRaw(" : ");
        } else {
            jg.writeRaw(':');
        }
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
    public void writeObjectEntrySeparator(JsonGenerator jg) throws IOException
    {
        jg.writeRaw(',');
        _objectIndenter.writeIndentation(jg, _nesting);
    }

    @Override
    public void writeEndObject(JsonGenerator jg, int nrOfEntries) throws IOException
    {
        if (!_objectIndenter.isInline()) {
            --_nesting;
        }
        if (nrOfEntries > 0) {
            _objectIndenter.writeIndentation(jg, _nesting);
        } else {
            jg.writeRaw(' ');
        }
        jg.writeRaw('}');
    }

    @Override
    public void writeStartArray(JsonGenerator jg) throws IOException
    {
        if (!_arrayIndenter.isInline()) {
            ++_nesting;
        }
        jg.writeRaw('[');
    }

    @Override
    public void beforeArrayValues(JsonGenerator jg) throws IOException {
        _arrayIndenter.writeIndentation(jg, _nesting);
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
    public void writeArrayValueSeparator(JsonGenerator gen) throws IOException
    {
        gen.writeRaw(',');
        _arrayIndenter.writeIndentation(gen, _nesting);
    }

    @Override
    public void writeEndArray(JsonGenerator gen, int nrOfValues) throws IOException
    {
        if (!_arrayIndenter.isInline()) {
            --_nesting;
        }
        if (nrOfValues > 0) {
            _arrayIndenter.writeIndentation(gen, _nesting);
        } else {
            gen.writeRaw(' ');
        }
        gen.writeRaw(']');
    }

    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    /**
     * Dummy implementation that adds no indentation whatsoever
     */
    public static class NopIndenter
        implements Indenter, java.io.Serializable
    {
        public static final NopIndenter instance = new NopIndenter();

        @Override
        public void writeIndentation(JsonGenerator jg, int level) throws IOException { }

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
        @SuppressWarnings("hiding")
        public static final FixedSpaceIndenter instance = new FixedSpaceIndenter();

        @Override
        public void writeIndentation(JsonGenerator jg, int level) throws IOException
        {
            jg.writeRaw(' ');
        }

        @Override
        public boolean isInline() { return true; }
    }
}
