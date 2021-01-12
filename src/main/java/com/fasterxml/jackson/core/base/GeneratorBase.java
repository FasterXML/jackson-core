package com.fasterxml.jackson.core.base;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.JacksonFeatureSet;
import com.fasterxml.jackson.core.util.VersionUtil;

/**
 * This base class implements part of API that a JSON generator exposes
 * to applications, adds shared internal methods that sub-classes
 * can use and adds some abstract methods sub-classes must implement.
 */
public abstract class GeneratorBase extends JsonGenerator
{
    public final static int SURR1_FIRST = 0xD800;
    public final static int SURR1_LAST = 0xDBFF;
    public final static int SURR2_FIRST = 0xDC00;
    public final static int SURR2_LAST = 0xDFFF;

    // // // Constants for validation messages

    protected final static String WRITE_BINARY = "write a binary value";
    protected final static String WRITE_BOOLEAN = "write a boolean value";
    protected final static String WRITE_NULL = "write a null";
    protected final static String WRITE_NUMBER = "write a number";
    protected final static String WRITE_RAW = "write a raw (unencoded) value";
    protected final static String WRITE_STRING = "write a string";

    /**
     * This value is the limit of scale allowed for serializing {@link java.math.BigDecimal}
     * in "plain" (non-engineering) notation; intent is to prevent asymmetric
     * attack whereupon simple eng-notation with big scale is used to generate
     * huge "plain" serialization. See [core#315] for details.
     */
    protected final static int MAX_BIG_DECIMAL_SCALE = 9999;

    /*
    /**********************************************************************
    /* Default capabilities
    /**********************************************************************
     */

    /**
     * Default set of {@link StreamReadCapability}ies that may be used as
     * basis for format-specific readers (or as bogus instance if non-null
     * set needs to be passed).
     */
    protected final static JacksonFeatureSet<StreamWriteCapability> DEFAULT_WRITE_CAPABILITIES
        = JacksonFeatureSet.fromDefaults(StreamWriteCapability.values());

    /**
     * Default set of {@link StreamReadCapability}ies for typical textual formats,
     * to use either as-is, or as a base with possible differences.
     */
    protected final static JacksonFeatureSet<StreamWriteCapability> DEFAULT_TEXTUAL_WRITE_CAPABILITIES
        = DEFAULT_WRITE_CAPABILITIES.with(StreamWriteCapability.CAN_WRITE_FORMATTED_NUMBERS);

    /**
     * Default set of {@link StreamReadCapability}ies for typical binary formats,
     * to use either as-is, or as a base with possible differences.
     */
    protected final static JacksonFeatureSet<StreamWriteCapability> DEFAULT_BINARY_WRITE_CAPABILITIES
        = DEFAULT_WRITE_CAPABILITIES.with(StreamWriteCapability.CAN_WRITE_BINARY_NATIVELY);

    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */

    /**
     * Context object used both to pass some initial settings and to allow
     * triggering of Object serialization through generator.
     *
     * @since 3.0
     */
    protected final ObjectWriteContext _objectWriteContext;

    /**
     * Bit flag composed of bits that indicate which
     * {@link com.fasterxml.jackson.core.StreamWriteFeature}s
     * are enabled.
     */
    protected int _streamWriteFeatures;

    /*
    /**********************************************************************
    /* State
    /**********************************************************************
     */

    /**
     * Flag that indicates whether generator is closed or not. Gets
     * set when it is closed by an explicit call
     * ({@link #close}).
     */
    protected boolean _closed;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    protected GeneratorBase(ObjectWriteContext writeCtxt, int streamWriteFeatures) {
        super();
        _objectWriteContext = writeCtxt;
        _streamWriteFeatures = streamWriteFeatures;
    }

    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */

    @Override public final boolean isEnabled(StreamWriteFeature f) { return (_streamWriteFeatures & f.getMask()) != 0; }
    @Override public int streamWriteFeatures() { return _streamWriteFeatures; }

    // public int formatWriteFeatures();

    @Override
    public final JsonGenerator configure(StreamWriteFeature f, boolean state) {
        if (state) {
            _streamWriteFeatures |= f.getMask();
        } else {
            _streamWriteFeatures &= ~f.getMask();
        }
        return this;
    }

    /*
    /**********************************************************************
    /* Public API, accessors
    /**********************************************************************
     */

    // public Object getCurrentValue();
    // public void setCurrentValue(Object v);

    // public TokenStreamContext getOutputContext();

    @Override public ObjectWriteContext getObjectWriteContext() { return _objectWriteContext; }

    /*
    /**********************************************************************
    /* Public API, write methods, structural
    /**********************************************************************
     */

    //public void writeStartArray()
    //public void writeEndArray()
    //public void writeStartObject()
    //public void writeEndObject()

    @Override
    public void writeStartArray(Object forValue, int size) throws JacksonException {
        writeStartArray(forValue);
    }

    @Override
    public void writeStartObject(Object forValue, int size) throws JacksonException
    {
        writeStartObject(forValue);
    }

    /*
    /**********************************************************************
    /* Public API, write methods, textual
    /**********************************************************************
     */

    @Override public void writeFieldName(SerializableString name) throws JacksonException {
        writeFieldName(name.getValue());
    }

    //public abstract void writeString(String text);

    //public abstract void writeString(char[] text, int offset, int len);

    @Override
    public void writeString(Reader reader, int len) throws JacksonException {
        // Let's implement this as "unsupported" to make it easier to add new parser impls
        _reportUnsupportedOperation();
    }
    
