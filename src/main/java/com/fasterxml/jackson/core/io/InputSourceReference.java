package com.fasterxml.jackson.core.io;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Abstraction that encloses information about input source (streaming or
 * not) for the purpose of including pertinent information in
 * location (see {@link com.fasterxml.jackson.core.JsonLocation})
 * objections, most commonly to be printed out as part of {@code Exception}
 * messages.
 *
 * @since 2.13
 */
public class InputSourceReference
    // sort of: we will read back as "UNKNOWN_INPUT"
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    protected final static InputSourceReference UNKNOWN_INPUT =
            new InputSourceReference(false, null);

    /**
     * Reference to the actual underlying source.
     */
    protected final transient Object _rawSource;

    /**
     * For static input sources, indicates offset from the beginning
     * of static array.
     * {@code -1} if not in use.
     */
    protected final int _offset;

    /**
     * For static input sources, indicates length of content in
     * the static array.
     * {@code -1} if not in use.
     */
    protected final int _length;

    /**
     * Marker flag to indicate whether included content is textual or not:
     * this is taken to mean, by default, that a snippet of content may be
     * displayed for exception messages. 
     */
    protected final boolean _textualContent;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */    

    public InputSourceReference(boolean textualContent, Object rawSource) {
        this(textualContent, rawSource, -1, -1);
    }

    public InputSourceReference(boolean textualContent, Object rawSource,
            int offset, int length)
    {
        _textualContent = textualContent;
        _rawSource = rawSource;
        _offset = offset;
        _length = length;
    }

    /**
     * Accessor for getting a placeholder for cases where actual input source
     * is not known (or is not something that system wants to expose).
     *
     * @return Placeholder "unknown" (or "empty") instance to use instead of
     *    {@code null} reference
     */
    public static InputSourceReference unknown() {
        return UNKNOWN_INPUT;
    }

    /**
     * Factory method for legacy code to use for constructing instances to
     * input sources for which only minimal amount of information is available.
     * Assumed not to contain textual content (no snippet displayed).
     * 
     * @param rawSource Underlying raw input source
     *
     * @return Instance with minimal information about source (basically just
     *    raw source without offsets; 
     */
    public static InputSourceReference rawSource(Object rawSource) {
        // 14-Mar-2021, tatu: Just to avoid russian-doll-nesting, let's:
        if (rawSource instanceof InputSourceReference) {
            return (InputSourceReference) rawSource;
        }
        return new InputSourceReference(false, rawSource);
    }

    /*
    /**********************************************************************
    /* Serializable overrides
    /**********************************************************************
     */    
    
    // For JDK serialization: can/should not retain raw source, so need
    // not read or write anything

    private void readObject(ObjectInputStream in) throws IOException {
        // nop: but must override the method
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        // nop: but must override the method
    }    

    protected Object readResolve() {
        return UNKNOWN_INPUT;
    }    

    /*
    /**********************************************************************
    /* Basic accessors
    /**********************************************************************
     */    

    public boolean hasTextualContent() {
        return _textualContent;
    }

    public Object getSource() {
        return _rawSource;
    }

    /*
    /**********************************************************************
    /* Standard method overrides
    /**********************************************************************
     */    

    // Just needed for JsonLocation#equals(): although it'd seem we only need
    // to care about identity, for backwards compatibility better compare
    // bit more
    @Override
    public boolean equals(Object other)
    {
        if (other == this) return true;
        if (other == null) return false;
        if (!(other instanceof InputSourceReference)) return false;
        InputSourceReference otherSrc = (InputSourceReference) other;

        return _rawSource == otherSrc._rawSource;
    }
}