    //public abstract void writeRaw(String text);

    //public abstract void writeRaw(char[] text, int offset, int len);

    @Override
    public void writeString(SerializableString text) throws JacksonException {
        writeString(text.getValue());
    }

    @Override public void writeRawValue(String text) throws JacksonException {
        _verifyValueWrite("write raw value");
        writeRaw(text);
    }

    @Override public void writeRawValue(String text, int offset, int len) throws JacksonException {
        _verifyValueWrite("write raw value");
        writeRaw(text, offset, len);
    }

    @Override public void writeRawValue(char[] text, int offset, int len) throws JacksonException {
        _verifyValueWrite("write raw value");
        writeRaw(text, offset, len);
    }

    @Override public void writeRawValue(SerializableString text) throws JacksonException {
        _verifyValueWrite("write raw value");
        writeRaw(text);
    }

    @Override
    public int writeBinary(Base64Variant b64variant, InputStream data, int dataLength) throws JacksonException {
        // Let's implement this as "unsupported" to make it easier to add new parser impls
        _reportUnsupportedOperation();
        return 0;
    }

    /*
    /**********************************************************************
    /* Public API, write methods, primitive
    /**********************************************************************
     */

    // Not implemented at this level, added as placeholders

     /*
    public abstract void writeNumber(int i)
    public abstract void writeNumber(long l)
    public abstract void writeNumber(double d)
    public abstract void writeNumber(float f)
    public abstract void writeNumber(BigDecimal dec)
    public abstract void writeBoolean(boolean state)
    public abstract void writeNull()
    */

    /*
    /**********************************************************************
    /* Public API, write methods, POJOs, trees
    /**********************************************************************
     */

    @Override
    public void writeObject(Object value) throws JacksonException {
        if (value == null) {
            // important: call method that does check value write:
            writeNull();
        } else {
            // We are NOT to call _verifyValueWrite here, because that will be
            // done when actual serialization of POJO occurs. If we did call it,
            // state would advance causing exception later on
            _objectWriteContext.writeValue(this, value);
        }
    }

    @Override
    public void writeTree(TreeNode rootNode) throws JacksonException {
        // As with 'writeObject()', we are not to check if write would work
        if (rootNode == null) {
            writeNull();
        } else {
            _objectWriteContext.writeTree(this, rootNode);
        }
    }

    /*
    /**********************************************************************
    /* Public API, low-level output handling
    /**********************************************************************
     */

//    @Override public abstract void flush();
    @Override public void close() { _closed = true; }
    @Override public boolean isClosed() { return _closed; }

    /*
    /**********************************************************************
    /* Package methods for this, sub-classes
    /**********************************************************************
     */

    /**
     * Method called to release any buffers generator may be holding,
     * once generator is being closed.
     */
    protected abstract void _releaseBuffers();

    /**
     * Method called before trying to write a value (scalar or structured),
     * to verify that this is legal in current output state, as well as to
     * output separators if and as necessary.
     * 
     * @param typeMsg Additional message used for generating exception message
     *   if value output is NOT legal in current generator output state.
     *
     * @throws JacksonException if there is a problem in trying to write a value
     */
    protected abstract void _verifyValueWrite(String typeMsg) throws JacksonException;

    /**
     * Overridable factory method called to instantiate an appropriate {@link PrettyPrinter}
     * for case of "just use the default one", when default pretty printer handling enabled.
     *
     * @return Instance of "default" pretty printer to use
     */
    protected PrettyPrinter _constructDefaultPrettyPrinter() {
        return new DefaultPrettyPrinter();
    }

    /**
     * Helper method used to serialize a {@link java.math.BigDecimal} as a String,
     * for serialization, taking into account configuration settings
     *
     * @param value BigDecimal value to convert to String
     *
     * @return String representation of {@code value}
     *
     * @throws JacksonException if there is a problem serializing value as String
     */
    protected String _asString(BigDecimal value) throws JacksonException {
        if (StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN.enabledIn(_streamWriteFeatures)) {
            // 24-Aug-2016, tatu: [core#315] prevent possible DoS vector
            int scale = value.scale();
            if ((scale < -MAX_BIG_DECIMAL_SCALE) || (scale > MAX_BIG_DECIMAL_SCALE)) {
                _reportError(String.format(
"Attempt to write plain `java.math.BigDecimal` (see JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN) with illegal scale (%d): needs to be between [-%d, %d]",
scale, MAX_BIG_DECIMAL_SCALE, MAX_BIG_DECIMAL_SCALE));
            }
            return value.toPlainString();
        }
        return value.toString();
    }

    /*
    /**********************************************************************
    /* UTF-8 related helper method(s)
    /**********************************************************************
     */

    protected final int _decodeSurrogate(int surr1, int surr2) throws JacksonException
    {
        // First is known to be valid, but how about the other?
        if (surr2 < SURR2_FIRST || surr2 > SURR2_LAST) {
            String msg = "Incomplete surrogate pair: first char 0x"+Integer.toHexString(surr1)+", second 0x"+Integer.toHexString(surr2);
            _reportError(msg);
        }
        int c = 0x10000 + ((surr1 - SURR1_FIRST) << 10) + (surr2 - SURR2_FIRST);
        return c;
    }

    /*
    /**********************************************************************
    /* Helper methods: error reporting
    /**********************************************************************
     */

    protected void _throwInternal() { VersionUtil.throwInternal(); }
}
